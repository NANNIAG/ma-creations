import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CartProvider, useCart } from '../../cart/CartContext'
import { WishlistProvider, useWishlist } from '../../wishlist/WishlistContext'
import { CART_TOKEN_KEY } from '../../services/cartStorage'
import { WISHLIST_TOKEN_KEY } from '../../services/wishlistStorage'

const CUSTOMER_TOKEN_KEY = 'ma_customer_token'

const authState = vi.hoisted(() => ({
  isAuthenticated: false,
  isLoading: false,
}))

vi.mock('../../customer/CustomerAuthContext', () => ({
  useCustomerAuth: () => ({
    customer: authState.isAuthenticated ? { id: 1, mobileNumber: '9876543210' } : null,
    isAuthenticated: authState.isAuthenticated,
    isLoading: authState.isLoading,
    requestOtp: vi.fn(),
    verifyOtp: vi.fn(),
    logout: vi.fn(),
    refreshCustomer: vi.fn(),
  }),
  CustomerAuthProvider: ({ children }) => children,
}))

vi.mock('../../services/customerAuthStorage', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    isCustomerAuthenticated: () => authState.isAuthenticated,
  }
})

const cartMocks = vi.hoisted(() => ({
  getCart: vi.fn(),
  mergeGuestCart: vi.fn(),
  addItem: vi.fn(),
  emptyCart: () => ({
    id: null,
    guestToken: null,
    itemCount: 0,
    subtotal: 0,
    currency: 'INR',
    items: [],
  }),
}))

const wishlistMocks = vi.hoisted(() => ({
  getWishlist: vi.fn(),
  mergeGuestWishlist: vi.fn(),
  addToWishlist: vi.fn(),
  emptyWishlist: () => ({
    id: null,
    guestToken: null,
    itemCount: 0,
    items: [],
  }),
}))

vi.mock('../../services/cartService', () => ({
  emptyCart: cartMocks.emptyCart,
  getCart: (...args) => cartMocks.getCart(...args),
  mergeGuestCart: (...args) => cartMocks.mergeGuestCart(...args),
  addItem: (...args) => cartMocks.addItem(...args),
  updateQuantity: vi.fn(),
  removeItem: vi.fn(),
  clearCart: vi.fn(),
}))

vi.mock('../../services/wishlistService', () => ({
  emptyWishlist: wishlistMocks.emptyWishlist,
  getWishlist: (...args) => wishlistMocks.getWishlist(...args),
  mergeGuestWishlist: (...args) => wishlistMocks.mergeGuestWishlist(...args),
  addToWishlist: (...args) => wishlistMocks.addToWishlist(...args),
  removeFromWishlist: vi.fn(),
  clearWishlist: vi.fn(),
  toggleWishlist: vi.fn(),
}))

function CartProbe() {
  const { itemCount, mergeError, cart } = useCart()
  return (
    <div>
      <span data-testid="cart-count">{itemCount}</span>
      <span data-testid="cart-id">{cart?.id ?? ''}</span>
      {mergeError ? <span data-testid="cart-merge-error">{mergeError}</span> : null}
    </div>
  )
}

function WishlistProbe() {
  const { itemCount, mergeError } = useWishlist()
  return (
    <div>
      <span data-testid="wishlist-count">{itemCount}</span>
      {mergeError ? <span data-testid="wishlist-merge-error">{mergeError}</span> : null}
    </div>
  )
}

function renderProviders() {
  return render(
    <MemoryRouter>
      <CartProvider>
        <WishlistProvider>
          <CartProbe />
          <WishlistProbe />
        </WishlistProvider>
      </CartProvider>
    </MemoryRouter>,
  )
}

describe('guest to customer cart/wishlist merge', () => {
  beforeEach(() => {
    localStorage.clear()
    authState.isAuthenticated = false
    authState.isLoading = false
    cartMocks.getCart.mockReset()
    cartMocks.mergeGuestCart.mockReset()
    wishlistMocks.getWishlist.mockReset()
    wishlistMocks.mergeGuestWishlist.mockReset()
  })

  it('merges guest cart and wishlist after login then clears guest tokens', async () => {
    localStorage.setItem(CART_TOKEN_KEY, 'guest-cart')
    localStorage.setItem(WISHLIST_TOKEN_KEY, 'guest-wish')
    localStorage.setItem(CUSTOMER_TOKEN_KEY, 'customer-jwt')

    cartMocks.mergeGuestCart.mockResolvedValue({
      id: 50,
      guestToken: null,
      itemCount: 3,
      subtotal: 300,
      currency: 'INR',
      items: [{ id: 1, productId: 5, quantity: 3 }],
    })
    wishlistMocks.mergeGuestWishlist.mockResolvedValue({
      id: 60,
      guestToken: null,
      itemCount: 2,
      items: [{ productId: 5 }, { productId: 6 }],
    })

    authState.isAuthenticated = true
    renderProviders()

    await waitFor(() => {
      expect(cartMocks.mergeGuestCart).toHaveBeenCalled()
      expect(wishlistMocks.mergeGuestWishlist).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByTestId('cart-count')).toHaveTextContent('3')
      expect(screen.getByTestId('wishlist-count')).toHaveTextContent('2')
    })

    expect(localStorage.getItem(CART_TOKEN_KEY)).toBeNull()
    expect(localStorage.getItem(WISHLIST_TOKEN_KEY)).toBeNull()
  })

  it('keeps guest tokens when merge fails', async () => {
    localStorage.setItem(CART_TOKEN_KEY, 'guest-cart')
    localStorage.setItem(WISHLIST_TOKEN_KEY, 'guest-wish')
    localStorage.setItem(CUSTOMER_TOKEN_KEY, 'customer-jwt')

    cartMocks.mergeGuestCart.mockRejectedValue(new Error('Merge failed'))
    wishlistMocks.mergeGuestWishlist.mockRejectedValue(new Error('Wishlist merge failed'))

    authState.isAuthenticated = true
    renderProviders()

    await waitFor(() => {
      expect(screen.getByTestId('cart-merge-error')).toHaveTextContent('Merge failed')
      expect(screen.getByTestId('wishlist-merge-error')).toHaveTextContent('Wishlist merge failed')
    })

    expect(localStorage.getItem(CART_TOKEN_KEY)).toBe('guest-cart')
    expect(localStorage.getItem(WISHLIST_TOKEN_KEY)).toBe('guest-wish')
  })

  it('loads customer cart without merge when no guest token', async () => {
    localStorage.setItem(CUSTOMER_TOKEN_KEY, 'customer-jwt')
    cartMocks.getCart.mockResolvedValue({
      id: 99,
      guestToken: null,
      itemCount: 1,
      subtotal: 50,
      currency: 'INR',
      items: [{ id: 1, productId: 5, quantity: 1 }],
    })
    wishlistMocks.getWishlist.mockResolvedValue({
      id: 88,
      guestToken: null,
      itemCount: 0,
      items: [],
    })

    authState.isAuthenticated = true
    renderProviders()

    await waitFor(() => {
      expect(cartMocks.getCart).toHaveBeenCalled()
      expect(screen.getByTestId('cart-count')).toHaveTextContent('1')
    })
    expect(cartMocks.mergeGuestCart).not.toHaveBeenCalled()
  })

  it('logout clears local cart view but does not call clearCart API', async () => {
    localStorage.setItem(CUSTOMER_TOKEN_KEY, 'customer-jwt')
    cartMocks.getCart.mockResolvedValue({
      id: 99,
      guestToken: null,
      itemCount: 2,
      subtotal: 100,
      currency: 'INR',
      items: [],
    })
    wishlistMocks.getWishlist.mockResolvedValue({
      id: 88,
      itemCount: 1,
      items: [{ productId: 1 }],
    })

    authState.isAuthenticated = true
    const { rerender } = renderProviders()

    await waitFor(() => {
      expect(screen.getByTestId('cart-count')).toHaveTextContent('2')
    })

    authState.isAuthenticated = false
    localStorage.removeItem(CUSTOMER_TOKEN_KEY)
    rerender(
      <MemoryRouter>
        <CartProvider>
          <WishlistProvider>
            <CartProbe />
            <WishlistProbe />
          </WishlistProvider>
        </CartProvider>
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByTestId('cart-count')).toHaveTextContent('0')
      expect(screen.getByTestId('wishlist-count')).toHaveTextContent('0')
    })
  })
})

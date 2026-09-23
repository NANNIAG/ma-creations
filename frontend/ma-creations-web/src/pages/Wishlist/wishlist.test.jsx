import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CartProvider } from '../../cart/CartContext'
import { WishlistProvider } from '../../wishlist/WishlistContext'
import Header from '../../components/Header/Header'
import ProductCard from '../../components/ProductCard/ProductCard'
import WishlistPage from './WishlistPage'
import { WISHLIST_TOKEN_KEY } from '../../services/wishlistStorage'

vi.mock('../../customer/CustomerAuthContext', () => ({
  useCustomerAuth: () => ({
    customer: null,
    isAuthenticated: false,
    isLoading: false,
    requestOtp: vi.fn(),
    verifyOtp: vi.fn(),
    logout: vi.fn(),
    refreshCustomer: vi.fn(),
  }),
  CustomerAuthProvider: ({ children }) => children,
}))

vi.mock('../../services/wishlistService', () => ({
  emptyWishlist: () => ({
    id: null,
    guestToken: null,
    itemCount: 0,
    items: [],
  }),
  getWishlist: vi.fn(),
  addToWishlist: vi.fn(),
  removeFromWishlist: vi.fn(),
  clearWishlist: vi.fn(),
  mergeGuestWishlist: vi.fn(),
}))

vi.mock('../../services/cartService', () => ({
  emptyCart: () => ({
    id: null,
    guestToken: null,
    itemCount: 0,
    subtotal: 0,
    currency: 'INR',
    items: [],
  }),
  getCart: vi.fn(async () => ({
    id: null,
    guestToken: null,
    itemCount: 0,
    subtotal: 0,
    currency: 'INR',
    items: [],
  })),
  addItem: vi.fn(async () => ({
    id: 1,
    guestToken: 'cart-t',
    itemCount: 1,
    subtotal: 10,
    currency: 'INR',
    items: [],
  })),
  updateQuantity: vi.fn(),
  removeItem: vi.fn(),
  clearCart: vi.fn(),
}))

import * as wishlistService from '../../services/wishlistService'
import * as cartService from '../../services/cartService'

const sampleWishlist = {
  id: 1,
  guestToken: 'wish-token-1',
  itemCount: 1,
  items: [
    {
      id: 10,
      productId: 5,
      title: 'Water Bottle',
      slug: 'water-bottle',
      imageUrl: '/api/media/a.jpg',
      unitPrice: 150,
      mrp: 200,
      discountPercent: 25,
    },
  ],
}

function renderWithProviders(ui, { route = '/' } = {}) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <CartProvider>
        <WishlistProvider>
          <Routes>
            <Route path="*" element={ui} />
            <Route path="/wishlist" element={<WishlistPage />} />
          </Routes>
        </WishlistProvider>
      </CartProvider>
    </MemoryRouter>,
  )
}

describe('Header wishlist', () => {
  it('enables wishlist icon with count badge linking to /wishlist', () => {
    render(
      <MemoryRouter>
        <Header cartCount={0} wishlistCount={2} />
      </MemoryRouter>,
    )
    expect(screen.getByLabelText('Wishlist')).toHaveAttribute('href', '/wishlist')
    expect(screen.getByLabelText('2 items in wishlist')).toHaveTextContent('2')
  })
})

describe('Wishlist ProductCard + page', () => {
  beforeEach(() => {
    localStorage.clear()
    wishlistService.getWishlist.mockReset()
    wishlistService.addToWishlist.mockReset()
    wishlistService.removeFromWishlist.mockReset()
    wishlistService.getWishlist.mockResolvedValue(wishlistService.emptyWishlist())
    cartService.addItem.mockClear()
  })

  it('adds product to wishlist from heart', async () => {
    const user = userEvent.setup()
    wishlistService.addToWishlist.mockResolvedValue(sampleWishlist)

    renderWithProviders(
      <ProductCard
        product={{
          id: 5,
          title: 'Water Bottle',
          sellingPrice: 150,
          mrp: 200,
          discountPercent: 25,
        }}
      />,
    )

    await user.click(screen.getByLabelText('Add to wishlist'))
    await waitFor(() => {
      expect(wishlistService.addToWishlist).toHaveBeenCalledWith(5)
    })
  })

  it('removes product when heart is active', async () => {
    const user = userEvent.setup()
    localStorage.setItem(WISHLIST_TOKEN_KEY, 'wish-token-1')
    wishlistService.getWishlist.mockResolvedValue(sampleWishlist)
    wishlistService.removeFromWishlist.mockResolvedValue(wishlistService.emptyWishlist())

    render(
      <MemoryRouter>
        <CartProvider>
          <WishlistProvider>
            <ProductCard
              product={{
                id: 5,
                title: 'Water Bottle',
                sellingPrice: 150,
                mrp: 200,
              }}
            />
          </WishlistProvider>
        </CartProvider>
      </MemoryRouter>,
    )

    expect(await screen.findByLabelText('Remove from wishlist')).toBeInTheDocument()
    await user.click(screen.getByLabelText('Remove from wishlist'))
    await waitFor(() => {
      expect(wishlistService.removeFromWishlist).toHaveBeenCalledWith(5)
    })
  })

  it('Add to Cart still works alongside wishlist', async () => {
    const user = userEvent.setup()
    renderWithProviders(
      <ProductCard
        product={{
          id: 5,
          title: 'Water Bottle',
          sellingPrice: 150,
          mrp: 200,
        }}
      />,
    )

    await user.click(screen.getByRole('button', { name: 'Add to Cart' }))
    await waitFor(() => {
      expect(cartService.addItem).toHaveBeenCalledWith(5, 1)
    })
  })

  it('renders wishlist page items and empty state', async () => {
    localStorage.setItem(WISHLIST_TOKEN_KEY, 'wish-token-1')
    wishlistService.getWishlist.mockResolvedValue(sampleWishlist)

    render(
      <MemoryRouter initialEntries={['/wishlist']}>
        <CartProvider>
          <WishlistProvider>
            <WishlistPage />
          </WishlistProvider>
        </CartProvider>
      </MemoryRouter>,
    )

    expect(await screen.findByText('Water Bottle')).toBeInTheDocument()
    expect(screen.getByText('1 item')).toBeInTheDocument()
  })

  it('shows empty wishlist state', async () => {
    render(
      <MemoryRouter initialEntries={['/wishlist']}>
        <CartProvider>
          <WishlistProvider>
            <WishlistPage />
          </WishlistProvider>
        </CartProvider>
      </MemoryRouter>,
    )

    expect(await screen.findByText('Your wishlist is empty')).toBeInTheDocument()
  })
})

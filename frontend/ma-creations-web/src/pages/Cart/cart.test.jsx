import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CartProvider } from '../../cart/CartContext'
import { WishlistProvider } from '../../wishlist/WishlistContext'
import Header from '../../components/Header/Header'
import ProductCard from '../../components/ProductCard/ProductCard'
import CartPage from './CartPage'
import { CART_TOKEN_KEY } from '../../services/cartStorage'

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

vi.mock('../../services/cartService', () => ({
  emptyCart: () => ({
    id: null,
    guestToken: null,
    itemCount: 0,
    subtotal: 0,
    currency: 'INR',
    items: [],
  }),
  getCart: vi.fn(),
  addItem: vi.fn(),
  updateQuantity: vi.fn(),
  removeItem: vi.fn(),
  clearCart: vi.fn(),
  mergeGuestCart: vi.fn(),
}))

vi.mock('../../services/wishlistService', () => ({
  emptyWishlist: () => ({
    id: null,
    guestToken: null,
    itemCount: 0,
    items: [],
  }),
  getWishlist: vi.fn(async () => ({
    id: null,
    guestToken: null,
    itemCount: 0,
    items: [],
  })),
  addToWishlist: vi.fn(),
  removeFromWishlist: vi.fn(),
  clearWishlist: vi.fn(),
}))

import * as cartService from '../../services/cartService'

function renderWithCart(ui, { route = '/' } = {}) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <CartProvider>
        <WishlistProvider>
          <Routes>
            <Route path="*" element={ui} />
            <Route path="/cart" element={<CartPage />} />
            <Route path="/checkout" element={<div>Checkout should not open</div>} />
          </Routes>
        </WishlistProvider>
      </CartProvider>
    </MemoryRouter>,
  )
}

const sampleCart = {
  id: 1,
  guestToken: 'guest-token-1',
  itemCount: 2,
  subtotal: 300,
  currency: 'INR',
  items: [
    {
      id: 10,
      productId: 5,
      title: 'Example Product',
      slug: 'example-product',
      imageUrl: '/api/media/example.jpg',
      unitPrice: 150,
      mrp: 200,
      quantity: 2,
      lineTotal: 300,
    },
  ],
}

describe('cart UI', () => {
  beforeEach(() => {
    localStorage.clear()
    cartService.getCart.mockReset()
    cartService.addItem.mockReset()
    cartService.updateQuantity.mockReset()
    cartService.removeItem.mockReset()
    cartService.clearCart.mockReset()
    cartService.getCart.mockResolvedValue(cartService.emptyCart())
  })

  it('Add to Cart calls cart service', async () => {
    const user = userEvent.setup()
    cartService.addItem.mockResolvedValue({
      ...sampleCart,
      itemCount: 1,
      items: [{ ...sampleCart.items[0], quantity: 1, lineTotal: 150 }],
    })

    renderWithCart(
      <ProductCard
        product={{
          id: 5,
          title: 'Example Product',
          sellingPrice: 150,
          mrp: 200,
          discountPercent: 25,
        }}
      />,
    )

    await user.click(screen.getByRole('button', { name: 'Add to Cart' }))
    await waitFor(() => {
      expect(cartService.addItem).toHaveBeenCalledWith(5, 1)
    })
  })

  it('cart badge shows item count', () => {
    render(
      <MemoryRouter>
        <Header cartCount={3} />
      </MemoryRouter>,
    )
    expect(screen.getByLabelText('3 items in cart')).toHaveTextContent('3')
    expect(screen.getByLabelText('Cart')).toHaveAttribute('href', '/cart')
  })

  it('cart page renders items and supports quantity/remove/clear', async () => {
    const user = userEvent.setup()
    localStorage.setItem(CART_TOKEN_KEY, 'guest-token-1')
    cartService.getCart.mockResolvedValue(sampleCart)
    cartService.updateQuantity.mockResolvedValue({
      ...sampleCart,
      itemCount: 3,
      subtotal: 450,
      items: [{ ...sampleCart.items[0], quantity: 3, lineTotal: 450 }],
    })
    cartService.removeItem.mockResolvedValue(cartService.emptyCart())
    cartService.clearCart.mockResolvedValue(cartService.emptyCart())

    render(
      <MemoryRouter initialEntries={['/cart']}>
        <CartProvider>
          <CartPage />
        </CartProvider>
      </MemoryRouter>,
    )

    expect(await screen.findByText('Example Product')).toBeInTheDocument()

    await user.click(screen.getByLabelText('Increase quantity'))
    await waitFor(() => {
      expect(cartService.updateQuantity).toHaveBeenCalledWith(10, 3)
    })

    cartService.getCart.mockResolvedValue(sampleCart)
    cartService.updateQuantity.mockResolvedValue({
      ...sampleCart,
      itemCount: 1,
      subtotal: 150,
      items: [{ ...sampleCart.items[0], quantity: 1, lineTotal: 150 }],
    })
    await user.click(screen.getByLabelText('Decrease quantity'))
    await waitFor(() => {
      expect(cartService.updateQuantity).toHaveBeenCalled()
    })

    await user.click(screen.getByRole('button', { name: 'Remove' }))
    await waitFor(() => {
      expect(cartService.removeItem).toHaveBeenCalledWith(10)
    })
  })

  it('clear cart calls service', async () => {
    const user = userEvent.setup()
    localStorage.setItem(CART_TOKEN_KEY, 'guest-token-1')
    cartService.getCart.mockResolvedValue(sampleCart)
    cartService.clearCart.mockResolvedValue(cartService.emptyCart())

    render(
      <MemoryRouter initialEntries={['/cart']}>
        <CartProvider>
          <CartPage />
        </CartProvider>
      </MemoryRouter>,
    )

    expect(await screen.findByText('Example Product')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Clear cart' }))
    await waitFor(() => {
      expect(cartService.clearCart).toHaveBeenCalled()
    })
  })

  it('shows empty cart state', async () => {
    localStorage.setItem(CART_TOKEN_KEY, 'guest-token-1')
    cartService.getCart.mockResolvedValue(cartService.emptyCart())

    render(
      <MemoryRouter initialEntries={['/cart']}>
        <CartProvider>
          <CartPage />
        </CartProvider>
      </MemoryRouter>,
    )

    expect(await screen.findByText('Your cart is empty')).toBeInTheDocument()
  })

  it('checkout button navigates to checkout', async () => {
    const user = userEvent.setup()
    localStorage.setItem(CART_TOKEN_KEY, 'guest-token-1')
    cartService.getCart.mockResolvedValue(sampleCart)

    render(
      <MemoryRouter initialEntries={['/cart']}>
        <CartProvider>
          <Routes>
            <Route path="/cart" element={<CartPage />} />
            <Route path="/checkout" element={<div>Checkout page</div>} />
          </Routes>
        </CartProvider>
      </MemoryRouter>,
    )

    const checkoutBtn = await screen.findByRole('button', { name: 'Proceed to checkout' })
    expect(checkoutBtn).toBeEnabled()
    await user.click(checkoutBtn)
    expect(await screen.findByText('Checkout page')).toBeInTheDocument()
  })
})

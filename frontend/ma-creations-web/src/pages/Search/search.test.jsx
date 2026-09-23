import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CartProvider } from '../../cart/CartContext'
import { WishlistProvider } from '../../wishlist/WishlistContext'
import Header from '../../components/Header/Header'
import SearchPage from './SearchPage'

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

vi.mock('../../services/productService', () => ({
  fetchProducts: vi.fn(),
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
    guestToken: 't',
    itemCount: 1,
    subtotal: 10,
    currency: 'INR',
    items: [],
  })),
  updateQuantity: vi.fn(),
  removeItem: vi.fn(),
  clearCart: vi.fn(),
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

import { fetchProducts } from '../../services/productService'
import * as cartService from '../../services/cartService'

describe('Header search', () => {
  it('enables search icon and navigates to /search?q=...', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route
            path="/"
            element={
              <>
                <Header cartCount={0} />
                <div>Home</div>
              </>
            }
          />
          <Route path="/search" element={<div>Search route</div>} />
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByLabelText('Search')).toBeEnabled()
    await user.click(screen.getByLabelText('Search'))
    expect(screen.getByPlaceholderText('Search products')).toBeInTheDocument()

    await user.type(screen.getByPlaceholderText('Search products'), 'bottle')
    await user.click(screen.getByRole('button', { name: 'Search' }))
    expect(await screen.findByText('Search route')).toBeInTheDocument()
  })

  it('does not navigate for whitespace-only submit', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/']}>
        <Header cartCount={0} />
      </MemoryRouter>,
    )

    await user.click(screen.getByLabelText('Search'))
    await user.type(screen.getByPlaceholderText('Search products'), '   ')
    await user.click(screen.getByRole('button', { name: 'Search' }))
    expect(screen.getByPlaceholderText('Search products')).toBeInTheDocument()
  })
})

describe('SearchPage', () => {
  beforeEach(() => {
    fetchProducts.mockReset()
    cartService.addItem.mockClear()
  })

  it('does not call API when q is empty', async () => {
    render(
      <MemoryRouter initialEntries={['/search']}>
        <CartProvider>
        <WishlistProvider>
          <SearchPage />
        </WishlistProvider>
      </CartProvider>
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: 'Search' })).toBeInTheDocument()
    expect(fetchProducts).not.toHaveBeenCalled()
  })

  it('renders results, count, and supports sort preserving q', async () => {
    const user = userEvent.setup()
    fetchProducts.mockResolvedValue([
      {
        id: 5,
        title: 'Water Bottle',
        sellingPrice: 150,
        mrp: 200,
        discountPercent: 25,
      },
    ])

    render(
      <MemoryRouter initialEntries={['/search?q=bottle']}>
        <CartProvider>
          <WishlistProvider>
            <Routes>
              <Route path="/search" element={<SearchPage />} />
            </Routes>
          </WishlistProvider>
        </CartProvider>
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: 'Results for “bottle”' })).toBeInTheDocument()
    expect(screen.getByText('1 result')).toBeInTheDocument()
    expect(screen.getByText('Water Bottle')).toBeInTheDocument()
    expect(fetchProducts).toHaveBeenCalledWith({ search: 'bottle', sort: 'newest' })

    await user.selectOptions(screen.getByRole('combobox'), 'price_asc')
    await waitFor(() => {
      expect(fetchProducts).toHaveBeenLastCalledWith({ search: 'bottle', sort: 'price_asc' })
    })
  })

  it('shows empty results state', async () => {
    fetchProducts.mockResolvedValue([])

    render(
      <MemoryRouter initialEntries={['/search?q=zzzz']}>
        <CartProvider>
        <WishlistProvider>
          <SearchPage />
        </WishlistProvider>
      </CartProvider>
      </MemoryRouter>,
    )

    expect(await screen.findByText(/No products match/i)).toBeInTheDocument()
  })

  it('shows error state', async () => {
    fetchProducts.mockRejectedValue(new Error('boom'))

    render(
      <MemoryRouter initialEntries={['/search?q=bottle']}>
        <CartProvider>
        <WishlistProvider>
          <SearchPage />
        </WishlistProvider>
      </CartProvider>
      </MemoryRouter>,
    )

    expect(await screen.findByText('Could not search products')).toBeInTheDocument()
    expect(screen.getByText('boom')).toBeInTheDocument()
  })

  it('Add to Cart from search results uses cart service', async () => {
    const user = userEvent.setup()
    fetchProducts.mockResolvedValue([
      {
        id: 5,
        title: 'Water Bottle',
        sellingPrice: 150,
        mrp: 200,
      },
    ])

    render(
      <MemoryRouter initialEntries={['/search?q=bottle']}>
        <CartProvider>
        <WishlistProvider>
          <SearchPage />
        </WishlistProvider>
      </CartProvider>
      </MemoryRouter>,
    )

    expect(await screen.findByText('Water Bottle')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Add to Cart' }))
    await waitFor(() => {
      expect(cartService.addItem).toHaveBeenCalledWith(5, 1)
    })
  })
})

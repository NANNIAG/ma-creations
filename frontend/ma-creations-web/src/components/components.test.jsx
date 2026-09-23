import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { CartProvider } from '../cart/CartContext'
import { WishlistProvider } from '../wishlist/WishlistContext'
import CategoryCard from '../components/CategoryCard/CategoryCard'
import ProductCard from '../components/ProductCard/ProductCard'
import ErrorState from '../components/ErrorState/ErrorState'

vi.mock('../customer/CustomerAuthContext', () => ({
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

vi.mock('../services/cartService', () => ({
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
  addItem: vi.fn(),
  updateQuantity: vi.fn(),
  removeItem: vi.fn(),
  clearCart: vi.fn(),
}))

vi.mock('../services/wishlistService', () => ({
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

describe('ProductCard', () => {
  it('renders title, pricing and link', () => {
    render(
      <MemoryRouter>
        <CartProvider>
          <WishlistProvider>
            <ProductCard
              product={{
                id: 3,
                title: 'Classy Water Bottles',
                sellingPrice: 147,
                mrp: 188,
                discountPercent: 22,
                averageRating: 4,
                ratingCount: 135,
                primaryImageUrl: null,
              }}
            />
          </WishlistProvider>
        </CartProvider>
      </MemoryRouter>,
    )

    expect(screen.getByText('Classy Water Bottles')).toBeInTheDocument()
    expect(screen.getByText('22% off')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Add to Cart' })).toBeInTheDocument()
    expect(screen.getByLabelText('Add to wishlist')).toBeInTheDocument()
  })
})

describe('CategoryCard', () => {
  it('links to category PLP', () => {
    render(
      <MemoryRouter>
        <CategoryCard category={{ id: 1, name: 'Hydration & Drinkware' }} />
      </MemoryRouter>,
    )

    const link = screen.getByRole('link')
    expect(link).toHaveAttribute('href', '/categories/1')
    expect(screen.getByText('Hydration & Drinkware')).toBeInTheDocument()
  })
})

describe('ErrorState', () => {
  it('shows message used by PDP not-found flows', () => {
    render(
      <ErrorState
        title="Product not found"
        message="This product does not exist or was removed."
      />,
    )
    expect(screen.getByText('Product not found')).toBeInTheDocument()
  })
})

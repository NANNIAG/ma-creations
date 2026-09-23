import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CartProvider } from '../cart/CartContext'
import { WishlistProvider } from '../wishlist/WishlistContext'

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

vi.mock('../services/categoryService', () => ({
  fetchCategories: vi.fn(),
}))

vi.mock('../services/productService', () => ({
  fetchProducts: vi.fn(),
  fetchProductById: vi.fn(),
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

vi.mock('../config/brand', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    isWhatsAppConfigured: vi.fn(() => false),
    buildWhatsAppUrl: vi.fn(() => null),
    isInstagramConfigured: vi.fn(() => false),
    resolveInstagramFollowUrl: vi.fn(() => null),
    instagramHandle: '',
  }
})

import { fetchCategories } from '../services/categoryService'
import { fetchProductById, fetchProducts } from '../services/productService'
import {
  buildWhatsAppUrl,
  isInstagramConfigured,
  isWhatsAppConfigured,
  resolveInstagramFollowUrl,
} from '../config/brand'
import CategoryPage from '../pages/Category/CategoryPage'
import HomePage from '../pages/Home/HomePage'
import ProductDetailsPage from '../pages/ProductDetails/ProductDetailsPage'
import WhatsAppWidget from '../components/WhatsAppWidget/WhatsAppWidget'
import InstagramSection from '../components/InstagramSection/InstagramSection'

function renderStorefront(initialEntries) {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <CartProvider>
        <WishlistProvider>
          <Routes>
            <Route path="/categories/:categoryId" element={<CategoryPage />} />
            <Route path="/products/:id" element={<ProductDetailsPage />} />
            <Route path="/" element={<HomePage />} />
          </Routes>
        </WishlistProvider>
      </CartProvider>
    </MemoryRouter>,
  )
}

describe('CategoryPage sorting', () => {
  beforeEach(() => {
    fetchCategories.mockResolvedValue([{ id: 1, name: 'Hydration & Drinkware' }])
    fetchProducts.mockResolvedValue([])
  })

  it('requests products with selected sort', async () => {
    const user = userEvent.setup()
    renderStorefront(['/categories/1'])

    expect(await screen.findByRole('heading', { name: 'Hydration & Drinkware' })).toBeInTheDocument()
    expect(fetchProducts).toHaveBeenCalledWith({ categoryId: '1', sort: 'newest' })

    await user.selectOptions(screen.getByRole('combobox'), 'price_asc')
    expect(fetchProducts).toHaveBeenLastCalledWith({ categoryId: '1', sort: 'price_asc' })
  })

  it('shows empty state when category has no products', async () => {
    renderStorefront(['/categories/1'])

    expect(await screen.findByText(/No products in this category yet/i)).toBeInTheDocument()
  })
})

describe('ProductDetailsPage', () => {
  beforeEach(() => {
    fetchProductById.mockReset()
    isWhatsAppConfigured.mockReturnValue(false)
    buildWhatsAppUrl.mockReturnValue(null)
  })

  it('shows not found state', async () => {
    const { ApiError } = await import('../services/apiClient')
    fetchProductById.mockRejectedValue(
      new ApiError('PRODUCT_NOT_FOUND', 'Product not found: 9', 404),
    )

    renderStorefront(['/products/9'])

    expect(await screen.findByText('Product not found')).toBeInTheDocument()
  })

  it('renders product title and sticky CTA region', async () => {
    fetchProductById.mockResolvedValue({
      id: 2,
      title: 'Essential Lunch Boxes',
      sellingPrice: 100,
      mrp: 120,
      discountPercent: 17,
      category: { id: 2, name: 'Lunch & Meal Prep' },
      images: [],
    })

    renderStorefront(['/products/2'])

    expect(await screen.findByRole('heading', { name: 'Essential Lunch Boxes' })).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Product actions' })).toBeInTheDocument()
    expect(screen.getAllByText(/WhatsApp TBD|Buy via WhatsApp/i).length).toBeGreaterThan(0)
  })

  it('enables Buy via WhatsApp when configured', async () => {
    isWhatsAppConfigured.mockReturnValue(true)
    buildWhatsAppUrl.mockReturnValue('https://wa.me/919999999999?text=hello')

    fetchProductById.mockResolvedValue({
      id: 2,
      title: 'Essential Lunch Boxes',
      sellingPrice: 100,
      mrp: 120,
      images: [],
    })

    renderStorefront(['/products/2'])

    expect(await screen.findByRole('heading', { name: 'Essential Lunch Boxes' })).toBeInTheDocument()
    const links = screen.getAllByRole('link').filter((el) => el.getAttribute('href')?.includes('wa.me'))
    expect(links.length).toBeGreaterThan(0)
  })
})

describe('HomePage', () => {
  beforeEach(() => {
    fetchCategories.mockResolvedValue([
      { id: 1, name: 'Hydration & Drinkware' },
      { id: 2, name: 'Lunch & Meal Prep' },
    ])
    fetchProducts.mockResolvedValue([])
  })

  it('renders hero and category navigation from API', async () => {
    render(
      <MemoryRouter>
        <CartProvider>
          <WishlistProvider>
            <HomePage />
          </WishlistProvider>
        </CartProvider>
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: 'Shop by category' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Hydration/i })).toHaveAttribute('href', '/categories/1')
    expect(screen.getByText(/No products yet/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Shop Now' })).toBeInTheDocument()
  })
})

describe('WhatsAppWidget', () => {
  it('shows configuration state when number is missing', () => {
    isWhatsAppConfigured.mockReturnValue(false)
    render(<WhatsAppWidget />)
    expect(screen.getByText(/WhatsApp not configured/i)).toBeInTheDocument()
  })

  it('renders chat link when configured', () => {
    isWhatsAppConfigured.mockReturnValue(true)
    buildWhatsAppUrl.mockReturnValue('https://wa.me/919999999999')
    render(<WhatsAppWidget />)
    expect(screen.getByRole('link', { name: 'Chat on WhatsApp' })).toHaveAttribute(
      'href',
      'https://wa.me/919999999999',
    )
  })
})

describe('InstagramSection', () => {
  it('disables follow when not configured', () => {
    isInstagramConfigured.mockReturnValue(false)
    resolveInstagramFollowUrl.mockReturnValue(null)
    render(<InstagramSection />)
    expect(screen.getByRole('button', { name: 'Follow on Instagram' })).toBeDisabled()
    expect(screen.getByText(/placeholders only/i)).toBeInTheDocument()
  })

  it('enables follow link when configured', () => {
    isInstagramConfigured.mockReturnValue(true)
    resolveInstagramFollowUrl.mockReturnValue('https://instagram.com/example')
    render(<InstagramSection />)
    expect(screen.getByRole('link', { name: 'Follow on Instagram' })).toHaveAttribute(
      'href',
      'https://instagram.com/example',
    )
  })
})

import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../../services/authService', () => ({
  loginAdmin: vi.fn(),
  logoutAdmin: vi.fn(),
}))

vi.mock('../../services/authStorage', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    isAdminAuthenticated: vi.fn(),
    getAdminEmail: vi.fn(() => 'admin@macreations.test'),
  }
})

vi.mock('../../services/categoryService', () => ({
  fetchCategories: vi.fn(),
}))

vi.mock('../../services/adminProductService', () => ({
  createAdminProduct: vi.fn(),
  fetchAdminProducts: vi.fn(),
  fetchAdminProduct: vi.fn(),
  updateAdminProduct: vi.fn(),
  updateProductStatus: vi.fn(),
}))

import { loginAdmin } from '../../services/authService'
import { isAdminAuthenticated } from '../../services/authStorage'
import { fetchCategories } from '../../services/categoryService'
import {
  createAdminProduct,
  fetchAdminProducts,
  updateProductStatus,
} from '../../services/adminProductService'
import { ApiError } from '../../services/apiClient'
import ProtectedAdminRoute from '../../components/admin/ProtectedAdminRoute'
import AdminLoginPage from '../admin/AdminLoginPage'
import AdminAddProductPage from '../admin/AdminAddProductPage'
import AdminProductsPage from '../admin/AdminProductsPage'

describe('AdminLoginPage', () => {
  beforeEach(() => {
    loginAdmin.mockReset()
    isAdminAuthenticated.mockReturnValue(false)
  })

  it('renders login fields', () => {
    render(
      <MemoryRouter>
        <AdminLoginPage />
      </MemoryRouter>,
    )
    expect(screen.getByRole('heading', { name: 'Admin login' })).toBeInTheDocument()
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument()
  })

  it('shows validation errors when empty', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <AdminLoginPage />
      </MemoryRouter>,
    )
    await user.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(screen.getByText('Email is required')).toBeInTheDocument()
    expect(screen.getByText('Password is required')).toBeInTheDocument()
  })

  it('shows failed login message', async () => {
    const user = userEvent.setup()
    loginAdmin.mockRejectedValue(new ApiError('INVALID_CREDENTIALS', 'Invalid email or password', 401))

    render(
      <MemoryRouter>
        <AdminLoginPage />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText(/email/i), 'admin@macreations.test')
    await user.type(screen.getByLabelText(/password/i), 'wrong')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Invalid email or password')).toBeInTheDocument()
  })

  it('navigates on successful login', async () => {
    const user = userEvent.setup()
    loginAdmin.mockResolvedValue({ accessToken: 't', email: 'admin@macreations.test' })

    render(
      <MemoryRouter initialEntries={['/admin/login']}>
        <Routes>
          <Route path="/admin/login" element={<AdminLoginPage />} />
          <Route path="/admin" element={<div>Dashboard OK</div>} />
        </Routes>
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText(/email/i), 'admin@macreations.test')
    await user.type(screen.getByLabelText(/password/i), 'secret')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Dashboard OK')).toBeInTheDocument()
  })
})

describe('ProtectedAdminRoute', () => {
  it('redirects unauthenticated users to login', () => {
    isAdminAuthenticated.mockReturnValue(false)
    render(
      <MemoryRouter initialEntries={['/admin']}>
        <Routes>
          <Route element={<ProtectedAdminRoute />}>
            <Route path="/admin" element={<div>Secret</div>} />
          </Route>
          <Route path="/admin/login" element={<div>Login Page</div>} />
        </Routes>
      </MemoryRouter>,
    )
    expect(screen.getByText('Login Page')).toBeInTheDocument()
  })

  it('renders protected content when authenticated', () => {
    isAdminAuthenticated.mockReturnValue(true)
    render(
      <MemoryRouter initialEntries={['/admin']}>
        <Routes>
          <Route element={<ProtectedAdminRoute />}>
            <Route path="/admin" element={<div>Secret</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    )
    expect(screen.getByText('Secret')).toBeInTheDocument()
  })
})

describe('AdminAddProductPage', () => {
  beforeEach(() => {
    fetchCategories.mockResolvedValue([{ id: 1, name: 'Hydration & Drinkware' }])
    createAdminProduct.mockReset()
  })

  it('loads categories into the dropdown', async () => {
    render(
      <MemoryRouter>
        <AdminAddProductPage />
      </MemoryRouter>,
    )
    expect(await screen.findByRole('option', { name: 'Hydration & Drinkware' })).toBeInTheDocument()
  })

  it('shows success after product creation', async () => {
    const user = userEvent.setup()
    createAdminProduct.mockResolvedValue({ id: 5, title: 'Modern Bottle', images: [] })

    render(
      <MemoryRouter>
        <AdminAddProductPage />
      </MemoryRouter>,
    )

    await screen.findByRole('option', { name: 'Hydration & Drinkware' })
    await user.type(screen.getByLabelText(/product title/i), 'Modern Bottle')
    await user.selectOptions(screen.getByLabelText(/category/i), '1')
    await user.type(screen.getByLabelText(/selling price/i), '100')
    await user.type(screen.getByLabelText(/mrp/i), '120')

    const file = new File(['img'], 'bottle.jpg', { type: 'image/jpeg' })
    await user.upload(screen.getByLabelText(/image/i), file)
    await user.click(screen.getByRole('button', { name: 'Save Product' }))

    expect(await screen.findByText(/created successfully/i)).toBeInTheDocument()
    expect(createAdminProduct).toHaveBeenCalled()
  })
})

describe('AdminProductsPage', () => {
  beforeEach(() => {
    fetchAdminProducts.mockReset()
    updateProductStatus.mockReset()
  })

  it('renders product list rows with Published badge', async () => {
    fetchAdminProducts.mockResolvedValue([
      {
        id: 1,
        title: 'Festive Candle',
        category: { id: 5, name: 'Home Decor & Festivity' },
        sellingPrice: 200,
        mrp: 250,
        discountPercent: 20,
        averageRating: 4.5,
        ratingCount: 10,
        createdAt: '2026-03-01T12:00:00Z',
        primaryImageUrl: null,
        published: true,
      },
    ])

    render(
      <MemoryRouter>
        <AdminProductsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Festive Candle')).toBeInTheDocument()
    expect(screen.getByText('Home Decor & Festivity')).toBeInTheDocument()
    expect(screen.getByText('Published')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Hide' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Edit' })).toHaveAttribute(
      'href',
      '/admin/products/1/edit',
    )
  })

  it('hides a product after confirmation and keeps the row', async () => {
    const user = userEvent.setup()
    fetchAdminProducts.mockResolvedValue([
      {
        id: 2,
        title: 'Old Bottle',
        category: { id: 1, name: 'Hydration & Drinkware' },
        sellingPrice: 100,
        mrp: 120,
        discountPercent: 17,
        primaryImageUrl: null,
        published: true,
      },
    ])
    updateProductStatus.mockResolvedValue({
      id: 2,
      title: 'Old Bottle',
      published: false,
    })

    render(
      <MemoryRouter>
        <AdminProductsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Old Bottle')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Hide' }))
    const dialog = screen.getByRole('alertdialog')
    expect(dialog).toBeInTheDocument()
    await user.click(within(dialog).getByRole('button', { name: 'Hide' }))

    expect(updateProductStatus).toHaveBeenCalledWith(2, false)
    expect(await screen.findByText(/hidden from storefront/i)).toBeInTheDocument()
    expect(screen.getByText('Old Bottle')).toBeInTheDocument()
    expect(screen.getByText('Hidden')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Publish' })).toBeInTheDocument()
  })

  it('does not hide when confirmation is cancelled', async () => {
    const user = userEvent.setup()
    fetchAdminProducts.mockResolvedValue([
      {
        id: 3,
        title: 'Keep Me',
        category: { id: 1, name: 'Hydration & Drinkware' },
        sellingPrice: 50,
        mrp: 60,
        primaryImageUrl: null,
        published: true,
      },
    ])

    render(
      <MemoryRouter>
        <AdminProductsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Keep Me')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Hide' }))
    expect(screen.getByRole('alertdialog')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(updateProductStatus).not.toHaveBeenCalled()
    expect(screen.getByText('Keep Me')).toBeInTheDocument()
    expect(screen.getByText('Published')).toBeInTheDocument()
  })

  it('publishes a hidden product in one click', async () => {
    const user = userEvent.setup()
    fetchAdminProducts.mockResolvedValue([
      {
        id: 4,
        title: 'Hidden Flask',
        category: { id: 1, name: 'Hydration & Drinkware' },
        sellingPrice: 80,
        mrp: 100,
        primaryImageUrl: null,
        published: false,
      },
    ])
    updateProductStatus.mockResolvedValue({
      id: 4,
      title: 'Hidden Flask',
      published: true,
    })

    render(
      <MemoryRouter>
        <AdminProductsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Hidden Flask')).toBeInTheDocument()
    expect(screen.getByText('Hidden')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Publish' }))

    expect(updateProductStatus).toHaveBeenCalledWith(4, true)
    expect(await screen.findByText(/published\./i)).toBeInTheDocument()
    expect(screen.getByText('Published')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Hide' })).toBeInTheDocument()
  })
})

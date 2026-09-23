import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CustomerAuthProvider } from '../../customer/CustomerAuthContext'
import Header from '../../components/Header/Header'
import CustomerLoginPage from '../account/CustomerLoginPage'
import CustomerAccountPage from '../account/CustomerAccountPage'
import StorefrontLayout from '../../layouts/StorefrontLayout'

vi.mock('../../services/customerAuthService', () => ({
  requestCustomerOtp: vi.fn(),
  verifyCustomerOtp: vi.fn(),
  fetchCurrentCustomer: vi.fn(),
  logoutCustomer: vi.fn(),
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
  addItem: vi.fn(),
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

import { ApiError } from '../../services/apiClient'
import {
  fetchCurrentCustomer,
  logoutCustomer,
  requestCustomerOtp,
  verifyCustomerOtp,
} from '../../services/customerAuthService'
import { CartProvider } from '../../cart/CartContext'
import { WishlistProvider } from '../../wishlist/WishlistContext'
import { clearCustomerSession } from '../../services/customerAuthStorage'

function renderWithProviders(ui, { route = '/' } = {}) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <CustomerAuthProvider>
        <CartProvider>
          <WishlistProvider>{ui}</WishlistProvider>
        </CartProvider>
      </CustomerAuthProvider>
    </MemoryRouter>,
  )
}

describe('Customer auth', () => {
  beforeEach(() => {
    clearCustomerSession()
    localStorage.clear()
    requestCustomerOtp.mockReset()
    verifyCustomerOtp.mockReset()
    fetchCurrentCustomer.mockReset()
    logoutCustomer.mockReset()
    fetchCurrentCustomer.mockRejectedValue(new Error('no session'))
  })

  it('Header shows Login when unauthenticated', async () => {
    renderWithProviders(<Header cartCount={0} wishlistCount={0} />)
    expect(await screen.findByRole('link', { name: 'Login' })).toHaveAttribute('href', '/login')
  })

  it('requests OTP then verifies and shows account', async () => {
    const user = userEvent.setup()
    requestCustomerOtp.mockResolvedValue({ message: 'OTP sent', retryAfterSeconds: 300 })
    verifyCustomerOtp.mockResolvedValue({
      accessToken: 'tok',
      customer: { id: 1, mobileNumber: '9876543210' },
    })
    fetchCurrentCustomer.mockResolvedValue({ id: 1, mobileNumber: '9876543210' })

    renderWithProviders(
      <Routes>
        <Route path="/login" element={<CustomerLoginPage />} />
        <Route path="/account" element={<CustomerAccountPage />} />
      </Routes>,
      { route: '/login' },
    )

    await user.type(screen.getByLabelText(/mobile number/i), '9876543210')
    await user.click(screen.getByRole('button', { name: /request otp/i }))

    expect(await screen.findByLabelText(/enter otp/i)).toBeInTheDocument()
    expect(requestCustomerOtp).toHaveBeenCalledWith('9876543210')

    await user.type(screen.getByLabelText(/enter otp/i), '123456')
    await user.click(screen.getByRole('button', { name: /verify otp/i }))

    expect(verifyCustomerOtp).toHaveBeenCalledWith('9876543210', '123456')
    expect(await screen.findByText(/my account/i)).toBeInTheDocument()
    expect(screen.getByText('9876543210')).toBeInTheDocument()
  })

  it('shows invalid OTP error', async () => {
    const user = userEvent.setup()
    requestCustomerOtp.mockResolvedValue({ message: 'OTP sent' })
    verifyCustomerOtp.mockRejectedValue(new ApiError('INVALID_OTP', 'Invalid or expired OTP', 401))

    renderWithProviders(<CustomerLoginPage />, { route: '/login' })

    await user.type(screen.getByLabelText(/mobile number/i), '9876543210')
    await user.click(screen.getByRole('button', { name: /request otp/i }))
    await screen.findByLabelText(/enter otp/i)
    await user.type(screen.getByLabelText(/enter otp/i), '000000')
    await user.click(screen.getByRole('button', { name: /verify otp/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/invalid or expired otp/i)
  })

  it('logs out from account page', async () => {
    const user = userEvent.setup()
    localStorage.setItem('ma_customer_token', 'tok')
    fetchCurrentCustomer.mockResolvedValue({ id: 1, mobileNumber: '9876543210' })
    logoutCustomer.mockResolvedValue(undefined)

    renderWithProviders(
      <Routes>
        <Route path="/account" element={<CustomerAccountPage />} />
        <Route path="/login" element={<CustomerLoginPage />} />
      </Routes>,
      { route: '/account' },
    )

    expect(await screen.findByText('9876543210')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /logout/i }))
    await waitFor(() => {
      expect(logoutCustomer).toHaveBeenCalled()
    })
  })

  it('Header shows Account when authenticated', async () => {
    localStorage.setItem('ma_customer_token', 'tok')
    fetchCurrentCustomer.mockResolvedValue({ id: 1, mobileNumber: '9876543210' })

    renderWithProviders(<Header cartCount={0} wishlistCount={0} />)
    expect(await screen.findByRole('link', { name: 'Account' })).toHaveAttribute('href', '/account')
  })
})

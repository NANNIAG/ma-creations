import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CustomerAuthProvider } from '../../customer/CustomerAuthContext'
import ProtectedCustomerRoute from '../../components/account/ProtectedCustomerRoute'
import CustomerAccountPage from '../account/CustomerAccountPage'
import CustomerOrdersPage from '../account/CustomerOrdersPage'
import CustomerOrderDetailPage from '../account/CustomerOrderDetailPage'

vi.mock('../../services/customerAuthService', () => ({
  requestCustomerOtp: vi.fn(),
  verifyCustomerOtp: vi.fn(),
  fetchCurrentCustomer: vi.fn(),
  logoutCustomer: vi.fn(),
}))

vi.mock('../../services/customerOrderService', () => ({
  fetchCustomerOrders: vi.fn(),
  fetchCustomerOrder: vi.fn(),
}))

import { fetchCurrentCustomer } from '../../services/customerAuthService'
import { fetchCustomerOrder, fetchCustomerOrders } from '../../services/customerOrderService'
import { setCustomerSession, clearCustomerSession } from '../../services/customerAuthStorage'

const customer = {
  id: 1,
  mobileNumber: '9876543210',
  firstName: 'Muskan',
  lastName: null,
  email: null,
}

function renderOrdersApp(initialPath) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <CustomerAuthProvider>
        <Routes>
          <Route path="/login" element={<div>Login page</div>} />
          <Route path="/account" element={<CustomerAccountPage />} />
          <Route element={<ProtectedCustomerRoute />}>
            <Route path="/account/orders" element={<CustomerOrdersPage />} />
            <Route path="/account/orders/:orderNumber" element={<CustomerOrderDetailPage />} />
          </Route>
        </Routes>
      </CustomerAuthProvider>
    </MemoryRouter>,
  )
}

describe('Customer order pages', () => {
  beforeEach(() => {
    clearCustomerSession()
    setCustomerSession({ accessToken: 'cust-token', mobileNumber: customer.mobileNumber })
    fetchCurrentCustomer.mockResolvedValue(customer)
    fetchCustomerOrders.mockReset()
    fetchCustomerOrder.mockReset()
  })

  it('navigates from account to order list', async () => {
    fetchCustomerOrders.mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
    const user = userEvent.setup()
    renderOrdersApp('/account')

    await waitFor(() => expect(screen.getByRole('heading', { name: 'My account' })).toBeInTheDocument())
    await user.click(screen.getByRole('button', { name: 'View orders' }))
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Orders' })).toBeInTheDocument())
    expect(screen.getByText('No orders yet')).toBeInTheDocument()
  })

  it('renders order list with badges and opens detail', async () => {
    fetchCustomerOrders.mockResolvedValue({
      content: [
        {
          orderNumber: 'MAC-100',
          placedAt: '2026-09-22T10:00:00Z',
          grandTotal: 190,
          paymentMethod: 'COD',
          paymentStatus: 'COD_PENDING',
          orderStatus: 'PLACED',
          itemCount: 2,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    fetchCustomerOrder.mockResolvedValue({
      orderNumber: 'MAC-100',
      placedAt: '2026-09-22T10:00:00Z',
      orderStatus: 'PLACED',
      paymentStatus: 'COD_PENDING',
      paymentMethod: 'COD',
      itemsSubtotal: 150,
      shippingCharge: 20,
      codCharge: 20,
      taxAmount: 0,
      discountAmount: 0,
      grandTotal: 190,
      contactName: 'Muskan',
      contactMobile: '9876543210',
      shippingAddress: {
        fullName: 'Muskan',
        mobile: '9876543210',
        line1: '1 Main',
        city: 'Mumbai',
        state: 'MH',
        postalCode: '400001',
        country: 'IN',
      },
      items: [
        {
          title: 'Snap Bottle',
          quantity: 2,
          unitSellingPrice: 75,
          lineSubtotal: 150,
        },
      ],
    })

    const user = userEvent.setup()
    renderOrdersApp('/account/orders')

    await waitFor(() => expect(screen.getByText('MAC-100')).toBeInTheDocument())
    expect(screen.getByText('PLACED')).toBeInTheDocument()
    expect(screen.getByText('COD PENDING')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'View details' }))
    await waitFor(() => expect(screen.getByText('Snap Bottle')).toBeInTheDocument())
    expect(screen.getByText('COD fee')).toBeInTheDocument()
    expect(screen.getByText(/Tax is not charged/i)).toBeInTheDocument()
  })

  it('shows order list error state', async () => {
    fetchCustomerOrders.mockRejectedValue(new Error('Network down'))
    renderOrdersApp('/account/orders')
    await waitFor(() => expect(screen.getByText('Network down')).toBeInTheDocument())
  })
})

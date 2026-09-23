import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProtectedAdminRoute from '../../components/admin/ProtectedAdminRoute'
import AdminLayout from '../../layouts/AdminLayout'
import AdminOrderDetailPage from '../admin/AdminOrderDetailPage'
import CustomerOrderDetailPage from '../account/CustomerOrderDetailPage'
import TrackOrderPage from '../TrackOrder/TrackOrderPage'
import { CustomerAuthProvider } from '../../customer/CustomerAuthContext'
import ProtectedCustomerRoute from '../../components/account/ProtectedCustomerRoute'

vi.mock('../../services/authStorage', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    isAdminAuthenticated: vi.fn(() => true),
    getAdminEmail: vi.fn(() => 'admin@macreations.test'),
  }
})

vi.mock('../../services/authService', () => ({
  logoutAdmin: vi.fn(),
}))

vi.mock('../../services/adminOrderService', () => ({
  fetchAdminOrder: vi.fn(),
  updateAdminOrderStatus: vi.fn(),
  updateAdminOrderTracking: vi.fn(),
}))

vi.mock('../../services/customerOrderService', () => ({
  fetchCustomerOrder: vi.fn(),
  fetchCustomerOrders: vi.fn(),
}))

vi.mock('../../services/customerAuthService', () => ({
  fetchCurrentCustomer: vi.fn(),
  logoutCustomer: vi.fn(),
  requestCustomerOtp: vi.fn(),
  verifyCustomerOtp: vi.fn(),
}))

vi.mock('../../services/guestOrderTrackingService', () => ({
  trackGuestOrder: vi.fn(),
}))

import {
  fetchAdminOrder,
  updateAdminOrderTracking,
} from '../../services/adminOrderService'
import { fetchCustomerOrder } from '../../services/customerOrderService'
import { fetchCurrentCustomer } from '../../services/customerAuthService'
import { trackGuestOrder } from '../../services/guestOrderTrackingService'
import { ApiError } from '../../services/apiClient'
import { clearCustomerSession, setCustomerSession } from '../../services/customerAuthStorage'

const baseAdminOrder = {
  orderNumber: 'MAC-200',
  placedAt: '2026-09-22T10:00:00Z',
  customerId: 9,
  contactName: 'Asha',
  contactMobile: '9123456780',
  orderStatus: 'SHIPPED',
  paymentStatus: 'PAID',
  paymentMethod: 'UPI',
  itemsSubtotal: 200,
  shippingCharge: 20,
  codCharge: 0,
  taxAmount: 0,
  discountAmount: 0,
  grandTotal: 220,
  trackingNumber: null,
  shippingAddress: { fullName: 'Asha', line1: '2 Road', city: 'Pune', state: 'MH', postalCode: '411001' },
  items: [{ title: 'Gift Box', quantity: 1, unitSellingPrice: 200, lineSubtotal: 200 }],
  paymentTransactions: [],
}

describe('Admin tracking UI', () => {
  beforeEach(() => {
    fetchAdminOrder.mockReset()
    updateAdminOrderTracking.mockReset()
  })

  it('renders tracking field, saves value, and shows validation', async () => {
    fetchAdminOrder.mockResolvedValue({ ...baseAdminOrder })
    updateAdminOrderTracking.mockResolvedValue({
      ...baseAdminOrder,
      trackingNumber: 'AWB-123',
    })
    const user = userEvent.setup()

    render(
      <MemoryRouter initialEntries={['/admin/orders/MAC-200']}>
        <Routes>
          <Route element={<ProtectedAdminRoute />}>
            <Route path="/admin" element={<AdminLayout />}>
              <Route path="orders/:orderNumber" element={<AdminOrderDetailPage />} />
            </Route>
          </Route>
        </Routes>
      </MemoryRouter>,
    )

    await waitFor(() => expect(screen.getByLabelText(/tracking \/ awb number/i)).toBeInTheDocument())
    await user.type(screen.getByLabelText(/tracking \/ awb number/i), 'AWB-123')
    await user.click(screen.getByRole('button', { name: /save tracking/i }))
    await waitFor(() =>
      expect(updateAdminOrderTracking).toHaveBeenCalledWith('MAC-200', 'AWB-123'),
    )
    await waitFor(() => expect(screen.getByText(/tracking number saved/i)).toBeInTheDocument())
  })

  it('shows existing tracking value', async () => {
    fetchAdminOrder.mockResolvedValue({ ...baseAdminOrder, trackingNumber: 'EXISTING-9' })
    render(
      <MemoryRouter initialEntries={['/admin/orders/MAC-200']}>
        <Routes>
          <Route element={<ProtectedAdminRoute />}>
            <Route path="/admin" element={<AdminLayout />}>
              <Route path="orders/:orderNumber" element={<AdminOrderDetailPage />} />
            </Route>
          </Route>
        </Routes>
      </MemoryRouter>,
    )
    await waitFor(() =>
      expect(screen.getByLabelText(/tracking \/ awb number/i)).toHaveValue('EXISTING-9'),
    )
  })
})

describe('Customer tracking display', () => {
  beforeEach(() => {
    clearCustomerSession()
    setCustomerSession({ accessToken: 'tok', mobileNumber: '9876543210' })
    fetchCurrentCustomer.mockResolvedValue({ id: 1, mobileNumber: '9876543210' })
    fetchCustomerOrder.mockReset()
  })

  it('shows tracking when present and message when absent', async () => {
    fetchCustomerOrder.mockResolvedValue({
      orderNumber: 'MAC-100',
      placedAt: '2026-09-22T10:00:00Z',
      orderStatus: 'SHIPPED',
      paymentStatus: 'PAID',
      paymentMethod: 'UPI',
      trackingNumber: 'AWB-44',
      itemsSubtotal: 100,
      shippingCharge: 20,
      codCharge: 0,
      taxAmount: 0,
      discountAmount: 0,
      grandTotal: 120,
      items: [{ title: 'Cup', quantity: 1, unitSellingPrice: 100, lineSubtotal: 100 }],
      shippingAddress: { fullName: 'A', city: 'Mumbai', postalCode: '400001' },
    })

    render(
      <MemoryRouter initialEntries={['/account/orders/MAC-100']}>
        <CustomerAuthProvider>
          <Routes>
            <Route element={<ProtectedCustomerRoute />}>
              <Route path="/account/orders/:orderNumber" element={<CustomerOrderDetailPage />} />
            </Route>
          </Routes>
        </CustomerAuthProvider>
      </MemoryRouter>,
    )

    await waitFor(() => expect(screen.getByText('AWB-44')).toBeInTheDocument())
    expect(screen.getByText(/tracking \/ awb number/i)).toBeInTheDocument()
  })

  it('shows unavailable message when tracking absent', async () => {
    fetchCustomerOrder.mockResolvedValue({
      orderNumber: 'MAC-100',
      placedAt: '2026-09-22T10:00:00Z',
      orderStatus: 'PLACED',
      paymentStatus: 'COD_PENDING',
      paymentMethod: 'COD',
      trackingNumber: null,
      itemsSubtotal: 100,
      shippingCharge: 20,
      codCharge: 20,
      taxAmount: 0,
      discountAmount: 0,
      grandTotal: 140,
      items: [{ title: 'Cup', quantity: 1, unitSellingPrice: 100, lineSubtotal: 100 }],
      shippingAddress: { fullName: 'A', city: 'Mumbai', postalCode: '400001' },
    })

    render(
      <MemoryRouter initialEntries={['/account/orders/MAC-100']}>
        <CustomerAuthProvider>
          <Routes>
            <Route element={<ProtectedCustomerRoute />}>
              <Route path="/account/orders/:orderNumber" element={<CustomerOrderDetailPage />} />
            </Route>
          </Routes>
        </CustomerAuthProvider>
      </MemoryRouter>,
    )

    await waitFor(() =>
      expect(
        screen.getByText(/tracking information will be available after the order is shipped/i),
      ).toBeInTheDocument(),
    )
  })
})

describe('Guest track order page', () => {
  beforeEach(() => {
    trackGuestOrder.mockReset()
  })

  it('tracks order and shows result with tracking number', async () => {
    trackGuestOrder.mockResolvedValue({
      orderNumber: 'MAC-G1',
      placedAt: '2026-09-22T10:00:00Z',
      orderStatus: 'SHIPPED',
      paymentStatus: 'COD_PENDING',
      paymentMethod: 'COD',
      trackingNumber: 'AWB-77',
      grandTotal: 190,
      deliveryCity: 'Mumbai',
      deliveryPostalCode: '400001',
      items: [{ title: 'Bottle', quantity: 1, lineSubtotal: 150 }],
    })
    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <TrackOrderPage />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText(/order number/i), 'MAC-G1')
    await user.type(screen.getByLabelText(/mobile number/i), '9876543210')
    await user.click(screen.getByRole('button', { name: /track order/i }))

    await waitFor(() => expect(screen.getByText('MAC-G1')).toBeInTheDocument())
    expect(screen.getByText('AWB-77')).toBeInTheDocument()
    expect(trackGuestOrder).toHaveBeenCalledWith({
      orderNumber: 'MAC-G1',
      mobileNumber: '9876543210',
    })
  })

  it('shows not found and no-tracking message paths', async () => {
    const user = userEvent.setup()
    trackGuestOrder.mockRejectedValue(new ApiError('ORDER_NOT_FOUND', 'Order not found', 404))
    render(
      <MemoryRouter>
        <TrackOrderPage />
      </MemoryRouter>,
    )
    await user.type(screen.getByLabelText(/order number/i), 'MAC-X')
    await user.type(screen.getByLabelText(/mobile number/i), '9876543210')
    await user.click(screen.getByRole('button', { name: /track order/i }))
    await waitFor(() => expect(screen.getByText(/order not found/i)).toBeInTheDocument())

    trackGuestOrder.mockResolvedValue({
      orderNumber: 'MAC-G2',
      placedAt: '2026-09-22T10:00:00Z',
      orderStatus: 'PLACED',
      paymentStatus: 'COD_PENDING',
      paymentMethod: 'COD',
      trackingNumber: null,
      grandTotal: 190,
      items: [{ title: 'Bottle', quantity: 1, lineSubtotal: 150 }],
    })
    await user.clear(screen.getByLabelText(/order number/i))
    await user.type(screen.getByLabelText(/order number/i), 'MAC-G2')
    await user.click(screen.getByRole('button', { name: /track order/i }))
    await waitFor(() =>
      expect(
        screen.getByText(/tracking information will be available after the order is shipped/i),
      ).toBeInTheDocument(),
    )
  })
})

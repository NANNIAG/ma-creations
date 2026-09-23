import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProtectedAdminRoute from '../../components/admin/ProtectedAdminRoute'
import AdminLayout from '../../layouts/AdminLayout'
import AdminOrdersPage from '../admin/AdminOrdersPage'
import AdminOrderDetailPage from '../admin/AdminOrderDetailPage'

vi.mock('../../services/authService', () => ({
  loginAdmin: vi.fn(),
  logoutAdmin: vi.fn(),
}))

vi.mock('../../services/authStorage', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    isAdminAuthenticated: vi.fn(() => true),
    getAdminEmail: vi.fn(() => 'admin@macreations.test'),
  }
})

vi.mock('../../services/adminOrderService', () => ({
  fetchAdminOrders: vi.fn(),
  fetchAdminOrder: vi.fn(),
  updateAdminOrderStatus: vi.fn(),
}))

import {
  fetchAdminOrder,
  fetchAdminOrders,
  updateAdminOrderStatus,
} from '../../services/adminOrderService'

function renderAdminOrders(path) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route element={<ProtectedAdminRoute />}>
          <Route path="/admin" element={<AdminLayout />}>
            <Route path="orders" element={<AdminOrdersPage />} />
            <Route path="orders/:orderNumber" element={<AdminOrderDetailPage />} />
          </Route>
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('Admin order pages', () => {
  beforeEach(() => {
    fetchAdminOrders.mockReset()
    fetchAdminOrder.mockReset()
    updateAdminOrderStatus.mockReset()
  })

  it('renders order list, search, and filters', async () => {
    fetchAdminOrders.mockResolvedValue({
      content: [
        {
          orderNumber: 'MAC-200',
          placedAt: '2026-09-22T10:00:00Z',
          contactName: 'Asha',
          contactMobile: '9123456780',
          grandTotal: 220,
          paymentMethod: 'UPI',
          paymentStatus: 'PAID',
          orderStatus: 'PLACED',
          itemCount: 1,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const user = userEvent.setup()
    renderAdminOrders('/admin/orders')

    await waitFor(() => expect(screen.getByText('MAC-200')).toBeInTheDocument())
    expect(screen.getByText('Asha')).toBeInTheDocument()
    expect(screen.getAllByText('PAID').length).toBeGreaterThan(0)

    await user.type(screen.getByPlaceholderText(/order number or mobile/i), 'MAC-200')
    await user.click(screen.getByRole('button', { name: 'Search' }))
    await waitFor(() =>
      expect(fetchAdminOrders).toHaveBeenCalledWith(
        expect.objectContaining({ q: 'MAC-200' }),
      ),
    )

    await user.selectOptions(screen.getByLabelText(/order status/i), 'PROCESSING')
    await waitFor(() =>
      expect(fetchAdminOrders).toHaveBeenCalledWith(
        expect.objectContaining({ status: 'PROCESSING' }),
      ),
    )
  })

  it('shows empty state', async () => {
    fetchAdminOrders.mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })
    renderAdminOrders('/admin/orders')
    await waitFor(() => expect(screen.getByText('No orders found')).toBeInTheDocument())
  })

  it('shows detail and updates status', async () => {
    fetchAdminOrder.mockResolvedValue({
      orderNumber: 'MAC-200',
      placedAt: '2026-09-22T10:00:00Z',
      customerId: 9,
      contactName: 'Asha',
      contactMobile: '9123456780',
      orderStatus: 'PLACED',
      paymentStatus: 'PAID',
      paymentMethod: 'UPI',
      itemsSubtotal: 200,
      shippingCharge: 20,
      codCharge: 0,
      taxAmount: 0,
      discountAmount: 0,
      grandTotal: 220,
      shippingAddress: {
        fullName: 'Asha',
        line1: '2 Road',
        city: 'Pune',
        state: 'MH',
        postalCode: '411001',
      },
      items: [{ title: 'Gift Box', quantity: 1, unitSellingPrice: 200, lineSubtotal: 200 }],
      paymentTransactions: [
        {
          id: 1,
          provider: 'RAZORPAY',
          providerOrderId: 'order_1',
          providerPaymentId: 'pay_1',
          status: 'CAPTURED',
          amount: 220,
          paymentMethod: 'UPI',
        },
      ],
    })
    updateAdminOrderStatus.mockResolvedValue({
      orderNumber: 'MAC-200',
      placedAt: '2026-09-22T10:00:00Z',
      customerId: 9,
      contactName: 'Asha',
      contactMobile: '9123456780',
      orderStatus: 'PROCESSING',
      paymentStatus: 'PAID',
      paymentMethod: 'UPI',
      itemsSubtotal: 200,
      shippingCharge: 20,
      codCharge: 0,
      taxAmount: 0,
      discountAmount: 0,
      grandTotal: 220,
      shippingAddress: {
        fullName: 'Asha',
        line1: '2 Road',
        city: 'Pune',
        state: 'MH',
        postalCode: '411001',
      },
      items: [{ title: 'Gift Box', quantity: 1, unitSellingPrice: 200, lineSubtotal: 200 }],
      paymentTransactions: [],
    })

    const user = userEvent.setup()
    renderAdminOrders('/admin/orders/MAC-200')

    await waitFor(() => expect(screen.getByText('Gift Box')).toBeInTheDocument())
    expect(screen.getByText(/provider order: order_1/i)).toBeInTheDocument()
    expect(screen.getByText(/payment status is controlled by razorpay/i)).toBeInTheDocument()

    const statusSelect = screen.getByLabelText(/^status$/i)
    await user.selectOptions(statusSelect, 'PROCESSING')
    await user.click(screen.getByRole('button', { name: 'Save status' }))

    await waitFor(() =>
      expect(updateAdminOrderStatus).toHaveBeenCalledWith('MAC-200', 'PROCESSING'),
    )
    await waitFor(() =>
      expect(screen.getByText(/order status updated to processing/i)).toBeInTheDocument(),
    )
  })

  it('shows loading then error', async () => {
    fetchAdminOrders.mockRejectedValue(new Error('Server error'))
    renderAdminOrders('/admin/orders')
    await waitFor(() => expect(screen.getByText('Server error')).toBeInTheDocument())
  })
})

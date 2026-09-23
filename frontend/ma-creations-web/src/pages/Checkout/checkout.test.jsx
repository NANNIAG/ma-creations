import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CartProvider } from '../../cart/CartContext'
import CheckoutPage from './CheckoutPage'
import OrderSuccessPage from './OrderSuccessPage'
import OrderPaymentFailedPage from './OrderPaymentFailedPage'

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
}))

vi.mock('../../services/checkoutService', () => ({
  previewCheckout: vi.fn(),
}))

vi.mock('../../services/orderService', () => ({
  placeOrder: vi.fn(),
  fetchOrderSummary: vi.fn(),
  createIdempotencyKey: () => 'test-idem-key',
}))

vi.mock('../../services/paymentService', () => ({
  initiatePayment: vi.fn(),
  verifyPayment: vi.fn(),
  reportPaymentFailure: vi.fn(),
  loadRazorpayScript: vi.fn(async () => {}),
  openRazorpayCheckout: vi.fn(),
  buildRazorpayCheckoutOptions: vi.fn(),
}))

vi.mock('../../services/cartStorage', () => ({
  getCartToken: vi.fn(() => 'guest-token'),
  clearCartToken: vi.fn(),
}))

vi.mock('../../services/customerAuthStorage', () => ({
  isCustomerAuthenticated: vi.fn(() => false),
}))

import * as cartService from '../../services/cartService'
import { previewCheckout } from '../../services/checkoutService'
import { placeOrder, fetchOrderSummary } from '../../services/orderService'
import { openRazorpayCheckout, verifyPayment } from '../../services/paymentService'

const cartWithItem = {
  id: 1,
  guestToken: 'guest-token',
  itemCount: 1,
  subtotal: 150,
  currency: 'INR',
  items: [
    {
      id: 10,
      productId: 5,
      title: 'Bottle',
      quantity: 1,
      unitPrice: 150,
      mrp: 200,
      lineTotal: 150,
    },
  ],
}

const readyPreview = {
  valid: true,
  readyToPlace: true,
  requiresReview: false,
  previewHash: 'hash-ready',
  currency: 'INR',
  itemsSubtotal: 150,
  shippingCharge: 20,
  codCharge: 0,
  taxAmount: 0,
  discountAmount: 0,
  grandTotal: 170,
  pendingRules: [],
  lines: [
    {
      productId: 5,
      title: 'Bottle',
      quantity: 1,
      unitSellingPrice: 150,
      lineSubtotal: 150,
    },
  ],
  issues: [],
}

const readyCodPreview = {
  ...readyPreview,
  previewHash: 'hash-cod',
  codCharge: 20,
  grandTotal: 190,
}

function renderCheckout(initialPath = '/checkout') {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <CartProvider>
        <Routes>
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/order-success/:orderNumber" element={<OrderSuccessPage />} />
          <Route path="/order-payment-failed/:orderNumber" element={<OrderPaymentFailedPage />} />
          <Route path="/" element={<div>Home</div>} />
          <Route path="/cart" element={<div>Cart page</div>} />
        </Routes>
      </CartProvider>
    </MemoryRouter>,
  )
}

describe('Checkout UI', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    cartService.getCart.mockResolvedValue(cartWithItem)
    previewCheckout.mockImplementation(async ({ paymentMethod } = {}) => {
      if (paymentMethod === 'COD') {
        return readyCodPreview
      }
      return readyPreview
    })
  })

  it('renders checkout and loads preview', async () => {
    renderCheckout()
    expect(await screen.findByRole('heading', { name: 'Checkout' })).toBeInTheDocument()
    await waitFor(() => expect(previewCheckout).toHaveBeenCalled())
    expect(await screen.findByText('Bottle')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Place order' })).toBeEnabled()
  })

  it('prepaid summary shows shipping and hides COD fee and GST', async () => {
    renderCheckout()
    await waitFor(() => expect(previewCheckout).toHaveBeenCalled())
    expect(await screen.findByText('Shipping')).toBeInTheDocument()
    expect(screen.getByText('₹20.00')).toBeInTheDocument()
    expect(screen.getByText('₹170.00')).toBeInTheDocument()
    expect(screen.queryByText('COD Fee')).not.toBeInTheDocument()
    expect(screen.queryByText(/^Tax$/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/GST/i)).not.toBeInTheDocument()
  })

  it('COD summary shows shipping and COD fee; grand total updates', async () => {
    const user = userEvent.setup()
    renderCheckout()
    await waitFor(() => expect(previewCheckout).toHaveBeenCalled())
    await user.click(screen.getByLabelText(/Cash on Delivery/i))
    await waitFor(() =>
      expect(previewCheckout).toHaveBeenCalledWith(
        expect.objectContaining({ paymentMethod: 'COD' }),
      ),
    )
    expect(await screen.findByText('COD Fee')).toBeInTheDocument()
    expect(screen.getByText('₹190.00')).toBeInTheDocument()
    expect(screen.queryByText(/^Tax$/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/GST/i)).not.toBeInTheDocument()
  })

  it('shows empty cart state', async () => {
    cartService.getCart.mockResolvedValue({
      id: null,
      itemCount: 0,
      subtotal: 0,
      items: [],
    })
    renderCheckout()
    expect(await screen.findByText(/Your cart is empty/i)).toBeInTheDocument()
  })

  it('validates address before place order', async () => {
    const user = userEvent.setup()
    renderCheckout()
    const placeBtn = await screen.findByRole('button', { name: 'Place order' })
    await waitFor(() => expect(placeBtn).toBeEnabled())
    await user.click(placeBtn)
    expect(await screen.findByText(/Please fix the highlighted fields/i)).toBeInTheDocument()
    expect(placeOrder).not.toHaveBeenCalled()
  })

  it('places COD order for guest and navigates to success', async () => {
    const user = userEvent.setup()
    placeOrder.mockResolvedValue({
      orderNumber: 'MAC-COD-1',
      status: 'PLACED',
      paymentStatus: 'COD_PENDING',
      paymentMethod: 'COD',
      grandTotal: 150,
      requiresOnlinePayment: false,
      items: [{ title: 'Bottle', quantity: 1, lineSubtotal: 150 }],
    })
    cartService.getCart
      .mockResolvedValueOnce(cartWithItem)
      .mockResolvedValue({ id: 1, items: [], itemCount: 0, subtotal: 0 })

    renderCheckout()
    await waitFor(() => expect(screen.getByRole('button', { name: 'Place order' })).toBeEnabled())

    await user.type(screen.getByLabelText(/^Name$/i), 'Muskan')
    await user.type(screen.getByLabelText(/^Mobile number$/i), '9876543210')
    await user.type(screen.getByLabelText(/^Full name$/i), 'Muskan')
    await user.type(screen.getByLabelText(/^Mobile$/i), '9876543210')
    await user.type(screen.getByLabelText(/Address line 1/i), '12 Street')
    await user.type(screen.getByLabelText(/^City$/i), 'Mumbai')
    await user.type(screen.getByLabelText(/^State$/i), 'MH')
    await user.type(screen.getByLabelText(/PIN code/i), '400001')
    await user.click(screen.getByLabelText(/Cash on Delivery/i))

    await waitFor(() =>
      expect(previewCheckout).toHaveBeenCalledWith(
        expect.objectContaining({ paymentMethod: 'COD' }),
      ),
    )
    await waitFor(() => expect(screen.getByRole('button', { name: 'Place order' })).toBeEnabled())
    await user.click(screen.getByRole('button', { name: 'Place order' }))

    await waitFor(() => expect(placeOrder).toHaveBeenCalled())
    expect(placeOrder.mock.calls[0][0].paymentMethod).toBe('COD')
    expect(placeOrder.mock.calls[0][0]).not.toHaveProperty('grandTotal')
    expect(await screen.findByText(/Order placed/i)).toBeInTheDocument()
    expect(screen.getByText('MAC-COD-1')).toBeInTheDocument()
  })

  it('initiates Razorpay for UPI and verifies payment', async () => {
    const user = userEvent.setup()
    placeOrder.mockResolvedValue({
      orderNumber: 'MAC-UPI-1',
      status: 'PENDING_PAYMENT',
      paymentStatus: 'PENDING',
      paymentMethod: 'UPI',
      requiresOnlinePayment: true,
      razorpay: {
        keyId: 'rzp_test',
        razorpayOrderId: 'order_rzp',
        amount: 15000,
        currency: 'INR',
      },
    })
    openRazorpayCheckout.mockImplementation((_init, handlers) => {
      handlers.onSuccess({
        razorpay_order_id: 'order_rzp',
        razorpay_payment_id: 'pay_1',
        razorpay_signature: 'sig',
      })
    })
    verifyPayment.mockResolvedValue({
      orderNumber: 'MAC-UPI-1',
      paid: true,
      paymentStatus: 'PAID',
      orderStatus: 'PLACED',
    })

    renderCheckout()
    await screen.findByRole('button', { name: 'Place order' })

    await user.type(screen.getByLabelText(/^Name$/i), 'Muskan')
    await user.type(screen.getByLabelText(/^Mobile number$/i), '9876543210')
    await user.type(screen.getByLabelText(/^Full name$/i), 'Muskan')
    await user.type(screen.getByLabelText(/^Mobile$/i), '9876543210')
    await user.type(screen.getByLabelText(/Address line 1/i), '12 Street')
    await user.type(screen.getByLabelText(/^City$/i), 'Mumbai')
    await user.type(screen.getByLabelText(/^State$/i), 'MH')
    await user.type(screen.getByLabelText(/PIN code/i), '400001')
    await user.click(screen.getByRole('button', { name: 'Place order' }))

    await waitFor(() => expect(openRazorpayCheckout).toHaveBeenCalled())
    await waitFor(() => expect(verifyPayment).toHaveBeenCalled())
    expect(await screen.findByText(/Payment confirmed/i)).toBeInTheDocument()
  })

  it('navigates to failure when Razorpay is dismissed', async () => {
    const user = userEvent.setup()
    placeOrder.mockResolvedValue({
      orderNumber: 'MAC-FAIL-1',
      requiresOnlinePayment: true,
      razorpay: {
        keyId: 'rzp_test',
        razorpayOrderId: 'order_rzp',
        amount: 15000,
        currency: 'INR',
      },
    })
    openRazorpayCheckout.mockImplementation((_init, handlers) => {
      handlers.onDismiss()
    })

    renderCheckout()
    await screen.findByRole('button', { name: 'Place order' })

    await user.type(screen.getByLabelText(/^Name$/i), 'Muskan')
    await user.type(screen.getByLabelText(/^Mobile number$/i), '9876543210')
    await user.type(screen.getByLabelText(/^Full name$/i), 'Muskan')
    await user.type(screen.getByLabelText(/^Mobile$/i), '9876543210')
    await user.type(screen.getByLabelText(/Address line 1/i), '12 Street')
    await user.type(screen.getByLabelText(/^City$/i), 'Mumbai')
    await user.type(screen.getByLabelText(/^State$/i), 'MH')
    await user.type(screen.getByLabelText(/PIN code/i), '400001')
    await user.click(screen.getByRole('button', { name: 'Place order' }))

    expect(await screen.findByText(/Payment not completed/i)).toBeInTheDocument()
    expect(screen.getByText('MAC-FAIL-1')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Retry payment/i })).toBeInTheDocument()
  })

  it('refreshes preview when payment method changes', async () => {
    const user = userEvent.setup()
    renderCheckout()
    await waitFor(() => expect(previewCheckout).toHaveBeenCalled())
    previewCheckout.mockClear()
    await user.click(screen.getByLabelText(/Net Banking/i))
    await waitFor(() =>
      expect(previewCheckout).toHaveBeenCalledWith(
        expect.objectContaining({ paymentMethod: 'NET_BANKING' }),
      ),
    )
  })

  it('shows Pay Later unavailable from place-order error', async () => {
    const user = userEvent.setup()
    const { ApiError } = await import('../../services/apiClient')
    placeOrder.mockRejectedValue(new ApiError('PAY_LATER_UNAVAILABLE', 'Pay Later unavailable', 400))

    renderCheckout()
    await screen.findByRole('button', { name: 'Place order' })

    await user.type(screen.getByLabelText(/^Name$/i), 'Muskan')
    await user.type(screen.getByLabelText(/^Mobile number$/i), '9876543210')
    await user.type(screen.getByLabelText(/^Full name$/i), 'Muskan')
    await user.type(screen.getByLabelText(/^Mobile$/i), '9876543210')
    await user.type(screen.getByLabelText(/Address line 1/i), '12 Street')
    await user.type(screen.getByLabelText(/^City$/i), 'Mumbai')
    await user.type(screen.getByLabelText(/^State$/i), 'MH')
    await user.type(screen.getByLabelText(/PIN code/i), '400001')
    await user.click(screen.getByLabelText(/Pay Later/i))
    await user.click(screen.getByRole('button', { name: 'Place order' }))

    expect(await screen.findByText(/Pay Later is not available/i)).toBeInTheDocument()
  })

  it('handles stale preview error', async () => {
    const user = userEvent.setup()
    const { ApiError } = await import('../../services/apiClient')
    placeOrder.mockRejectedValue(
      new ApiError('CHECKOUT_REVIEW_REQUIRED', 'stale', 400),
    )

    renderCheckout()
    await screen.findByRole('button', { name: 'Place order' })

    await user.type(screen.getByLabelText(/^Name$/i), 'Muskan')
    await user.type(screen.getByLabelText(/^Mobile number$/i), '9876543210')
    await user.type(screen.getByLabelText(/^Full name$/i), 'Muskan')
    await user.type(screen.getByLabelText(/^Mobile$/i), '9876543210')
    await user.type(screen.getByLabelText(/Address line 1/i), '12 Street')
    await user.type(screen.getByLabelText(/^City$/i), 'Mumbai')
    await user.type(screen.getByLabelText(/^State$/i), 'MH')
    await user.type(screen.getByLabelText(/PIN code/i), '400001')
    await user.click(screen.getByRole('button', { name: 'Place order' }))

    expect(await screen.findByText(/Refreshing preview/i)).toBeInTheDocument()
  })

  it('shows order success page summary', async () => {
    fetchOrderSummary.mockResolvedValue({
      orderNumber: 'MAC-OK',
      status: 'PLACED',
      paymentStatus: 'PAID',
      paymentMethod: 'UPI',
      grandTotal: 150,
      contactName: 'Muskan',
      items: [{ title: 'Bottle', quantity: 1, lineSubtotal: 150 }],
    })
    render(
      <MemoryRouter initialEntries={['/order-success/MAC-OK']}>
        <Routes>
          <Route path="/order-success/:orderNumber" element={<OrderSuccessPage />} />
        </Routes>
      </MemoryRouter>,
    )
    expect(await screen.findByText('MAC-OK')).toBeInTheDocument()
    expect(screen.getByText(/Payment confirmed/i)).toBeInTheDocument()
  })
})

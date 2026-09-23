import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('./apiClient', () => ({
  apiRequest: vi.fn(),
}))

vi.mock('./customerAuthStorage', () => ({
  isCustomerAuthenticated: vi.fn(() => false),
}))

import { apiRequest } from './apiClient'
import {
  initiatePayment,
  verifyPayment,
  reportPaymentFailure,
  buildRazorpayCheckoutOptions,
} from './paymentService'

describe('paymentService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('initiatePayment posts orderNumber and idempotencyKey only', async () => {
    apiRequest.mockResolvedValue({ razorpayOrderId: 'order_1', amount: 100 })
    await initiatePayment({ orderNumber: 'MAC-1', idempotencyKey: 'k1' })
    expect(apiRequest).toHaveBeenCalledWith(
      '/api/payments/initiate',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ orderNumber: 'MAC-1', idempotencyKey: 'k1' }),
      }),
    )
  })

  it('verifyPayment posts Razorpay callback fields', async () => {
    apiRequest.mockResolvedValue({ paid: true })
    await verifyPayment({
      orderNumber: 'MAC-1',
      razorpayOrderId: 'order_1',
      razorpayPaymentId: 'pay_1',
      razorpaySignature: 'sig',
    })
    expect(apiRequest).toHaveBeenCalledWith(
      '/api/payments/verify',
      expect.objectContaining({
        method: 'POST',
      }),
    )
  })

  it('reportPaymentFailure posts order number', async () => {
    apiRequest.mockResolvedValue({ paid: false })
    await reportPaymentFailure({ orderNumber: 'MAC-1' })
    expect(apiRequest).toHaveBeenCalledWith(
      '/api/payments/fail',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ orderNumber: 'MAC-1' }),
      }),
    )
  })

  it('buildRazorpayCheckoutOptions uses public key and order id', () => {
    const options = buildRazorpayCheckoutOptions({
      keyId: 'rzp_test_x',
      razorpayOrderId: 'order_1',
      amount: 56700,
      currency: 'INR',
    })
    expect(options.key).toBe('rzp_test_x')
    expect(options.order_id).toBe('order_1')
    expect(options.amount).toBe(56700)
  })
})

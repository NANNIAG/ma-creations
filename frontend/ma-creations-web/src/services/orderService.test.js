import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createIdempotencyKey, fetchOrderSummary, placeOrder } from './orderService'

vi.mock('./apiClient', () => ({
  apiRequest: vi.fn(),
}))

vi.mock('./cartStorage', () => ({
  getCartToken: vi.fn(),
}))

vi.mock('./customerAuthStorage', () => ({
  isCustomerAuthenticated: vi.fn(),
}))

import { apiRequest } from './apiClient'
import { getCartToken } from './cartStorage'
import { isCustomerAuthenticated } from './customerAuthStorage'

describe('orderService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('places guest order with cart token and no money fields', async () => {
    isCustomerAuthenticated.mockReturnValue(false)
    getCartToken.mockReturnValue('guest-token')
    apiRequest.mockResolvedValue({ orderNumber: 'MAC-1' })

    await placeOrder({
      previewHash: 'abc',
      paymentMethod: 'COD',
      idempotencyKey: 'idem-1',
      contactName: 'Muskan',
      contactMobile: '9876543210',
      shippingAddress: {
        fullName: 'Muskan',
        mobile: '9876543210',
        line1: 'A-1',
        city: 'Mumbai',
        state: 'MH',
        postalCode: '400001',
        country: 'India',
      },
    })

    expect(apiRequest).toHaveBeenCalledWith(
      '/api/orders',
      expect.objectContaining({
        method: 'POST',
        auth: 'none',
        headers: expect.objectContaining({ 'X-Cart-Token': 'guest-token' }),
      }),
    )
    const body = JSON.parse(apiRequest.mock.calls[0][1].body)
    expect(body).not.toHaveProperty('grandTotal')
    expect(body).not.toHaveProperty('customerId')
    expect(body.previewHash).toBe('abc')
    expect(body.paymentMethod).toBe('COD')
  })

  it('places authenticated customer order without guest token', async () => {
    isCustomerAuthenticated.mockReturnValue(true)
    getCartToken.mockReturnValue('guest-token')
    apiRequest.mockResolvedValue({ orderNumber: 'MAC-2' })

    await placeOrder({
      previewHash: 'hash',
      paymentMethod: 'UPI',
      idempotencyKey: 'idem-2',
      contactName: 'Asha',
      contactMobile: '9123456780',
      shippingAddress: {
        fullName: 'Asha',
        mobile: '9123456780',
        line1: 'B-2',
        city: 'Pune',
        state: 'MH',
        postalCode: '411001',
        country: 'India',
      },
    })

    expect(apiRequest).toHaveBeenCalledWith(
      '/api/orders',
      expect.objectContaining({
        auth: 'customer',
        headers: expect.not.objectContaining({ 'X-Cart-Token': 'guest-token' }),
      }),
    )
  })

  it('fetches order summary', async () => {
    isCustomerAuthenticated.mockReturnValue(false)
    apiRequest.mockResolvedValue({ orderNumber: 'MAC-9' })
    await fetchOrderSummary('MAC-9')
    expect(apiRequest).toHaveBeenCalledWith(
      '/api/orders/MAC-9',
      expect.objectContaining({ method: 'GET', auth: 'none' }),
    )
  })

  it('creates idempotency keys', () => {
    expect(createIdempotencyKey()).toBeTruthy()
  })
})

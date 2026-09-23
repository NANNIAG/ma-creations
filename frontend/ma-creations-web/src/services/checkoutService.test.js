import { beforeEach, describe, expect, it, vi } from 'vitest'
import { previewCheckout } from './checkoutService'

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

describe('checkoutService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('posts preview with guest cart token when unauthenticated', async () => {
    isCustomerAuthenticated.mockReturnValue(false)
    getCartToken.mockReturnValue('guest-token')
    apiRequest.mockResolvedValue({ valid: true, readyToPlace: false })

    await previewCheckout({ paymentMethod: 'UPI', lastSeenPrices: { 5: 100 } })

    expect(apiRequest).toHaveBeenCalledWith(
      '/api/checkout/preview',
      expect.objectContaining({
        method: 'POST',
        auth: 'none',
        headers: expect.objectContaining({
          'X-Cart-Token': 'guest-token',
        }),
        body: JSON.stringify({
          paymentMethod: 'UPI',
          lastSeenPrices: { 5: 100 },
        }),
      }),
    )
  })

  it('uses customer auth without guest token when authenticated', async () => {
    isCustomerAuthenticated.mockReturnValue(true)
    getCartToken.mockReturnValue('guest-token')
    apiRequest.mockResolvedValue({ valid: true })

    await previewCheckout({ paymentMethod: 'COD' })

    expect(apiRequest).toHaveBeenCalledWith(
      '/api/checkout/preview',
      expect.objectContaining({
        auth: 'customer',
        headers: expect.not.objectContaining({
          'X-Cart-Token': 'guest-token',
        }),
      }),
    )
  })
})

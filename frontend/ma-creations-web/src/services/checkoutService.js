import { apiRequest } from './apiClient'
import { getCartToken } from './cartStorage'
import { isCustomerAuthenticated } from './customerAuthStorage'

const CART_TOKEN_HEADER = 'X-Cart-Token'

/**
 * Calls POST /api/checkout/preview.
 * Uses customer JWT when logged in; otherwise guest cart token.
 * Does not create an order.
 */
export async function previewCheckout({ paymentMethod, lastSeenPrices } = {}) {
  if (!paymentMethod) {
    throw new Error('paymentMethod is required')
  }

  const headers = {
    'Content-Type': 'application/json',
  }
  const guestToken = getCartToken()
  if (!isCustomerAuthenticated() && guestToken) {
    headers[CART_TOKEN_HEADER] = guestToken
  }

  return apiRequest('/api/checkout/preview', {
    method: 'POST',
    auth: isCustomerAuthenticated() ? 'customer' : 'none',
    headers,
    body: JSON.stringify({
      paymentMethod,
      lastSeenPrices: lastSeenPrices || {},
    }),
  })
}

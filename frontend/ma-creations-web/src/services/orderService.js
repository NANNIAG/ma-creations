import { apiRequest } from './apiClient'
import { getCartToken } from './cartStorage'
import { isCustomerAuthenticated } from './customerAuthStorage'

const CART_TOKEN_HEADER = 'X-Cart-Token'

function cartAuthHeaders() {
  const headers = {
    'Content-Type': 'application/json',
  }
  const guestToken = getCartToken()
  if (!isCustomerAuthenticated() && guestToken) {
    headers[CART_TOKEN_HEADER] = guestToken
  }
  return headers
}

/**
 * POST /api/orders — creates order from current cart.
 * Never send totals, customerId, or order status from the client.
 */
export async function placeOrder(payload) {
  if (!payload?.previewHash) {
    throw new Error('previewHash is required')
  }
  if (!payload?.paymentMethod) {
    throw new Error('paymentMethod is required')
  }
  if (!payload?.idempotencyKey) {
    throw new Error('idempotencyKey is required')
  }

  return apiRequest('/api/orders', {
    method: 'POST',
    auth: isCustomerAuthenticated() ? 'customer' : 'none',
    headers: cartAuthHeaders(),
    body: JSON.stringify({
      previewHash: payload.previewHash,
      paymentMethod: payload.paymentMethod,
      idempotencyKey: payload.idempotencyKey,
      contactName: payload.contactName,
      contactMobile: payload.contactMobile,
      contactEmail: payload.contactEmail || undefined,
      shippingAddress: payload.shippingAddress,
    }),
  })
}

/**
 * GET /api/orders/{orderNumber} — confirmation summary only.
 */
export async function fetchOrderSummary(orderNumber) {
  if (!orderNumber) {
    throw new Error('orderNumber is required')
  }
  return apiRequest(`/api/orders/${encodeURIComponent(orderNumber)}`, {
    method: 'GET',
    auth: isCustomerAuthenticated() ? 'customer' : 'none',
  })
}

export function createIdempotencyKey() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return `idem-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

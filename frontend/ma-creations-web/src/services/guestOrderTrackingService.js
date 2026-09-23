import { apiRequest } from './apiClient'

/**
 * GET /api/orders/track — guest tracking (order number + mobile).
 * No auth. Never sends customerId.
 */
export async function trackGuestOrder({ orderNumber, mobileNumber }) {
  if (!orderNumber || !String(orderNumber).trim()) {
    throw new Error('Order number is required')
  }
  if (!mobileNumber || !String(mobileNumber).trim()) {
    throw new Error('Mobile number is required')
  }
  const params = new URLSearchParams()
  params.set('orderNumber', String(orderNumber).trim())
  params.set('mobileNumber', String(mobileNumber).trim())
  return apiRequest(`/api/orders/track?${params.toString()}`, {
    method: 'GET',
    auth: 'none',
  })
}

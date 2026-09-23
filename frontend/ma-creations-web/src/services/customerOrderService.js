import { apiRequest } from './apiClient'

/**
 * GET /api/customer/orders — authenticated customer's orders only.
 * customer_id is never sent from the client.
 */
export async function fetchCustomerOrders({ page = 0, size = 20 } = {}) {
  const params = new URLSearchParams()
  params.set('page', String(page))
  params.set('size', String(size))
  return apiRequest(`/api/customer/orders?${params.toString()}`, {
    method: 'GET',
    auth: 'customer',
  })
}

/**
 * GET /api/customer/orders/{orderNumber}
 */
export async function fetchCustomerOrder(orderNumber) {
  if (!orderNumber) {
    throw new Error('orderNumber is required')
  }
  return apiRequest(`/api/customer/orders/${encodeURIComponent(orderNumber)}`, {
    method: 'GET',
    auth: 'customer',
  })
}

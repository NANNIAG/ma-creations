import { apiRequest } from './apiClient'

/**
 * GET /api/admin/orders
 */
export async function fetchAdminOrders({
  q = '',
  status = '',
  paymentStatus = '',
  page = 0,
  size = 20,
} = {}) {
  const params = new URLSearchParams()
  params.set('page', String(page))
  params.set('size', String(size))
  if (q) params.set('q', q)
  if (status) params.set('status', status)
  if (paymentStatus) params.set('paymentStatus', paymentStatus)
  return apiRequest(`/api/admin/orders?${params.toString()}`, {
    method: 'GET',
    auth: 'admin',
  })
}

/**
 * GET /api/admin/orders/{orderNumber}
 */
export async function fetchAdminOrder(orderNumber) {
  if (!orderNumber) {
    throw new Error('orderNumber is required')
  }
  return apiRequest(`/api/admin/orders/${encodeURIComponent(orderNumber)}`, {
    method: 'GET',
    auth: 'admin',
  })
}

/**
 * PATCH /api/admin/orders/{orderNumber}/status
 * Updates OrderStatus only — never PaymentStatus.
 */
export async function updateAdminOrderStatus(orderNumber, status) {
  if (!orderNumber) {
    throw new Error('orderNumber is required')
  }
  if (!status) {
    throw new Error('status is required')
  }
  return apiRequest(`/api/admin/orders/${encodeURIComponent(orderNumber)}/status`, {
    method: 'PATCH',
    auth: 'admin',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  })
}

/**
 * PATCH /api/admin/orders/{orderNumber}/tracking
 * Manual AWB / tracking number only — no provider or URL.
 */
export async function updateAdminOrderTracking(orderNumber, trackingNumber) {
  if (!orderNumber) {
    throw new Error('orderNumber is required')
  }
  if (!trackingNumber || !String(trackingNumber).trim()) {
    throw new Error('trackingNumber is required')
  }
  return apiRequest(`/api/admin/orders/${encodeURIComponent(orderNumber)}/tracking`, {
    method: 'PATCH',
    auth: 'admin',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ trackingNumber: String(trackingNumber).trim() }),
  })
}

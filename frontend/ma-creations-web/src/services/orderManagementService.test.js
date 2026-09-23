import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('./apiClient', () => ({
  apiRequest: vi.fn(),
}))

import { apiRequest } from './apiClient'
import { fetchCustomerOrder, fetchCustomerOrders } from './customerOrderService'
import {
  fetchAdminOrder,
  fetchAdminOrders,
  updateAdminOrderStatus,
} from './adminOrderService'

describe('customerOrderService', () => {
  beforeEach(() => {
    apiRequest.mockReset()
  })

  it('lists orders with customer auth and never sends customerId', async () => {
    apiRequest.mockResolvedValue({ content: [] })
    await fetchCustomerOrders({ page: 1, size: 10 })
    expect(apiRequest).toHaveBeenCalledWith(
      '/api/customer/orders?page=1&size=10',
      expect.objectContaining({ method: 'GET', auth: 'customer' }),
    )
    const [, options] = apiRequest.mock.calls[0]
    expect(JSON.stringify(options)).not.toMatch(/customerId|customer_id/i)
  })

  it('fetches detail by order number', async () => {
    apiRequest.mockResolvedValue({ orderNumber: 'MAC-1' })
    await fetchCustomerOrder('MAC-1')
    expect(apiRequest).toHaveBeenCalledWith(
      '/api/customer/orders/MAC-1',
      expect.objectContaining({ auth: 'customer' }),
    )
  })
})

describe('adminOrderService', () => {
  beforeEach(() => {
    apiRequest.mockReset()
  })

  it('passes search and filters', async () => {
    apiRequest.mockResolvedValue({ content: [] })
    await fetchAdminOrders({
      q: '98765',
      status: 'PLACED',
      paymentStatus: 'PAID',
      page: 0,
      size: 20,
    })
    expect(apiRequest).toHaveBeenCalledWith(
      expect.stringContaining('/api/admin/orders?'),
      expect.objectContaining({ auth: 'admin' }),
    )
    const [path] = apiRequest.mock.calls[0]
    expect(path).toContain('q=98765')
    expect(path).toContain('status=PLACED')
    expect(path).toContain('paymentStatus=PAID')
  })

  it('updates status only', async () => {
    apiRequest.mockResolvedValue({ orderStatus: 'PROCESSING' })
    await updateAdminOrderStatus('MAC-1', 'PROCESSING')
    expect(apiRequest).toHaveBeenCalledWith(
      '/api/admin/orders/MAC-1/status',
      expect.objectContaining({
        method: 'PATCH',
        auth: 'admin',
        body: JSON.stringify({ status: 'PROCESSING' }),
      }),
    )
  })

  it('fetches admin detail', async () => {
    apiRequest.mockResolvedValue({ orderNumber: 'MAC-1' })
    await fetchAdminOrder('MAC-1')
    expect(apiRequest).toHaveBeenCalledWith(
      '/api/admin/orders/MAC-1',
      expect.objectContaining({ auth: 'admin' }),
    )
  })
})

import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Button from '../../components/Button/Button'
import ErrorState from '../../components/ErrorState/ErrorState'
import LoadingState from '../../components/Loading/LoadingState'
import {
  formatInr,
  formatOrderDate,
  OrderStatusBadge,
  PaymentStatusBadge,
  statusLabel,
} from '../../orders/orderDisplay'
import { fetchAdminOrders } from '../../services/adminOrderService'

const ORDER_STATUSES = [
  '',
  'PENDING_PAYMENT',
  'PLACED',
  'PROCESSING',
  'SHIPPED',
  'DELIVERED',
  'PAYMENT_FAILED',
  'CANCELLED',
]

const PAYMENT_STATUSES = ['', 'PENDING', 'PAID', 'FAILED', 'COD_PENDING']

export default function AdminOrdersPage() {
  const [pageData, setPageData] = useState(null)
  const [page, setPage] = useState(0)
  const [q, setQ] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [status, setStatus] = useState('')
  const [paymentStatus, setPaymentStatus] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(
    async (pageIndex, overrides = {}) => {
      setLoading(true)
      setError('')
      try {
        const data = await fetchAdminOrders({
          page: pageIndex,
          size: 20,
          q: overrides.q !== undefined ? overrides.q : q,
          status: overrides.status !== undefined ? overrides.status : status,
          paymentStatus:
            overrides.paymentStatus !== undefined ? overrides.paymentStatus : paymentStatus,
        })
        setPageData(data)
        setPage(pageIndex)
      } catch (err) {
        setError(err.message || 'Could not load orders')
        setPageData(null)
      } finally {
        setLoading(false)
      }
    },
    [q, status, paymentStatus],
  )

  useEffect(() => {
    load(0)
  }, [load])

  function handleSearch(event) {
    event.preventDefault()
    const nextQ = searchInput.trim()
    setQ(nextQ)
    load(0, { q: nextQ })
  }

  if (loading && !pageData) {
    return <LoadingState label="Loading orders…" />
  }

  if (error && !pageData) {
    return <ErrorState message={error} onRetry={() => load(page)} />
  }

  const orders = pageData?.content || []
  const totalPages = pageData?.totalPages ?? 0

  return (
    <div>
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">Orders</h1>
          <p className="mt-1 text-sm text-ma-muted">Fulfillment and payment overview.</p>
        </div>
      </div>

      <form
        onSubmit={handleSearch}
        className="mt-6 flex flex-col gap-3 rounded-xl bg-ma-surface p-4 ring-1 ring-ma-border md:flex-row md:flex-wrap md:items-end"
      >
        <label className="flex min-w-[12rem] flex-1 flex-col gap-1 text-xs font-semibold uppercase tracking-wide text-ma-muted">
          Search
          <input
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Order number or mobile"
            className="rounded-lg border border-ma-border bg-ma-bg px-3 py-2 text-sm font-normal normal-case text-ma-text"
          />
        </label>
        <label className="flex flex-col gap-1 text-xs font-semibold uppercase tracking-wide text-ma-muted">
          Order status
          <select
            value={status}
            onChange={(e) => {
              const next = e.target.value
              setStatus(next)
              load(0, { status: next })
            }}
            className="rounded-lg border border-ma-border bg-ma-bg px-3 py-2 text-sm font-normal normal-case text-ma-text"
          >
            {ORDER_STATUSES.map((value) => (
              <option key={value || 'all'} value={value}>
                {value ? statusLabel(value) : 'All'}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-1 text-xs font-semibold uppercase tracking-wide text-ma-muted">
          Payment status
          <select
            value={paymentStatus}
            onChange={(e) => {
              const next = e.target.value
              setPaymentStatus(next)
              load(0, { paymentStatus: next })
            }}
            className="rounded-lg border border-ma-border bg-ma-bg px-3 py-2 text-sm font-normal normal-case text-ma-text"
          >
            {PAYMENT_STATUSES.map((value) => (
              <option key={value || 'all'} value={value}>
                {value ? statusLabel(value) : 'All'}
              </option>
            ))}
          </select>
        </label>
        <Button type="submit" size="sm">
          Search
        </Button>
      </form>

      {error && (
        <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800" role="alert">
          {error}
        </p>
      )}

      {orders.length === 0 ? (
        <div className="mt-10 rounded-xl bg-ma-surface p-8 text-center ring-1 ring-ma-border">
          <p className="font-medium">No orders found</p>
          <p className="mt-2 text-sm text-ma-muted">Try clearing filters or search.</p>
        </div>
      ) : (
        <div className="mt-6 overflow-x-auto rounded-xl ring-1 ring-ma-border">
          <table className="min-w-full divide-y divide-ma-border text-left text-sm">
            <thead className="bg-ma-surface text-xs uppercase tracking-wide text-ma-muted">
              <tr>
                <th className="px-3 py-3">Order</th>
                <th className="px-3 py-3">Date</th>
                <th className="px-3 py-3">Customer</th>
                <th className="px-3 py-3">Total</th>
                <th className="px-3 py-3">Payment</th>
                <th className="px-3 py-3">Status</th>
                <th className="px-3 py-3"> </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ma-border bg-ma-bg">
              {orders.map((order) => (
                <tr key={order.orderNumber}>
                  <td className="px-3 py-3 font-medium">{order.orderNumber}</td>
                  <td className="px-3 py-3 whitespace-nowrap text-ma-muted">
                    {formatOrderDate(order.placedAt)}
                  </td>
                  <td className="px-3 py-3">
                    <div>{order.contactName || '—'}</div>
                    <div className="text-xs text-ma-muted">{order.contactMobile || '—'}</div>
                  </td>
                  <td className="px-3 py-3 whitespace-nowrap">{formatInr(order.grandTotal)}</td>
                  <td className="px-3 py-3">
                    <div className="flex flex-col gap-1">
                      <span className="text-xs text-ma-muted">{statusLabel(order.paymentMethod)}</span>
                      <PaymentStatusBadge status={order.paymentStatus} />
                    </div>
                  </td>
                  <td className="px-3 py-3">
                    <OrderStatusBadge status={order.orderStatus} />
                  </td>
                  <td className="px-3 py-3">
                    <Link to={`/admin/orders/${encodeURIComponent(order.orderNumber)}`}>
                      <Button variant="secondary" size="sm">
                        View details
                      </Button>
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {totalPages > 1 && (
        <div className="mt-6 flex items-center justify-between gap-3">
          <Button
            variant="secondary"
            size="sm"
            disabled={page <= 0 || loading}
            onClick={() => load(page - 1)}
          >
            Previous
          </Button>
          <span className="text-xs text-ma-muted">
            Page {page + 1} of {totalPages}
          </span>
          <Button
            variant="secondary"
            size="sm"
            disabled={page + 1 >= totalPages || loading}
            onClick={() => load(page + 1)}
          >
            Next
          </Button>
        </div>
      )}
    </div>
  )
}

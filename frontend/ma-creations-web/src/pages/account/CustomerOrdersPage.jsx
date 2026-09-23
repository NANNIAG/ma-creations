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
} from '../../orders/orderDisplay'
import { fetchCustomerOrders } from '../../services/customerOrderService'

export default function CustomerOrdersPage() {
  const [pageData, setPageData] = useState(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async (pageIndex) => {
    setLoading(true)
    setError('')
    try {
      const data = await fetchCustomerOrders({ page: pageIndex, size: 20 })
      setPageData(data)
      setPage(pageIndex)
    } catch (err) {
      setError(err.message || 'Could not load orders')
      setPageData(null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load(0)
  }, [load])

  if (loading && !pageData) {
    return <LoadingState label="Loading orders…" />
  }

  if (error && !pageData) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-10 md:px-8">
        <ErrorState message={error} onRetry={() => load(page)} />
      </div>
    )
  }

  const orders = pageData?.content || []
  const totalPages = pageData?.totalPages ?? 0

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 md:px-8">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="text-sm font-semibold uppercase tracking-wide text-ma-primary">Account</p>
          <h1 className="mt-1 font-display text-3xl text-ma-text">Orders</h1>
          <p className="mt-1 text-sm text-ma-muted">Your recent purchases at MA CREATIONS.</p>
        </div>
        <Link to="/account" className="text-sm font-semibold text-ma-primary hover:underline">
          Back to account
        </Link>
      </div>

      {error && (
        <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800" role="alert">
          {error}
        </p>
      )}

      {orders.length === 0 ? (
        <div className="mt-10 rounded-xl bg-ma-surface p-8 text-center ring-1 ring-ma-border">
          <p className="text-base font-medium text-ma-text">No orders yet</p>
          <p className="mt-2 text-sm text-ma-muted">When you place an order, it will show up here.</p>
          <Link to="/" className="mt-6 inline-block">
            <Button>Continue shopping</Button>
          </Link>
        </div>
      ) : (
        <ul className="mt-8 space-y-3">
          {orders.map((order) => (
            <li
              key={order.orderNumber}
              className="rounded-xl bg-ma-surface p-4 ring-1 ring-ma-border md:p-5"
            >
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="text-sm font-semibold text-ma-text">{order.orderNumber}</p>
                  <p className="mt-1 text-xs text-ma-muted">{formatOrderDate(order.placedAt)}</p>
                  <p className="mt-2 text-sm text-ma-muted">
                    {order.itemCount} {order.itemCount === 1 ? 'item' : 'items'} ·{' '}
                    {formatInr(order.grandTotal)}
                  </p>
                </div>
                <div className="flex flex-col items-end gap-2">
                  <OrderStatusBadge status={order.orderStatus} />
                  <PaymentStatusBadge status={order.paymentStatus} />
                  <Link to={`/account/orders/${encodeURIComponent(order.orderNumber)}`}>
                    <Button variant="secondary" size="sm">
                      View details
                    </Button>
                  </Link>
                </div>
              </div>
            </li>
          ))}
        </ul>
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

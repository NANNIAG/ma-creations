import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import Button from '../../components/Button/Button'
import LoadingState from '../../components/Loading/LoadingState'
import { fetchOrderSummary } from '../../services/orderService'

function formatInr(value) {
  const number = Number(value)
  if (Number.isNaN(number)) {
    return String(value ?? '')
  }
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(number)
}

function statusLabel(status) {
  if (!status) return '—'
  return String(status).replaceAll('_', ' ')
}

export default function OrderSuccessPage() {
  const { orderNumber } = useParams()
  const location = useLocation()
  const [order, setOrder] = useState(location.state?.order || null)
  const [loading, setLoading] = useState(!location.state?.order)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    async function load() {
      if (!orderNumber) {
        setError('Missing order number')
        setLoading(false)
        return
      }
      try {
        const summary = await fetchOrderSummary(orderNumber)
        if (!cancelled) {
          setOrder((prev) => ({ ...(prev || {}), ...summary }))
        }
      } catch (err) {
        if (!cancelled) {
          setOrder((prev) => {
            if (!prev) {
              setError(err.message || 'Could not load order')
            }
            return prev
          })
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [orderNumber])

  if (loading) {
    return <LoadingState label="Loading order…" />
  }

  const paymentStatus = order?.paymentStatus || location.state?.verification?.paymentStatus
  const isCod = order?.paymentMethod === 'COD' || paymentStatus === 'COD_PENDING'

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 md:px-8">
      <div className="rounded-2xl bg-gradient-to-br from-ma-surface via-ma-bg to-ma-surface p-6 ring-1 ring-ma-border md:p-8">
        <p className="text-sm font-semibold uppercase tracking-wide text-ma-primary">MA CREATIONS</p>
        <h1 className="mt-2 font-display text-3xl text-ma-text md:text-4xl">
          {isCod ? 'Order placed' : 'Payment confirmed'}
        </h1>
        <p className="mt-2 text-ma-muted">
          {isCod
            ? 'Your COD order has been placed. Pay when it arrives.'
            : 'Thank you — your payment was verified by our servers.'}
        </p>

        {error && !order && <p className="mt-4 text-sm text-red-700">{error}</p>}

        <dl className="mt-6 space-y-3 text-sm">
          <div className="flex justify-between gap-4">
            <dt className="text-ma-muted">Order number</dt>
            <dd className="font-bold text-ma-text">{orderNumber}</dd>
          </div>
          <div className="flex justify-between gap-4">
            <dt className="text-ma-muted">Order status</dt>
            <dd className="font-medium">{statusLabel(order?.status)}</dd>
          </div>
          <div className="flex justify-between gap-4">
            <dt className="text-ma-muted">Payment status</dt>
            <dd className="font-medium">{statusLabel(paymentStatus)}</dd>
          </div>
          {order?.grandTotal != null && (
            <div className="flex justify-between gap-4">
              <dt className="text-ma-muted">Grand total</dt>
              <dd className="font-bold">{formatInr(order.grandTotal)}</dd>
            </div>
          )}
          {order?.contactName && (
            <div className="flex justify-between gap-4">
              <dt className="text-ma-muted">Contact</dt>
              <dd className="text-right">
                {order.contactName}
                {order.contactMobile ? ` · ${order.contactMobile}` : ''}
              </dd>
            </div>
          )}
          {(order?.shippingCity || order?.shippingPostalCode) && (
            <div className="flex justify-between gap-4">
              <dt className="text-ma-muted">Ship to</dt>
              <dd className="text-right">
                {[order.shippingCity, order.shippingState, order.shippingPostalCode]
                  .filter(Boolean)
                  .join(', ')}
              </dd>
            </div>
          )}
        </dl>

        {order?.items?.length > 0 && (
          <ul className="mt-6 space-y-2 border-t border-ma-border pt-4 text-sm">
            {order.items.map((item, idx) => (
              <li key={`${item.productId}-${idx}`} className="flex justify-between gap-3">
                <span>
                  {item.title} × {item.quantity}
                </span>
                <span className="font-medium">{formatInr(item.lineSubtotal)}</span>
              </li>
            ))}
          </ul>
        )}

        <div className="mt-8 flex flex-wrap gap-3">
          <Link to="/">
            <Button type="button">Continue shopping</Button>
          </Link>
          <Link to="/cart" className="inline-flex items-center text-sm font-semibold text-ma-primary hover:underline">
            Back to cart
          </Link>
        </div>
      </div>
    </div>
  )
}

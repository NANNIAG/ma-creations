import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import Button from '../../components/Button/Button'
import ErrorState from '../../components/ErrorState/ErrorState'
import LoadingState from '../../components/Loading/LoadingState'
import {
  formatInr,
  formatOrderDate,
  nextAllowedOrderStatuses,
  OrderStatusBadge,
  PaymentStatusBadge,
  statusLabel,
} from '../../orders/orderDisplay'
import {
  fetchAdminOrder,
  updateAdminOrderStatus,
  updateAdminOrderTracking,
} from '../../services/adminOrderService'

export default function AdminOrderDetailPage() {
  const { orderNumber } = useParams()
  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [statusDraft, setStatusDraft] = useState('')
  const [trackingDraft, setTrackingDraft] = useState('')
  const [saving, setSaving] = useState(false)
  const [savingTracking, setSavingTracking] = useState(false)
  const [feedback, setFeedback] = useState('')
  const [trackingError, setTrackingError] = useState('')

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError('')
      try {
        const data = await fetchAdminOrder(orderNumber)
        if (!cancelled) {
          setOrder(data)
          setStatusDraft(data.orderStatus || '')
          setTrackingDraft(data.trackingNumber || '')
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message || 'Could not load order')
          setOrder(null)
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [orderNumber])

  async function handleStatusUpdate(event) {
    event.preventDefault()
    if (!statusDraft || statusDraft === order?.orderStatus) {
      return
    }
    setSaving(true)
    setFeedback('')
    setError('')
    try {
      const updated = await updateAdminOrderStatus(orderNumber, statusDraft)
      setOrder(updated)
      setStatusDraft(updated.orderStatus)
      setTrackingDraft(updated.trackingNumber || '')
      setFeedback(`Order status updated to ${statusLabel(updated.orderStatus)}.`)
    } catch (err) {
      setError(err.message || 'Could not update status')
    } finally {
      setSaving(false)
    }
  }

  async function handleTrackingSave(event) {
    event.preventDefault()
    const value = trackingDraft.trim()
    if (!value) {
      setTrackingError('Tracking / AWB number is required')
      return
    }
    if (value.length > 64) {
      setTrackingError('Tracking number must be at most 64 characters')
      return
    }
    setSavingTracking(true)
    setTrackingError('')
    setFeedback('')
    setError('')
    try {
      const updated = await updateAdminOrderTracking(orderNumber, value)
      setOrder(updated)
      setTrackingDraft(updated.trackingNumber || '')
      setFeedback('Tracking number saved.')
    } catch (err) {
      setTrackingError(err.message || 'Could not save tracking')
    } finally {
      setSavingTracking(false)
    }
  }

  if (loading) {
    return <LoadingState label="Loading order…" />
  }

  if ((error && !order) || !order) {
    return (
      <div>
        <ErrorState message={error || 'Order not found'} />
        <Link to="/admin/orders" className="mt-6 inline-block">
          <Button variant="secondary">Back to orders</Button>
        </Link>
      </div>
    )
  }

  const allowedNext = nextAllowedOrderStatuses(order.orderStatus)
  const statusOptions = [order.orderStatus, ...allowedNext.filter((s) => s !== order.orderStatus)]
  const address = order.shippingAddress
  const items = order.items || []
  const txs = order.paymentTransactions || []
  const showCod = Number(order.codCharge) > 0
  const showDiscount = Number(order.discountAmount) > 0
  const trackingUnchanged = trackingDraft.trim() === (order.trackingNumber || '')

  return (
    <div>
      <Link to="/admin/orders" className="text-sm font-semibold text-ma-primary hover:underline">
        ← Orders
      </Link>
      <h1 className="mt-3 text-2xl font-bold">{order.orderNumber}</h1>
      <p className="mt-1 text-sm text-ma-muted">{formatOrderDate(order.placedAt)}</p>

      <div className="mt-4 flex flex-wrap gap-2">
        <OrderStatusBadge status={order.orderStatus} />
        <PaymentStatusBadge status={order.paymentStatus} />
        <span className="inline-flex rounded-md bg-ma-surface px-2 py-0.5 text-xs font-medium ring-1 ring-ma-border">
          {statusLabel(order.paymentMethod)}
        </span>
      </div>

      {feedback && (
        <p className="mt-4 rounded-lg bg-green-50 px-3 py-2 text-sm text-green-800" role="status">
          {feedback}
        </p>
      )}
      {error && (
        <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800" role="alert">
          {error}
        </p>
      )}

      <section className="mt-8 grid gap-6 md:grid-cols-2">
        <div className="rounded-xl bg-ma-surface p-4 ring-1 ring-ma-border">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-ma-muted">Customer</h2>
          <dl className="mt-3 space-y-2 text-sm">
            <div>
              <dt className="text-xs text-ma-muted">Contact name</dt>
              <dd className="font-medium">{order.contactName || '—'}</dd>
            </div>
            <div>
              <dt className="text-xs text-ma-muted">Mobile</dt>
              <dd>{order.contactMobile || order.customerMobile || '—'}</dd>
            </div>
            {order.contactEmail && (
              <div>
                <dt className="text-xs text-ma-muted">Email</dt>
                <dd>{order.contactEmail}</dd>
              </div>
            )}
            {order.customerId != null && (
              <div>
                <dt className="text-xs text-ma-muted">Customer ID</dt>
                <dd>{order.customerId}</dd>
              </div>
            )}
          </dl>
        </div>

        <div className="rounded-xl bg-ma-surface p-4 ring-1 ring-ma-border">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-ma-muted">
            Update order status
          </h2>
          {allowedNext.length === 0 ? (
            <p className="mt-3 text-sm text-ma-muted">
              No further fulfillment transitions are available for this order.
            </p>
          ) : (
            <form onSubmit={handleStatusUpdate} className="mt-3 flex flex-wrap items-end gap-3">
              <label className="flex flex-col gap-1 text-xs font-semibold uppercase tracking-wide text-ma-muted">
                Status
                <select
                  value={statusDraft}
                  onChange={(e) => setStatusDraft(e.target.value)}
                  className="rounded-lg border border-ma-border bg-ma-bg px-3 py-2 text-sm font-normal normal-case text-ma-text"
                >
                  {statusOptions.map((value) => (
                    <option key={value} value={value}>
                      {statusLabel(value)}
                    </option>
                  ))}
                </select>
              </label>
              <Button type="submit" size="sm" disabled={saving || statusDraft === order.orderStatus}>
                {saving ? 'Saving…' : 'Save status'}
              </Button>
            </form>
          )}
          <p className="mt-3 text-xs text-ma-muted">
            Payment status is controlled by Razorpay / COD flows and cannot be changed here.
          </p>
        </div>
      </section>

      <section className="mt-8 rounded-xl bg-ma-surface p-4 ring-1 ring-ma-border md:p-5">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-ma-muted">Tracking</h2>
        <p className="mt-2 text-sm text-ma-muted">
          Enter the courier AWB / tracking number for this order. Status is updated separately
          (typically when the order is marked SHIPPED).
        </p>
        <form onSubmit={handleTrackingSave} className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end">
          <label className="flex min-w-0 flex-1 flex-col gap-1 text-xs font-semibold uppercase tracking-wide text-ma-muted">
            Tracking / AWB number
            <input
              type="text"
              value={trackingDraft}
              onChange={(e) => setTrackingDraft(e.target.value)}
              maxLength={64}
              placeholder="e.g. ABC123456789"
              className="rounded-lg border border-ma-border bg-ma-bg px-3 py-2 text-sm font-normal normal-case text-ma-text"
            />
          </label>
          <Button type="submit" size="sm" disabled={savingTracking || trackingUnchanged || !trackingDraft.trim()}>
            {savingTracking ? 'Saving…' : 'Save tracking'}
          </Button>
        </form>
        {trackingError && (
          <p className="mt-3 text-sm text-red-800" role="alert">
            {trackingError}
          </p>
        )}
      </section>

      <section className="mt-8">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-ma-muted">Items</h2>
        <ul className="mt-3 divide-y divide-ma-border rounded-xl bg-ma-surface ring-1 ring-ma-border">
          {items.map((item, index) => (
            <li key={`${item.title}-${index}`} className="flex flex-wrap justify-between gap-2 p-4">
              <div>
                <p className="font-medium">{item.title}</p>
                <p className="mt-1 text-xs text-ma-muted">
                  Qty {item.quantity} · {formatInr(item.unitSellingPrice)} each
                </p>
              </div>
              <p className="text-sm font-semibold">{formatInr(item.lineSubtotal)}</p>
            </li>
          ))}
        </ul>
      </section>

      <section className="mt-8 grid gap-6 md:grid-cols-2">
        <div className="rounded-xl bg-ma-surface p-4 ring-1 ring-ma-border">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-ma-muted">Totals</h2>
          <dl className="mt-3 space-y-2 text-sm">
            <div className="flex justify-between gap-4">
              <dt className="text-ma-muted">Subtotal</dt>
              <dd>{formatInr(order.itemsSubtotal)}</dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt className="text-ma-muted">Shipping</dt>
              <dd>{formatInr(order.shippingCharge)}</dd>
            </div>
            {showCod && (
              <div className="flex justify-between gap-4">
                <dt className="text-ma-muted">COD fee</dt>
                <dd>{formatInr(order.codCharge)}</dd>
              </div>
            )}
            {showDiscount && (
              <div className="flex justify-between gap-4">
                <dt className="text-ma-muted">Discount</dt>
                <dd>−{formatInr(order.discountAmount)}</dd>
              </div>
            )}
            <div className="flex justify-between gap-4 border-t border-ma-border pt-2 font-semibold">
              <dt>Grand total</dt>
              <dd>{formatInr(order.grandTotal)}</dd>
            </div>
          </dl>
          <p className="mt-3 text-xs text-ma-muted">Tax amount on order: ₹0 (not charged).</p>
        </div>

        <div className="rounded-xl bg-ma-surface p-4 ring-1 ring-ma-border">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-ma-muted">Address</h2>
          <div className="mt-3 space-y-1 text-sm">
            <p className="font-medium">{address?.fullName || order.contactName}</p>
            <p>{address?.mobile || order.contactMobile}</p>
            {address?.line1 && <p>{address.line1}</p>}
            {address?.line2 && <p>{address.line2}</p>}
            {address?.landmark && <p>Landmark: {address.landmark}</p>}
            <p>
              {[address?.city, address?.state, address?.postalCode].filter(Boolean).join(', ')}
            </p>
          </div>
        </div>
      </section>

      <section className="mt-8">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-ma-muted">
          Payment transactions
        </h2>
        {txs.length === 0 ? (
          <p className="mt-3 text-sm text-ma-muted">No payment transactions recorded.</p>
        ) : (
          <ul className="mt-3 space-y-2">
            {txs.map((tx) => (
              <li
                key={tx.id}
                className="rounded-xl bg-ma-surface p-4 text-sm ring-1 ring-ma-border"
              >
                <div className="flex flex-wrap justify-between gap-2">
                  <span className="font-medium">{statusLabel(tx.provider)}</span>
                  <PaymentStatusBadge status={tx.status} />
                </div>
                <p className="mt-2 text-ma-muted">
                  {formatInr(tx.amount)} · {statusLabel(tx.paymentMethod)}
                </p>
                {tx.providerOrderId && (
                  <p className="mt-1 text-xs text-ma-muted">Provider order: {tx.providerOrderId}</p>
                )}
                {tx.providerPaymentId && (
                  <p className="text-xs text-ma-muted">Provider payment: {tx.providerPaymentId}</p>
                )}
                {tx.failureCode && (
                  <p className="mt-1 text-xs text-red-700">Failure: {tx.failureCode}</p>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}

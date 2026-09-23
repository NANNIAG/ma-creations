import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
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
import { fetchCustomerOrder } from '../../services/customerOrderService'

function AddressBlock({ address, contactName, contactMobile, contactEmail }) {
  if (!address && !contactName) {
    return <p className="text-sm text-ma-muted">No address on file.</p>
  }
  return (
    <div className="space-y-1 text-sm text-ma-text">
      <p className="font-medium">{address?.fullName || contactName}</p>
      <p>{address?.mobile || contactMobile}</p>
      {(address?.email || contactEmail) && <p>{address?.email || contactEmail}</p>}
      {address?.line1 && <p>{address.line1}</p>}
      {address?.line2 && <p>{address.line2}</p>}
      {address?.landmark && <p>Landmark: {address.landmark}</p>}
      <p>
        {[address?.city, address?.state, address?.postalCode].filter(Boolean).join(', ')}
      </p>
      {address?.country && <p>{address.country}</p>}
    </div>
  )
}

export default function CustomerOrderDetailPage() {
  const { orderNumber } = useParams()
  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError('')
      try {
        const data = await fetchCustomerOrder(orderNumber)
        if (!cancelled) setOrder(data)
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

  if (loading) {
    return <LoadingState label="Loading order…" />
  }

  if (error || !order) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-10 md:px-8">
        <ErrorState message={error || 'Order not found'} />
        <Link to="/account/orders" className="mt-6 inline-block">
          <Button variant="secondary">Back to orders</Button>
        </Link>
      </div>
    )
  }

  const showCod = Number(order.codCharge) > 0
  const showDiscount = Number(order.discountAmount) > 0
  const items = order.items || []

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 md:px-8">
      <Link to="/account/orders" className="text-sm font-semibold text-ma-primary hover:underline">
        ← Orders
      </Link>
      <h1 className="mt-3 font-display text-3xl text-ma-text">{order.orderNumber}</h1>
      <p className="mt-1 text-sm text-ma-muted">{formatOrderDate(order.placedAt)}</p>

      <div className="mt-4 flex flex-wrap gap-2">
        <OrderStatusBadge status={order.orderStatus} />
        <PaymentStatusBadge status={order.paymentStatus} />
        <span className="inline-flex rounded-md bg-ma-surface px-2 py-0.5 text-xs font-medium ring-1 ring-ma-border">
          {statusLabel(order.paymentMethod)}
        </span>
      </div>

      <section className="mt-8 rounded-xl bg-ma-surface p-4 ring-1 ring-ma-border md:p-5">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-ma-muted">
          Shipment tracking
        </h2>
        {order.trackingNumber ? (
          <dl className="mt-3 text-sm">
            <dt className="text-ma-muted">Tracking / AWB number</dt>
            <dd className="mt-1 font-semibold text-ma-text">{order.trackingNumber}</dd>
          </dl>
        ) : (
          <p className="mt-3 text-sm text-ma-muted">
            Tracking information will be available after the order is shipped.
          </p>
        )}
      </section>

      <section className="mt-8">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-ma-muted">Items</h2>
        <ul className="mt-3 divide-y divide-ma-border rounded-xl bg-ma-surface ring-1 ring-ma-border">
          {items.map((item, index) => (
            <li key={`${item.title}-${index}`} className="flex flex-wrap justify-between gap-2 p-4">
              <div>
                <p className="font-medium text-ma-text">{item.title}</p>
                <p className="mt-1 text-xs text-ma-muted">
                  Qty {item.quantity} · {formatInr(item.unitSellingPrice)} each
                </p>
              </div>
              <p className="text-sm font-semibold">{formatInr(item.lineSubtotal)}</p>
            </li>
          ))}
        </ul>
      </section>

      <section className="mt-8 rounded-xl bg-ma-surface p-4 ring-1 ring-ma-border md:p-5">
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
          <div className="flex justify-between gap-4 border-t border-ma-border pt-2 text-base font-semibold">
            <dt>Grand total</dt>
            <dd>{formatInr(order.grandTotal)}</dd>
          </div>
        </dl>
        <p className="mt-3 text-xs text-ma-muted">Tax is not charged on this order (₹0).</p>
      </section>

      <section className="mt-8">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-ma-muted">
          Shipping address
        </h2>
        <div className="mt-3 rounded-xl bg-ma-surface p-4 ring-1 ring-ma-border">
          <AddressBlock
            address={order.shippingAddress}
            contactName={order.contactName}
            contactMobile={order.contactMobile}
            contactEmail={order.contactEmail}
          />
        </div>
      </section>
    </div>
  )
}

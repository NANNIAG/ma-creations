import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import Button from '../../components/Button/Button'
import { useCart } from '../../cart/CartContext'
import { ApiError } from '../../services/apiClient'
import { createIdempotencyKey } from '../../services/orderService'
import {
  initiatePayment,
  loadRazorpayScript,
  openRazorpayCheckout,
  reportPaymentFailure,
  verifyPayment,
} from '../../services/paymentService'

function statusLabel(status) {
  if (!status) return '—'
  return String(status).replaceAll('_', ' ')
}

export default function OrderPaymentFailedPage() {
  const { orderNumber } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const { refreshCart } = useCart()
  const [retrying, setRetrying] = useState(false)
  const [error, setError] = useState('')

  const message = location.state?.message || 'Payment was not completed.'
  const paymentStatus = location.state?.paymentStatus || 'FAILED'

  async function handleRetry() {
    if (!orderNumber) return
    setRetrying(true)
    setError('')
    try {
      const init = await initiatePayment({
        orderNumber,
        idempotencyKey: createIdempotencyKey(),
      })
      await loadRazorpayScript()
      await new Promise((resolve, reject) => {
        openRazorpayCheckout(init, {
          onSuccess: async (rzpResponse) => {
            try {
              const verified = await verifyPayment({
                orderNumber,
                razorpayOrderId: rzpResponse.razorpay_order_id,
                razorpayPaymentId: rzpResponse.razorpay_payment_id,
                razorpaySignature: rzpResponse.razorpay_signature,
              })
              resolve(verified)
            } catch (err) {
              reject(err)
            }
          },
          onDismiss: async () => {
            try {
              await reportPaymentFailure({ orderNumber })
            } catch {
              /* ignore */
            }
            reject(new ApiError('PAYMENT_CANCELLED', 'Payment was cancelled', 400))
          },
          onFailure: async () => {
            try {
              await reportPaymentFailure({ orderNumber })
            } catch {
              /* ignore */
            }
            reject(new ApiError('PAYMENT_FAILED', 'Payment failed', 400))
          },
        })
      })
      await refreshCart()
      navigate(`/order-success/${encodeURIComponent(orderNumber)}`)
    } catch (err) {
      setError(err.message || 'Retry failed')
    } finally {
      setRetrying(false)
    }
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 md:px-8">
      <div className="rounded-2xl bg-ma-surface p-6 ring-1 ring-ma-border md:p-8">
        <p className="text-sm font-semibold uppercase tracking-wide text-ma-primary">MA CREATIONS</p>
        <h1 className="mt-2 font-display text-3xl text-ma-text">Payment not completed</h1>
        <p className="mt-2 text-ma-muted">{message}</p>
        <p className="mt-1 text-xs text-ma-muted">
          Browser checkout success alone is not enough — payment must be verified by the server.
        </p>

        <dl className="mt-6 space-y-3 text-sm">
          <div className="flex justify-between gap-4">
            <dt className="text-ma-muted">Order number</dt>
            <dd className="font-bold">{orderNumber}</dd>
          </div>
          <div className="flex justify-between gap-4">
            <dt className="text-ma-muted">Payment status</dt>
            <dd className="font-medium">{statusLabel(paymentStatus)}</dd>
          </div>
        </dl>

        {error && (
          <p className="mt-4 text-sm text-red-700" role="alert">
            {error}
          </p>
        )}

        <div className="mt-8 flex flex-wrap gap-3">
          <Button onClick={handleRetry} disabled={retrying}>
            {retrying ? 'Retrying…' : 'Retry payment'}
          </Button>
          <Link
            to="/checkout"
            className="inline-flex items-center rounded-lg border border-ma-primary px-4 py-2.5 text-sm font-semibold text-ma-text hover:bg-ma-primary/10"
          >
            Return to checkout
          </Link>
          <Link to="/cart" className="inline-flex items-center text-sm font-semibold text-ma-primary hover:underline">
            Back to cart
          </Link>
        </div>
      </div>
    </div>
  )
}

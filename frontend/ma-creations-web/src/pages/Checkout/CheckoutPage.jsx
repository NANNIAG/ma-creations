import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Button from '../../components/Button/Button'
import ErrorState from '../../components/ErrorState/ErrorState'
import LoadingState from '../../components/Loading/LoadingState'
import { useCart } from '../../cart/CartContext'
import { useCustomerAuth } from '../../customer/CustomerAuthContext'
import { ApiError } from '../../services/apiClient'
import { previewCheckout } from '../../services/checkoutService'
import { createIdempotencyKey, placeOrder } from '../../services/orderService'
import {
  initiatePayment,
  loadRazorpayScript,
  openRazorpayCheckout,
  reportPaymentFailure,
  verifyPayment,
} from '../../services/paymentService'

const PAYMENT_METHODS = [
  { value: 'UPI', label: 'UPI' },
  { value: 'CARD', label: 'Credit/Debit Card' },
  { value: 'NET_BANKING', label: 'Net Banking' },
  { value: 'PAY_LATER', label: 'Pay Later' },
  { value: 'COD', label: 'Cash on Delivery' },
]

function formatInr(value) {
  if (value === null || value === undefined) {
    return null
  }
  const number = Number(value)
  if (Number.isNaN(number)) {
    return String(value)
  }
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(number)
}

function chargeLabel(value, pending) {
  if (pending) {
    return 'Will be calculated'
  }
  if (value === null || value === undefined) {
    return 'Unavailable'
  }
  return formatInr(value)
}

const emptyAddress = {
  fullName: '',
  mobile: '',
  email: '',
  line1: '',
  line2: '',
  landmark: '',
  city: '',
  state: '',
  postalCode: '',
  country: 'India',
}

export default function CheckoutPage() {
  const navigate = useNavigate()
  const { cart, loading: cartLoading, refreshCart } = useCart()
  const { customer, isAuthenticated } = useCustomerAuth()

  const [contactName, setContactName] = useState('')
  const [contactMobile, setContactMobile] = useState('')
  const [contactEmail, setContactEmail] = useState('')
  const [address, setAddress] = useState(emptyAddress)
  const [paymentMethod, setPaymentMethod] = useState('UPI')
  const [preview, setPreview] = useState(null)
  const [previewLoading, setPreviewLoading] = useState(false)
  const [previewError, setPreviewError] = useState('')
  const [formError, setFormError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState({})
  const idempotencyKeyRef = useRef(createIdempotencyKey())
  const prefilledRef = useRef(false)

  const items = cart?.items || []
  const isEmpty = !cartLoading && items.length === 0

  const lastSeenPrices = useMemo(() => {
    const map = {}
    items.forEach((item) => {
      if (item?.productId != null && item?.unitPrice != null) {
        map[item.productId] = item.unitPrice
      }
    })
    return map
  }, [items])

  useEffect(() => {
    if (prefilledRef.current || !isAuthenticated || !customer) {
      return
    }
    prefilledRef.current = true
    if (customer.name) {
      setContactName(customer.name)
      setAddress((prev) => ({ ...prev, fullName: customer.name }))
    }
    if (customer.mobileNumber) {
      setContactMobile(customer.mobileNumber)
      setAddress((prev) => ({ ...prev, mobile: customer.mobileNumber }))
    }
    if (customer.email) {
      setContactEmail(customer.email)
      setAddress((prev) => ({ ...prev, email: customer.email }))
    }
  }, [customer, isAuthenticated])

  const runPreview = useCallback(async () => {
    if (isEmpty) {
      setPreview(null)
      return
    }
    setPreviewLoading(true)
    setPreviewError('')
    try {
      const data = await previewCheckout({ paymentMethod, lastSeenPrices })
      setPreview(data)
    } catch (err) {
      setPreview(null)
      setPreviewError(err.message || 'Could not load checkout preview')
    } finally {
      setPreviewLoading(false)
    }
  }, [isEmpty, paymentMethod, lastSeenPrices])

  useEffect(() => {
    if (cartLoading || isEmpty) {
      return
    }
    const timer = window.setTimeout(() => {
      runPreview()
    }, 250)
    return () => window.clearTimeout(timer)
  }, [cartLoading, isEmpty, runPreview])

  function updateAddress(field, value) {
    setAddress((prev) => ({ ...prev, [field]: value }))
  }

  function validateForm() {
    const errors = {}
    if (!contactName.trim()) errors.contactName = 'Name is required'
    if (!/^[6-9]\d{9}$/.test(contactMobile.replace(/\D/g, '').slice(-10))) {
      errors.contactMobile = 'Enter a valid 10-digit mobile number'
    }
    if (!address.fullName.trim()) errors.fullName = 'Full name is required'
    if (!/^[6-9]\d{9}$/.test(address.mobile.replace(/\D/g, '').slice(-10))) {
      errors.mobile = 'Enter a valid 10-digit mobile number'
    }
    if (!address.line1.trim()) errors.line1 = 'Address line 1 is required'
    if (!address.city.trim()) errors.city = 'City is required'
    if (!address.state.trim()) errors.state = 'State is required'
    if (!address.postalCode.trim()) errors.postalCode = 'PIN code is required'
    if (!address.country.trim()) errors.country = 'Country is required'
    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function handleOnlinePayment(orderNumber, razorpayPayload) {
    await loadRazorpayScript()
    return new Promise((resolve, reject) => {
      openRazorpayCheckout(razorpayPayload, {
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
            /* still treat as cancelled */
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
  }

  async function handlePlaceOrder(event) {
    event.preventDefault()
    setFormError('')
    if (!validateForm()) {
      setFormError('Please fix the highlighted fields.')
      return
    }
    if (!preview?.readyToPlace || !preview?.previewHash) {
      setFormError('Checkout is not ready. Review totals and try again.')
      await runPreview()
      return
    }
    if (preview.requiresReview) {
      setFormError('Prices changed. Please review the updated totals.')
      return
    }

    setSubmitting(true)
    try {
      const placed = await placeOrder({
        previewHash: preview.previewHash,
        paymentMethod,
        idempotencyKey: idempotencyKeyRef.current,
        contactName: contactName.trim(),
        contactMobile: contactMobile.trim(),
        contactEmail: contactEmail.trim() || undefined,
        shippingAddress: {
          fullName: address.fullName.trim(),
          mobile: address.mobile.trim(),
          email: address.email.trim() || contactEmail.trim() || undefined,
          line1: address.line1.trim(),
          line2: address.line2.trim() || undefined,
          landmark: address.landmark.trim() || undefined,
          city: address.city.trim(),
          state: address.state.trim(),
          postalCode: address.postalCode.trim(),
          country: address.country.trim() || 'India',
        },
      })

      if (paymentMethod === 'COD' || !placed.requiresOnlinePayment) {
        await refreshCart()
        navigate(`/order-success/${encodeURIComponent(placed.orderNumber)}`, {
          state: { order: placed },
        })
        return
      }

      const razorpay = placed.razorpay
      if (!razorpay?.razorpayOrderId) {
        throw new ApiError('PAYMENT_INIT_MISSING', 'Payment could not be started', 400)
      }

      try {
        const verified = await handleOnlinePayment(placed.orderNumber, razorpay)
        await refreshCart()
        navigate(`/order-success/${encodeURIComponent(placed.orderNumber)}`, {
          state: { order: placed, verification: verified },
        })
      } catch (payErr) {
        navigate(`/order-payment-failed/${encodeURIComponent(placed.orderNumber)}`, {
          state: {
            orderNumber: placed.orderNumber,
            message: payErr.message || 'Payment failed',
            paymentStatus: 'FAILED',
          },
        })
      }
    } catch (err) {
      if (err instanceof ApiError && err.code === 'CHECKOUT_REVIEW_REQUIRED') {
        setFormError('Your cart or prices changed. Refreshing preview…')
        idempotencyKeyRef.current = createIdempotencyKey()
        await runPreview()
        await refreshCart()
      } else if (err instanceof ApiError && err.code === 'PAY_LATER_UNAVAILABLE') {
        setFormError('Pay Later is not available yet. Choose another payment method.')
      } else {
        setFormError(err.message || 'Could not place order')
      }
    } finally {
      setSubmitting(false)
    }
  }

  async function handleRetryPayment(orderNumber) {
    setSubmitting(true)
    setFormError('')
    try {
      const init = await initiatePayment({
        orderNumber,
        idempotencyKey: `${idempotencyKeyRef.current}:retry`,
      })
      const verified = await handleOnlinePayment(orderNumber, init)
      await refreshCart()
      navigate(`/order-success/${encodeURIComponent(orderNumber)}`, {
        state: { verification: verified },
      })
    } catch (err) {
      setFormError(err.message || 'Retry failed')
      navigate(`/order-payment-failed/${encodeURIComponent(orderNumber)}`, {
        state: { orderNumber, message: err.message || 'Payment failed' },
      })
    } finally {
      setSubmitting(false)
    }
  }

  // expose retry for failure page via navigate state only — keep page focused
  void handleRetryPayment

  if (cartLoading) {
    return <LoadingState label="Loading checkout…" />
  }

  if (isEmpty) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-10">
        <h1 className="font-display text-3xl text-ma-text">Checkout</h1>
        <p className="mt-3 text-ma-muted">Your cart is empty.</p>
        <Link to="/" className="mt-6 inline-block font-semibold text-ma-primary hover:underline">
          Continue shopping
        </Link>
      </div>
    )
  }

  const pendingRules = preview?.pendingRules || []
  const shippingPending = pendingRules.some((r) => String(r).includes('SHIPPING'))
  const codPending = pendingRules.some((r) => String(r).includes('COD'))
  // V1: GST is not charged (prices are GST-inclusive) — do not show a tax/GST line.
  const payLaterBlocked =
    paymentMethod === 'PAY_LATER' &&
    preview?.issues?.some((i) => i.code === 'PAY_LATER_UNAVAILABLE')

  return (
    <div className="mx-auto max-w-6xl px-4 py-6 md:px-8 md:py-10">
      <nav className="text-sm text-ma-muted">
        <Link to="/" className="hover:text-ma-primary">
          Home
        </Link>
        <span className="mx-2">/</span>
        <Link to="/cart" className="hover:text-ma-primary">
          Cart
        </Link>
        <span className="mx-2">/</span>
        <span className="text-ma-text">Checkout</span>
      </nav>

      <h1 className="mt-4 font-display text-3xl tracking-tight text-ma-text md:text-4xl">Checkout</h1>
      {!isAuthenticated && (
        <p className="mt-2 text-sm text-ma-muted">
          Checking out as guest.{' '}
          <Link to="/login" className="font-semibold text-ma-primary hover:underline">
            Log in
          </Link>{' '}
          for a faster checkout next time.
        </p>
      )}

      <form onSubmit={handlePlaceOrder} className="mt-8 grid gap-8 lg:grid-cols-[1.2fr_0.8fr]">
        <div className="space-y-8">
          <section className="rounded-2xl bg-ma-surface/80 p-5 ring-1 ring-ma-border">
            <h2 className="text-lg font-bold text-ma-text">Contact information</h2>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <Field
                label="Name"
                value={contactName}
                onChange={setContactName}
                error={fieldErrors.contactName}
                autoComplete="name"
              />
              <Field
                label="Mobile number"
                value={contactMobile}
                onChange={setContactMobile}
                error={fieldErrors.contactMobile}
                autoComplete="tel"
                inputMode="numeric"
              />
              <Field
                label="Email (optional)"
                value={contactEmail}
                onChange={setContactEmail}
                error={fieldErrors.contactEmail}
                autoComplete="email"
                className="sm:col-span-2"
              />
            </div>
          </section>

          <section className="rounded-2xl bg-ma-surface/80 p-5 ring-1 ring-ma-border">
            <h2 className="text-lg font-bold text-ma-text">Shipping address</h2>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <Field
                label="Full name"
                value={address.fullName}
                onChange={(v) => updateAddress('fullName', v)}
                error={fieldErrors.fullName}
              />
              <Field
                label="Mobile"
                value={address.mobile}
                onChange={(v) => updateAddress('mobile', v)}
                error={fieldErrors.mobile}
                inputMode="numeric"
              />
              <Field
                label="Address line 1"
                value={address.line1}
                onChange={(v) => updateAddress('line1', v)}
                error={fieldErrors.line1}
                className="sm:col-span-2"
              />
              <Field
                label="Address line 2"
                value={address.line2}
                onChange={(v) => updateAddress('line2', v)}
                className="sm:col-span-2"
              />
              <Field
                label="Landmark"
                value={address.landmark}
                onChange={(v) => updateAddress('landmark', v)}
                className="sm:col-span-2"
              />
              <Field
                label="City"
                value={address.city}
                onChange={(v) => updateAddress('city', v)}
                error={fieldErrors.city}
              />
              <Field
                label="State"
                value={address.state}
                onChange={(v) => updateAddress('state', v)}
                error={fieldErrors.state}
              />
              <Field
                label="PIN code"
                value={address.postalCode}
                onChange={(v) => updateAddress('postalCode', v)}
                error={fieldErrors.postalCode}
                inputMode="numeric"
              />
              <Field
                label="Country"
                value={address.country}
                onChange={(v) => updateAddress('country', v)}
                error={fieldErrors.country}
              />
            </div>
          </section>

          <section className="rounded-2xl bg-ma-surface/80 p-5 ring-1 ring-ma-border">
            <h2 className="text-lg font-bold text-ma-text">Payment method</h2>
            <div className="mt-4 space-y-3">
              {PAYMENT_METHODS.map((method) => (
                <label
                  key={method.value}
                  className="flex cursor-pointer items-center gap-3 rounded-lg px-3 py-2 hover:bg-ma-bg/60"
                >
                  <input
                    type="radio"
                    name="paymentMethod"
                    value={method.value}
                    checked={paymentMethod === method.value}
                    onChange={() => setPaymentMethod(method.value)}
                    className="h-4 w-4 accent-ma-primary"
                  />
                  <span className="text-sm font-medium text-ma-text">{method.label}</span>
                </label>
              ))}
            </div>
            {paymentMethod === 'PAY_LATER' && (
              <p className="mt-3 text-xs text-ma-muted">
                Pay Later is a required checkout option. Availability depends on the merchant payment
                rail configuration.
              </p>
            )}
          </section>
        </div>

        <aside className="h-fit space-y-4 lg:sticky lg:top-6">
          <div className="rounded-2xl bg-gradient-to-b from-ma-surface to-ma-bg p-5 ring-1 ring-ma-border">
            <h2 className="text-lg font-bold text-ma-text">Order summary</h2>
            {previewLoading && <p className="mt-3 text-sm text-ma-muted">Calculating totals…</p>}
            {previewError && (
              <div className="mt-3">
                <ErrorState message={previewError} onRetry={runPreview} />
              </div>
            )}
            {preview?.lines?.length > 0 && (
              <ul className="mt-4 space-y-3 border-b border-ma-border pb-4">
                {preview.lines.map((line) => (
                  <li key={line.productId} className="flex justify-between gap-3 text-sm">
                    <div>
                      <p className="font-medium text-ma-text">{line.title}</p>
                      <p className="text-ma-muted">
                        Qty {line.quantity} × {formatInr(line.unitSellingPrice)}
                      </p>
                    </div>
                    <span className="font-semibold">{formatInr(line.lineSubtotal)}</span>
                  </li>
                ))}
              </ul>
            )}

            <dl className="mt-4 space-y-2 text-sm">
              <Row label="Subtotal" value={formatInr(preview?.itemsSubtotal)} />
              <Row label="Shipping" value={chargeLabel(preview?.shippingCharge, shippingPending)} />
              {paymentMethod === 'COD' && (
                <Row label="COD Fee" value={chargeLabel(preview?.codCharge, codPending)} />
              )}
              {preview?.discountAmount != null && Number(preview.discountAmount) > 0 && (
                <Row label="Discount" value={`−${formatInr(preview.discountAmount)}`} />
              )}
              <div className="flex justify-between border-t border-ma-border pt-3 text-base font-bold">
                <dt>Grand total</dt>
                <dd>
                  {preview?.grandTotal != null ? formatInr(preview.grandTotal) : 'Unavailable'}
                </dd>
              </div>
            </dl>

            {preview?.requiresReview && (
              <p className="mt-3 text-sm text-amber-800" role="alert">
                Some product prices changed. Review the updated summary before placing your order.
              </p>
            )}
            {preview?.issues?.length > 0 && (
              <ul className="mt-3 list-disc space-y-1 pl-5 text-xs text-ma-muted">
                {preview.issues.map((issue, idx) => (
                  <li key={`${issue.code}-${idx}`}>{issue.message}</li>
                ))}
              </ul>
            )}
            {payLaterBlocked && (
              <p className="mt-3 text-sm text-amber-800" role="alert">
                Pay Later is currently unavailable. Choose another method.
              </p>
            )}

            {formError && (
              <p className="mt-4 text-sm text-red-700" role="alert">
                {formError}
              </p>
            )}

            <Button
              type="submit"
              className="mt-5 w-full"
              disabled={
                submitting ||
                previewLoading ||
                !preview?.readyToPlace ||
                preview?.requiresReview ||
                payLaterBlocked
              }
            >
              {submitting ? 'Placing order…' : 'Place order'}
            </Button>
          </div>
        </aside>
      </form>
    </div>
  )
}

function Field({
  label,
  value,
  onChange,
  error,
  className = '',
  autoComplete,
  inputMode,
}) {
  return (
    <label className={`block text-sm ${className}`}>
      <span className="font-medium text-ma-text">{label}</span>
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        autoComplete={autoComplete}
        inputMode={inputMode}
        className="mt-1 w-full rounded-lg border border-ma-border bg-white px-3 py-2.5 text-ma-text outline-none ring-ma-primary focus:ring-2"
      />
      {error && <span className="mt-1 block text-xs text-red-700">{error}</span>}
    </label>
  )
}

function Row({ label, value }) {
  return (
    <div className="flex justify-between gap-3">
      <dt className="text-ma-muted">{label}</dt>
      <dd className="font-medium text-ma-text">{value ?? '—'}</dd>
    </div>
  )
}

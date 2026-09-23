import { apiRequest } from './apiClient'
import { isCustomerAuthenticated } from './customerAuthStorage'

/**
 * Payment foundation for future checkout (Step 23).
 * Never stores or sends Razorpay KEY_SECRET / WEBHOOK_SECRET.
 * Frontend Razorpay Checkout success is NOT authoritative — always call verifyPayment.
 */

/**
 * POST /api/payments/initiate
 * Server determines amount from the order. Do not send amount/customerId/totals.
 */
export async function initiatePayment({ orderNumber, idempotencyKey }) {
  if (!orderNumber) {
    throw new Error('orderNumber is required')
  }
  if (!idempotencyKey) {
    throw new Error('idempotencyKey is required')
  }

  return apiRequest('/api/payments/initiate', {
    method: 'POST',
    auth: isCustomerAuthenticated() ? 'customer' : 'none',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ orderNumber, idempotencyKey }),
  })
}

/**
 * POST /api/payments/verify
 * Sends Razorpay checkout callback fields for server-side signature verification.
 */
export async function verifyPayment({
  orderNumber,
  razorpayOrderId,
  razorpayPaymentId,
  razorpaySignature,
}) {
  if (!orderNumber || !razorpayOrderId || !razorpayPaymentId || !razorpaySignature) {
    throw new Error('orderNumber and Razorpay verification fields are required')
  }

  return apiRequest('/api/payments/verify', {
    method: 'POST',
    auth: isCustomerAuthenticated() ? 'customer' : 'none',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      orderNumber,
      razorpayOrderId,
      razorpayPaymentId,
      razorpaySignature,
    }),
  })
}

/**
 * POST /api/payments/fail — report checkout cancel/failure (does not delete order).
 */
export async function reportPaymentFailure({ orderNumber }) {
  if (!orderNumber) {
    throw new Error('orderNumber is required')
  }
  return apiRequest('/api/payments/fail', {
    method: 'POST',
    auth: isCustomerAuthenticated() ? 'customer' : 'none',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ orderNumber }),
  })
}

/**
 * Opens Razorpay Checkout using server-provided key/order/amount only.
 * Never uses KEY_SECRET. Success handler must still call verifyPayment.
 */
export function openRazorpayCheckout(initiateResponse, handlers = {}) {
  const options = buildRazorpayCheckoutOptions(initiateResponse, handlers)
  if (typeof window === 'undefined' || typeof window.Razorpay !== 'function') {
    return Promise.reject(new Error('Razorpay Checkout script is not loaded'))
  }
  const rzp = new window.Razorpay({
    ...options,
    handler: async (response) => {
      if (handlers.onSuccess) {
        await handlers.onSuccess(response)
      }
    },
  })
  if (handlers.onFailure) {
    rzp.on('payment.failed', handlers.onFailure)
  }
  rzp.open()
  return rzp
}

/**
 * Helper shape for Razorpay Checkout options.
 * amount is in paise from the backend initiate / place-order response.
 */
export function buildRazorpayCheckoutOptions(initiateResponse, handlers = {}) {
  if (!initiateResponse?.keyId || !initiateResponse?.razorpayOrderId) {
    throw new Error('Invalid initiate response')
  }
  return {
    key: initiateResponse.keyId,
    amount: initiateResponse.amount,
    currency: initiateResponse.currency || 'INR',
    order_id: initiateResponse.razorpayOrderId,
    handler: handlers.onSuccess,
    modal: {
      ondismiss: handlers.onDismiss,
    },
  }
}

/**
 * Loads the Razorpay Checkout script once (public key only; no secrets).
 */
export function loadRazorpayScript() {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('window is not available'))
  }
  if (typeof window.Razorpay === 'function') {
    return Promise.resolve()
  }
  return new Promise((resolve, reject) => {
    const existing = document.getElementById('razorpay-checkout-js')
    if (existing) {
      existing.addEventListener('load', () => resolve())
      existing.addEventListener('error', () => reject(new Error('Failed to load Razorpay')))
      return
    }
    const script = document.createElement('script')
    script.id = 'razorpay-checkout-js'
    script.src = 'https://checkout.razorpay.com/v1/checkout.js'
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('Failed to load Razorpay'))
    document.body.appendChild(script)
  })
}

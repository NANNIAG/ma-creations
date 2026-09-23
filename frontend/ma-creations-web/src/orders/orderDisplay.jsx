export function formatInr(value) {
  const number = Number(value)
  if (Number.isNaN(number)) {
    return String(value ?? '—')
  }
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(number)
}

export function formatOrderDate(value) {
  if (!value) return '—'
  try {
    return new Date(value).toLocaleString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return '—'
  }
}

export function statusLabel(status) {
  if (!status) return '—'
  return String(status).replaceAll('_', ' ')
}

export function OrderStatusBadge({ status }) {
  const tone = orderStatusTone(status)
  return (
    <span className={`inline-flex rounded-md px-2 py-0.5 text-xs font-medium ring-1 ${tone}`}>
      {statusLabel(status)}
    </span>
  )
}

export function PaymentStatusBadge({ status }) {
  const tone = paymentStatusTone(status)
  return (
    <span className={`inline-flex rounded-md px-2 py-0.5 text-xs font-medium ring-1 ${tone}`}>
      {statusLabel(status)}
    </span>
  )
}

function orderStatusTone(status) {
  switch (status) {
    case 'DELIVERED':
      return 'bg-green-50 text-green-800 ring-green-200'
    case 'SHIPPED':
    case 'PROCESSING':
      return 'bg-sky-50 text-sky-900 ring-sky-200'
    case 'PLACED':
      return 'bg-ma-surface text-ma-text ring-ma-border'
    case 'PAYMENT_FAILED':
    case 'CANCELLED':
      return 'bg-red-50 text-red-800 ring-red-200'
    case 'PENDING_PAYMENT':
    default:
      return 'bg-amber-50 text-amber-900 ring-amber-200'
  }
}

function paymentStatusTone(status) {
  switch (status) {
    case 'PAID':
      return 'bg-green-50 text-green-800 ring-green-200'
    case 'FAILED':
      return 'bg-red-50 text-red-800 ring-red-200'
    case 'COD_PENDING':
    case 'PENDING':
    default:
      return 'bg-amber-50 text-amber-900 ring-amber-200'
  }
}

/** Allowed next fulfillment statuses for admin UI (mirrors backend validator). */
export function nextAllowedOrderStatuses(current) {
  switch (current) {
    case 'PLACED':
      return ['PROCESSING']
    case 'PROCESSING':
      return ['SHIPPED']
    case 'SHIPPED':
      return ['DELIVERED']
    default:
      return []
  }
}

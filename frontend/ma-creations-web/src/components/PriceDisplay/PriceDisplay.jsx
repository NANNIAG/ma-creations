function formatInr(value) {
  if (value == null || value === '') {
    return null
  }
  const number = Number(value)
  if (Number.isNaN(number)) {
    return String(value)
  }
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(number)
}

export default function PriceDisplay({ sellingPrice, mrp, size = 'md' }) {
  const sellingClass = size === 'lg' ? 'text-xl md:text-2xl' : 'text-base'
  const mrpClass = size === 'lg' ? 'text-sm md:text-base' : 'text-sm'

  return (
    <div className="flex flex-wrap items-baseline gap-2">
      <span className={`font-bold text-ma-text ${sellingClass}`}>{formatInr(sellingPrice)}</span>
      {mrp != null && Number(mrp) > Number(sellingPrice) && (
        <span className={`text-ma-muted line-through ${mrpClass}`}>{formatInr(mrp)}</span>
      )}
    </div>
  )
}

export default function DiscountBadge({ percent }) {
  if (percent == null || percent <= 0) {
    return null
  }

  return (
    <span className="text-sm font-semibold text-ma-discount">{percent}% off</span>
  )
}

export default function RatingDisplay({ averageRating, ratingCount, size = 'sm' }) {
  if (averageRating == null) {
    return null
  }

  const rating = Number(averageRating)
  const pill =
    rating >= 4
      ? 'bg-ma-discount text-white'
      : rating >= 3
        ? 'bg-amber-600 text-white'
        : 'bg-ma-muted text-white'

  return (
    <div className={`flex items-center gap-2 ${size === 'lg' ? 'text-base' : 'text-sm'}`}>
      <span className={`inline-flex items-center rounded px-1.5 py-0.5 font-semibold ${pill}`}>
        {rating.toFixed(1)} ★
      </span>
      {ratingCount != null && (
        <span className="text-ma-muted">({ratingCount} ratings)</span>
      )}
    </div>
  )
}

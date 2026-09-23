import { useState } from 'react'
import { Link } from 'react-router-dom'
import { resolveCategoryImageUrl } from '../../config/brand'

const tileColors = [
  'bg-[#efe2d2]',
  'bg-[#e8d5c4]',
  'bg-[#f0e6da]',
  'bg-[#e5d8cb]',
  'bg-[#eddccf]',
]

/**
 * Category tile. Optional local image from public/categories/{slug}.jpg
 * (or VITE_CATEGORY_IMAGE_MAP_JSON). Falls back to color circle — no invented photos.
 */
export default function CategoryCard({ category, index = 0 }) {
  const [imageFailed, setImageFailed] = useState(false)

  if (!category) {
    return null
  }

  const color = tileColors[index % tileColors.length]
  const imageUrl = resolveCategoryImageUrl(category)
  const showImage = Boolean(imageUrl) && !imageFailed

  return (
    <Link
      to={`/categories/${category.id}`}
      aria-label={category.name}
      className="group flex w-[4.75rem] shrink-0 flex-col items-center gap-2 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ma-primary sm:w-24 md:w-28"
    >
      <div
        className={`relative flex h-[4.75rem] w-[4.75rem] items-center justify-center overflow-hidden rounded-full border-2 border-transparent text-center text-xs font-semibold text-ma-text transition group-hover:border-ma-primary sm:h-24 sm:w-24 md:h-28 md:w-28 ${
          showImage ? 'bg-ma-border/30' : color
        }`}
      >
        {showImage ? (
          <img
            src={imageUrl}
            alt=""
            className="h-full w-full object-cover"
            loading="lazy"
            onError={() => setImageFailed(true)}
          />
        ) : (
          <span className="px-2 leading-tight">{category.name.split('&')[0].trim()}</span>
        )}
      </div>
      <span className="line-clamp-2 text-center text-xs font-medium text-ma-text md:text-sm">
        {category.name}
      </span>
    </Link>
  )
}

import { Link } from 'react-router-dom'
import { brandLogoUrl } from '../../config/brand'

/**
 * Text wordmark by default. Drop a file in public/branding/ and set VITE_BRAND_LOGO_URL.
 */
export default function BrandMark({ to = '/', className = '' }) {
  return (
    <Link
      to={to}
      className={`inline-flex items-center gap-2 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ma-primary ${className}`}
      aria-label="MA CREATIONS home"
    >
      {brandLogoUrl ? (
        <img
          src={brandLogoUrl}
          alt="MA CREATIONS"
          className="h-10 w-auto max-w-[11rem] object-contain object-left md:h-12 md:max-w-[14rem]"
        />
      ) : (
        <span className="text-base font-bold tracking-wide text-ma-text md:text-lg">MA CREATIONS</span>
      )}
    </Link>
  )
}

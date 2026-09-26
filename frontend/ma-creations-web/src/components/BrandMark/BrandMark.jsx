import { useState } from 'react'
import { Link } from 'react-router-dom'
import { brandLogoUrl } from '../../config/brand'

/**
 * Tries public logo (/logo/logo.png by default). Text wordmark if the file is missing.
 */
export default function BrandMark({ to = '/', className = '' }) {
  const [logoReady, setLogoReady] = useState(false)
  const [logoFailed, setLogoFailed] = useState(false)
  const showText = !logoReady || logoFailed

  return (
    <Link
      to={to}
      className={`inline-flex items-center gap-2 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ma-primary ${className}`}
      aria-label="MA CREATIONS home"
    >
      {!logoFailed && brandLogoUrl ? (
        <img
          src={brandLogoUrl}
          alt="MA CREATIONS"
          className={`h-10 w-auto max-w-[11rem] object-contain object-left md:h-12 md:max-w-[14rem] ${
            logoReady ? '' : 'absolute h-0 w-0 opacity-0'
          }`}
          onLoad={() => setLogoReady(true)}
          onError={() => setLogoFailed(true)}
        />
      ) : null}
      {showText ? (
        <span className="text-base font-bold tracking-wide text-ma-text md:text-lg">MA CREATIONS</span>
      ) : null}
    </Link>
  )
}

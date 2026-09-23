import { Link } from 'react-router-dom'
import { isInstagramConfigured, resolveInstagramFollowUrl, isWhatsAppConfigured, buildWhatsAppUrl } from '../../config/brand'

export default function Footer() {
  const instagramHref = resolveInstagramFollowUrl()
  const whatsappHref = isWhatsAppConfigured()
    ? buildWhatsAppUrl('Hi MA CREATIONS, I have a question.')
    : null

  return (
    <footer className="mt-12 border-t border-ma-border bg-ma-surface">
      <div className="mx-auto grid max-w-6xl gap-8 px-4 py-10 md:grid-cols-3 md:px-8">
        <div>
          <p className="font-bold text-ma-text">MA CREATIONS</p>
          <p className="mt-2 text-sm text-ma-muted">
            Home & kitchen products with a clean boutique shopping experience.
          </p>
        </div>
        <div>
          <p className="font-semibold text-ma-text">Quick links</p>
          <ul className="mt-2 space-y-1 text-sm text-ma-muted">
            <li>
              <Link to="/" className="hover:text-ma-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-ma-primary">
                Home
              </Link>
            </li>
            <li>
              <Link to="/track-order" className="hover:text-ma-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-ma-primary">
                Track order
              </Link>
            </li>
            <li className="text-ma-muted/80" title="CLIENT CONFIRMATION REQUIRED">
              Return policy (content TBD)
            </li>
            {instagramHref && (
              <li>
                <a
                  href={instagramHref}
                  target="_blank"
                  rel="noreferrer"
                  className="hover:text-ma-primary"
                >
                  Instagram
                </a>
              </li>
            )}
            {whatsappHref && (
              <li>
                <a href={whatsappHref} target="_blank" rel="noreferrer" className="hover:text-ma-primary">
                  WhatsApp
                </a>
              </li>
            )}
          </ul>
        </div>
        <div>
          <p className="font-semibold text-ma-text">Payment trust</p>
          <p className="mt-2 text-sm text-ma-muted">
            Payment badges TBD — client confirmation required. Display only for now.
          </p>
          {!isInstagramConfigured() && !isWhatsAppConfigured() && (
            <p className="mt-3 text-xs text-ma-muted">
              Configure social links via <code className="text-[11px]">VITE_WHATSAPP_NUMBER</code> /{' '}
              <code className="text-[11px]">VITE_INSTAGRAM_HANDLE</code>.
            </p>
          )}
        </div>
      </div>
    </footer>
  )
}

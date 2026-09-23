import { buildWhatsAppUrl, isWhatsAppConfigured } from '../../config/brand'

/**
 * Floating WhatsApp entry. Number from VITE_WHATSAPP_NUMBER only — never invent a production number.
 * Raised above mobile PDP sticky CTA (`bottom-20` on small screens).
 */
export default function WhatsAppWidget() {
  if (!isWhatsAppConfigured()) {
    return (
      <div
        className="fixed bottom-20 right-3 z-50 max-w-[12rem] rounded-xl bg-ma-surface px-3 py-2 text-xs text-ma-muted shadow-sm ring-1 ring-ma-border md:bottom-5 md:right-4"
        role="status"
        title="Set VITE_WHATSAPP_NUMBER in .env.local (digits with country code)"
      >
        WhatsApp not configured — set <span className="font-medium">VITE_WHATSAPP_NUMBER</span>
      </div>
    )
  }

  const href = buildWhatsAppUrl('Hi MA CREATIONS, I have a question.')

  return (
    <a
      href={href}
      target="_blank"
      rel="noreferrer"
      aria-label="Chat on WhatsApp"
      className="fixed bottom-20 right-3 z-50 flex h-14 w-14 items-center justify-center rounded-full bg-[#25D366] text-white shadow-md transition hover:brightness-95 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ma-primary md:bottom-5 md:right-4"
    >
      <WhatsAppIcon />
    </a>
  )
}

function WhatsAppIcon() {
  return (
    <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M20 11.5A8.5 8.5 0 0 1 7.1 18.7L4 20l1.4-3A8.5 8.5 0 1 1 20 11.5Zm-8.5 6.7c1.3 0 2.5-.3 3.6-.9l.3-.2 2.1.6-.6-2 .2-.3a6.5 6.5 0 1 0-5.6 2.8Zm3.5-4.6c-.2-.1-1.1-.5-1.2-.6-.2-.1-.3-.1-.5.1-.1.2-.5.6-.6.7-.1.1-.2.2-.4.1-.2-.1-.8-.3-1.5-1-.6-.5-1-1.2-1.1-1.4-.1-.2 0-.3.1-.4l.3-.4c.1-.1.1-.2.2-.3 0-.1 0-.2 0-.3 0-.1-.5-1.2-.7-1.6-.2-.4-.3-.4-.5-.4h-.4c-.1 0-.3.1-.5.3-.2.2-.6.6-.6 1.4 0 .8.6 1.6.7 1.7.1.2 1.2 1.9 3 2.6 1.8.7 1.8.5 2.1.5.3 0 1-.4 1.1-.8.1-.4.1-.7.1-.8 0-.1-.1-.1-.2-.2Z" />
    </svg>
  )
}

import { isInstagramConfigured, resolveInstagramFollowUrl, instagramHandle } from '../../config/brand'
import Button from '../Button/Button'

/**
 * Social-proof section. Live Instagram grid requires client handle + embed choice.
 * Placeholders are decorative only — never presented as real posts.
 */
const PLACEHOLDER_COUNT = 6

export default function InstagramSection() {
  const followHref = resolveInstagramFollowUrl()
  const configured = isInstagramConfigured()

  return (
    <section className="mx-auto max-w-6xl px-4 py-10 md:px-8" aria-labelledby="instagram-heading">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 id="instagram-heading" className="text-xl font-bold text-ma-text md:text-2xl">
            Follow along
          </h2>
          <p className="mt-1 text-sm text-ma-muted">
            {configured
              ? `Follow ${instagramHandle || 'MA CREATIONS'} on Instagram. Live feed embed TBD.`
              : 'Instagram handle not configured — set VITE_INSTAGRAM_HANDLE or VITE_INSTAGRAM_FOLLOW_URL.'}
          </p>
        </div>
        {followHref ? (
          <a href={followHref} target="_blank" rel="noreferrer">
            <Button variant="secondary">Follow on Instagram</Button>
          </a>
        ) : (
          <Button
            variant="secondary"
            disabled
            title="Set VITE_INSTAGRAM_HANDLE or VITE_INSTAGRAM_FOLLOW_URL in .env.local"
          >
            Follow on Instagram
          </Button>
        )}
      </div>

      <div
        className="mt-6 grid grid-cols-2 gap-3 md:grid-cols-3 lg:grid-cols-6"
        aria-hidden={!configured}
      >
        {Array.from({ length: PLACEHOLDER_COUNT }, (_, i) => (
          <div
            key={i}
            className="aspect-square rounded-lg bg-gradient-to-br from-ma-border to-ma-primary/25 ring-1 ring-ma-border/60"
            title="Placeholder — not a live Instagram post"
          />
        ))}
      </div>
      <p className="mt-3 text-xs text-ma-muted">
        Grid tiles are placeholders only (not live Instagram content).
      </p>
    </section>
  )
}

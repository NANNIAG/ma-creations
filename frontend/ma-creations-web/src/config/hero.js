/**
 * Hero banner configuration.
 * Place client image files under: public/heroes/
 * Default paths: /heroes/hero-1.png … hero-3.png
 * Documented in public/heroes/README.md
 *
 * Optional JSON override via VITE_HERO_SLIDES_JSON, e.g.:
 * [{"id":"1","title":"","image":"/heroes/hero-1.png","fullBleed":true}]
 * fullBleed: designed banners (no duplicate headline overlay; image is clickable Shop Now)
 * Optional per-slide href (e.g. "/categories/1") overrides VITE_HERO_SHOP_NOW_TARGET
 *
 * Shop Now target (CLIENT CONFIRMATION REQUIRED for final destination):
 * VITE_HERO_SHOP_NOW_TARGET=#categories | /categories/1 | /
 *
 * Missing image files: HeroCarousel falls back to gradient placeholders.
 */

/** Default carousel slides — used when VITE_HERO_SLIDES_JSON is unset. */
export const DEFAULT_HERO_SLIDES = [
  {
    id: '1',
    title: 'MA CREATIONS',
    subtitle: 'Boutique home & kitchen essentials.',
    image: '/heroes/hero-1.png',
    tone: 'from-[#d4a373]/85 to-[#2c2c2c]/55',
  },
  {
    id: '2',
    title: 'Shop by category',
    subtitle: 'Hydration, lunch, gadgets, storage, and home décor — from the live catalog.',
    image: '/heroes/hero-2.png',
    tone: 'from-[#c48b5a]/80 to-[#2c2c2c]/50',
  },
  {
    id: '3',
    title: 'Thoughtful gifts',
    subtitle: 'Curated pieces for everyday living and celebrations.',
    image: '/heroes/hero-3.png',
    tone: 'from-[#b9895f]/75 to-[#2c2c2c]/55',
  },
]

export function getShopNowTarget() {
  const raw = import.meta.env.VITE_HERO_SHOP_NOW_TARGET?.trim()
  return raw || '#categories'
}

export function getHeroSlides() {
  const raw = import.meta.env.VITE_HERO_SLIDES_JSON?.trim()
  if (!raw) {
    return DEFAULT_HERO_SLIDES
  }
  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed) || parsed.length === 0) {
      return DEFAULT_HERO_SLIDES
    }
    return parsed.map((slide, index) => ({
      id: String(slide.id ?? index + 1),
      title: Object.prototype.hasOwnProperty.call(slide, 'title')
        ? String(slide.title || '')
        : 'MA CREATIONS',
      subtitle: slide.subtitle || '',
      image: slide.image || DEFAULT_HERO_SLIDES[index % DEFAULT_HERO_SLIDES.length].image || null,
      tone: slide.tone || DEFAULT_HERO_SLIDES[index % DEFAULT_HERO_SLIDES.length].tone,
      fullBleed: Boolean(slide.fullBleed),
      href: typeof slide.href === 'string' ? slide.href.trim() : '',
      ariaLabel: slide.ariaLabel || slide.title || 'Shop Now',
    }))
  } catch {
    return DEFAULT_HERO_SLIDES
  }
}

export function isHashTarget(target) {
  return Boolean(target && target.startsWith('#'))
}

export function isPathTarget(target) {
  return Boolean(target && target.startsWith('/') && !target.startsWith('//'))
}

/**
 * Frontend public runtime config (safe for the browser).
 * Inter vs Montserrat is CLIENT CONFIRMATION REQUIRED — temporary stack only.
 * Never put JWT/DB secrets in VITE_* variables.
 */

export const brandFontFamily =
  import.meta.env.VITE_BRAND_FONT_FAMILY?.trim() ||
  '"Inter", "Montserrat", system-ui, sans-serif'

/** Optional logo URL, e.g. /branding/logo.svg — leave empty for text wordmark. */
export const brandLogoUrl = import.meta.env.VITE_BRAND_LOGO_URL?.trim() || ''

export const whatsappNumber = import.meta.env.VITE_WHATSAPP_NUMBER?.trim() || ''

export const instagramHandle = import.meta.env.VITE_INSTAGRAM_HANDLE?.trim() || ''

export const instagramFollowUrl = import.meta.env.VITE_INSTAGRAM_FOLLOW_URL?.trim() || ''

/** Digits only; requires country code (e.g. 91…). */
export function normalizeWhatsAppDigits(raw = whatsappNumber) {
  return String(raw || '').replace(/[^\d]/g, '')
}

export function isWhatsAppConfigured(raw = whatsappNumber) {
  return normalizeWhatsAppDigits(raw).length >= 10
}

export function buildWhatsAppUrl(message = '', raw = whatsappNumber) {
  if (!isWhatsAppConfigured(raw)) {
    return null
  }
  const digits = normalizeWhatsAppDigits(raw)
  const text = message ? `?text=${encodeURIComponent(message)}` : ''
  return `https://wa.me/${digits}${text}`
}

export function resolveInstagramFollowUrl(
  handle = instagramHandle,
  followUrl = instagramFollowUrl,
) {
  if (followUrl) {
    return followUrl
  }
  if (!handle) {
    return null
  }
  const cleaned = handle.replace(/^@/, '')
  return `https://instagram.com/${cleaned}`
}

export function isInstagramConfigured() {
  return Boolean(resolveInstagramFollowUrl())
}

/**
 * Optional map: { "hydration-drinkware": "/categories/hydration-drinkware.jpg", ... }
 * or by numeric id string: { "1": "/categories/1.jpg" }
 */
export function getCategoryImageMap() {
  const raw = import.meta.env.VITE_CATEGORY_IMAGE_MAP_JSON?.trim()
  if (!raw) {
    return {}
  }
  try {
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

/**
 * Resolve a local static category image without a new media API.
 * Prefer VITE_CATEGORY_IMAGE_MAP_JSON entries. If unset, optional auto path
 * `/categories/{slug}.jpg` when VITE_CATEGORY_IMAGES_AUTO=true.
 */
export function resolveCategoryImageUrl(category) {
  if (!category) {
    return null
  }
  const map = getCategoryImageMap()
  if (map[String(category.id)]) {
    return map[String(category.id)]
  }
  if (category.slug && map[category.slug]) {
    return map[category.slug]
  }
  const auto =
    String(import.meta.env.VITE_CATEGORY_IMAGES_AUTO || '')
      .trim()
      .toLowerCase() === 'true'
  if (auto && category.slug) {
    return `/categories/${category.slug}.jpg`
  }
  return null
}

/**
 * Optional full-bleed category PLP banners (designed category creatives).
 * Map: { "hydration-drinkware": "/heroes/hydration-drinkware.jpg", ... }
 */
export function getCategoryBannerMap() {
  const raw = import.meta.env.VITE_CATEGORY_BANNER_MAP_JSON?.trim()
  if (!raw) {
    return {}
  }
  try {
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

export function resolveCategoryBannerUrl(category) {
  if (!category) {
    return null
  }
  const map = getCategoryBannerMap()
  if (map[String(category.id)]) {
    return map[String(category.id)]
  }
  if (category.slug && map[category.slug]) {
    return map[category.slug]
  }
  return null
}

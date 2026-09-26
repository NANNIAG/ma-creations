/**
 * Frontend public runtime config (safe for the browser).
 * Inter vs Montserrat is CLIENT CONFIRMATION REQUIRED — temporary stack only.
 * Never put JWT/DB secrets in VITE_* variables.
 */

export const brandFontFamily =
  import.meta.env.VITE_BRAND_FONT_FAMILY?.trim() ||
  '"Inter", "Montserrat", system-ui, sans-serif'

/**
 * Logo path under public/logo/. Override with VITE_BRAND_LOGO_URL if needed.
 * BrandMark falls back to the text wordmark when the file is missing.
 */
export const DEFAULT_BRAND_LOGO_URL = '/logo/logo.png'

export const brandLogoUrl =
  import.meta.env.VITE_BRAND_LOGO_URL?.trim() || DEFAULT_BRAND_LOGO_URL

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
 * Default category tile images under public/categories/.
 * Keys: numeric id (seed order) and DB slug. CategoryCard falls back if missing.
 */
export const DEFAULT_CATEGORY_IMAGE_MAP = {
  1: '/categories/hydration.png',
  'hydration-drinkware': '/categories/hydration.png',
  2: '/categories/lunch.png',
  'lunch-meal-prep': '/categories/lunch.png',
  3: '/categories/kitchen-gadgets.png',
  'kitchen-gadgets-prep': '/categories/kitchen-gadgets.png',
  4: '/categories/storage.png',
  'storage-kitchenware': '/categories/storage.png',
  5: '/categories/home-decor.png',
  'home-decor-festivity': '/categories/home-decor.png',
}

/**
 * Optional map override: { "hydration-drinkware": "/categories/hydration.png", ... }
 * or by numeric id string: { "1": "/categories/hydration.png" }
 */
export function getCategoryImageMap() {
  const raw = import.meta.env.VITE_CATEGORY_IMAGE_MAP_JSON?.trim()
  if (!raw) {
    return { ...DEFAULT_CATEGORY_IMAGE_MAP }
  }
  try {
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') {
      return { ...DEFAULT_CATEGORY_IMAGE_MAP }
    }
    return { ...DEFAULT_CATEGORY_IMAGE_MAP, ...parsed }
  } catch {
    return { ...DEFAULT_CATEGORY_IMAGE_MAP }
  }
}

/**
 * Resolve a local static category image without a new media API.
 * Prefer VITE_CATEGORY_IMAGE_MAP_JSON entries, then defaults above.
 * Optional auto path `/categories/{slug}.jpg` when VITE_CATEGORY_IMAGES_AUTO=true.
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

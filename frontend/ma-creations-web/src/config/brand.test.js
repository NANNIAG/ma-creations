import { describe, expect, it } from 'vitest'
import {
  buildWhatsAppUrl,
  isWhatsAppConfigured,
  normalizeWhatsAppDigits,
  resolveCategoryImageUrl,
  resolveInstagramFollowUrl,
} from './brand'
import { DEFAULT_HERO_SLIDES, getShopNowTarget, isHashTarget, isPathTarget } from './hero'

describe('brand config helpers', () => {
  it('normalizes WhatsApp digits and validates length', () => {
    expect(normalizeWhatsAppDigits('+91 98-0000-0000')).toBe('919800000000')
    expect(isWhatsAppConfigured('123')).toBe(false)
    expect(isWhatsAppConfigured('919800000000')).toBe(true)
    expect(buildWhatsAppUrl('Hello', '919800000000')).toContain('wa.me/919800000000')
    expect(buildWhatsAppUrl('Hello', '')).toBeNull()
  })

  it('resolves Instagram follow URL from handle or explicit URL', () => {
    expect(resolveInstagramFollowUrl('@demo', '')).toBe('https://instagram.com/demo')
    expect(resolveInstagramFollowUrl('', 'https://instagram.com/explicit')).toBe(
      'https://instagram.com/explicit',
    )
    expect(resolveInstagramFollowUrl('', '')).toBeNull()
  })

  it('resolves default category tile paths by id and slug', () => {
    expect(
      resolveCategoryImageUrl({ id: 1, name: 'Hydration & Drinkware', slug: 'hydration-drinkware' }),
    ).toBe('/categories/hydration.png')
    expect(
      resolveCategoryImageUrl({ id: 3, name: 'Kitchen Gadgets & Prep', slug: 'kitchen-gadgets-prep' }),
    ).toBe('/categories/kitchen-gadgets.png')
    expect(resolveCategoryImageUrl({ id: 99, name: 'Unknown' })).toBeNull()
  })
})

describe('hero config helpers', () => {
  it('classifies Shop Now targets', () => {
    expect(isHashTarget('#categories')).toBe(true)
    expect(isPathTarget('/categories/1')).toBe(true)
    expect(isPathTarget('https://evil.com')).toBe(false)
    expect(getShopNowTarget()).toBeTruthy()
  })

  it('defaults to hero-1..hero-3 image paths', () => {
    expect(DEFAULT_HERO_SLIDES).toHaveLength(3)
    expect(DEFAULT_HERO_SLIDES.map((s) => s.image)).toEqual([
      '/heroes/hero-1.png',
      '/heroes/hero-2.png',
      '/heroes/hero-3.png',
    ])
  })
})

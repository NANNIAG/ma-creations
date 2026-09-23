import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, resolveMediaUrl } from './apiClient'
import { fetchCategories } from './categoryService'
import { fetchProductById, fetchProducts } from './productService'
import { CART_TOKEN_KEY } from './cartStorage'
import * as cartService from './cartService'

vi.stubGlobal('fetch', vi.fn())

describe('categoryService', () => {
  beforeEach(() => {
    fetch.mockReset()
  })

  it('fetches categories from /api/categories', async () => {
    fetch.mockResolvedValue({
      ok: true,
      json: async () => ({ data: [{ id: 1, name: 'Hydration & Drinkware' }] }),
    })

    const data = await fetchCategories()
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/categories'),
      expect.any(Object),
    )
    expect(data[0].name).toBe('Hydration & Drinkware')
  })
})

describe('productService', () => {
  beforeEach(() => {
    fetch.mockReset()
  })

  it('fetches products with category and sort query params', async () => {
    fetch.mockResolvedValue({
      ok: true,
      json: async () => ({
        data: [
          {
            id: 1,
            title: 'Bottle',
            primaryImageUrl: '/api/media/a.jpg',
          },
        ],
      }),
    })

    const data = await fetchProducts({ categoryId: 2, sort: 'price_asc' })
    expect(fetch).toHaveBeenCalledWith(
      expect.stringMatching(/\/api\/products\?categoryId=2&sort=price_asc/),
      expect.any(Object),
    )
    expect(data[0].primaryImageUrl).toContain('/api/media/a.jpg')
  })

  it('fetches products with search query param', async () => {
    fetch.mockResolvedValue({
      ok: true,
      json: async () => ({ data: [] }),
    })

    await fetchProducts({ search: 'bottle', sort: 'newest' })
    expect(fetch).toHaveBeenCalledWith(
      expect.stringMatching(/\/api\/products\?sort=newest&search=bottle/),
      expect.any(Object),
    )
  })

  it('omits blank search from query string', async () => {
    fetch.mockResolvedValue({
      ok: true,
      json: async () => ({ data: [] }),
    })

    await fetchProducts({ search: '   ', sort: 'newest' })
    expect(fetch).toHaveBeenCalledWith(
      expect.stringMatching(/\/api\/products\?sort=newest$/),
      expect.any(Object),
    )
  })

  it('fetches product detail and maps image urls', async () => {
    fetch.mockResolvedValue({
      ok: true,
      json: async () => ({
        data: {
          id: 9,
          title: 'Bottle',
          images: [{ id: 1, url: '/api/media/x.jpg', sortOrder: 0 }],
        },
      }),
    })

    const data = await fetchProductById(9)
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/products/9'),
      expect.any(Object),
    )
    expect(data.images[0].url).toContain('/api/media/x.jpg')
  })

  it('throws ApiError on failure', async () => {
    fetch.mockResolvedValue({
      ok: false,
      status: 404,
      json: async () => ({
        error: { code: 'PRODUCT_NOT_FOUND', message: 'Product not found: 404' },
      }),
    })

    await expect(fetchProductById(404)).rejects.toBeInstanceOf(ApiError)
  })
})

describe('cartService', () => {
  beforeEach(() => {
    fetch.mockReset()
    localStorage.clear()
  })

  it('persists guest token from addItem response and sends X-Cart-Token', async () => {
    fetch.mockResolvedValue({
      ok: true,
      json: async () => ({
        data: {
          id: 1,
          guestToken: 'abc-123',
          itemCount: 1,
          subtotal: 10,
          currency: 'INR',
          items: [],
        },
      }),
    })

    await cartService.addItem(9, 1)

    expect(localStorage.getItem(CART_TOKEN_KEY)).toBe('abc-123')
    const [, options] = fetch.mock.calls[0]
    expect(options.headers['X-Cart-Token']).toBeUndefined()
    expect(options.headers.Authorization).toBeUndefined()

    fetch.mockResolvedValue({
      ok: true,
      json: async () => ({
        data: {
          id: 1,
          guestToken: 'abc-123',
          itemCount: 1,
          subtotal: 10,
          currency: 'INR',
          items: [],
        },
      }),
    })

    await cartService.getCart()
    const [, getOptions] = fetch.mock.calls[1]
    expect(getOptions.headers['X-Cart-Token']).toBe('abc-123')
    expect(getOptions.headers.Authorization).toBeUndefined()
  })

  it('does not call API on getCart when no token exists', async () => {
    const cart = await cartService.getCart()
    expect(cart.itemCount).toBe(0)
    expect(fetch).not.toHaveBeenCalled()
  })
})

describe('wishlistService', () => {
  beforeEach(() => {
    fetch.mockReset()
    localStorage.clear()
  })

  it('persists guest token from addToWishlist and sends X-Wishlist-Token', async () => {
    const { addToWishlist, getWishlist } = await import('./wishlistService')
    const { WISHLIST_TOKEN_KEY } = await import('./wishlistStorage')

    fetch.mockResolvedValue({
      ok: true,
      json: async () => ({
        data: {
          id: 1,
          guestToken: 'wish-abc',
          itemCount: 1,
          items: [],
        },
      }),
    })

    await addToWishlist(9)

    expect(localStorage.getItem(WISHLIST_TOKEN_KEY)).toBe('wish-abc')
    const [, options] = fetch.mock.calls[0]
    expect(options.headers.Authorization).toBeUndefined()

    fetch.mockResolvedValue({
      ok: true,
      json: async () => ({
        data: {
          id: 1,
          guestToken: 'wish-abc',
          itemCount: 1,
          items: [],
        },
      }),
    })

    await getWishlist()
    const [, getOptions] = fetch.mock.calls[1]
    expect(getOptions.headers['X-Wishlist-Token']).toBe('wish-abc')
  })

  it('does not call API on getWishlist when no token exists', async () => {
    const { getWishlist } = await import('./wishlistService')
    const wishlist = await getWishlist()
    expect(wishlist.itemCount).toBe(0)
    expect(fetch).not.toHaveBeenCalled()
  })
})

describe('resolveMediaUrl', () => {
  it('prefixes relative api media paths', () => {
    expect(resolveMediaUrl('/api/media/file.jpg')).toMatch(/\/api\/media\/file\.jpg$/)
  })

  it('keeps absolute urls', () => {
    expect(resolveMediaUrl('https://cdn.example/x.jpg')).toBe('https://cdn.example/x.jpg')
  })
})

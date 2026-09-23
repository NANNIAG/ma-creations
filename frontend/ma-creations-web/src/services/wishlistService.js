import { apiRequest, resolveMediaUrl } from './apiClient'
import { isCustomerAuthenticated } from './customerAuthStorage'
import { clearWishlistToken, getWishlistToken, setWishlistToken } from './wishlistStorage'

const WISHLIST_TOKEN_HEADER = 'X-Wishlist-Token'

function emptyWishlist() {
  return {
    id: null,
    guestToken: null,
    itemCount: 0,
    items: [],
  }
}

function mapWishlist(wishlist) {
  if (!wishlist) {
    return emptyWishlist()
  }
  return {
    ...wishlist,
    items: (wishlist.items || []).map((item) => ({
      ...item,
      imageUrl: resolveMediaUrl(item.imageUrl),
      sellingPrice: item.unitPrice,
      primaryImageUrl: resolveMediaUrl(item.imageUrl),
    })),
  }
}

function persistGuestToken(wishlist) {
  if (!isCustomerAuthenticated() && wishlist?.guestToken) {
    setWishlistToken(wishlist.guestToken)
  }
  return mapWishlist(wishlist)
}

function wishlistHeaders({ includeGuestToken = true } = {}) {
  const headers = {
    'Content-Type': 'application/json',
  }
  if (includeGuestToken) {
    const token = getWishlistToken()
    if (token) {
      headers[WISHLIST_TOKEN_HEADER] = token
    }
  }
  return headers
}

async function wishlistRequest(path, options = {}) {
  const authenticated = isCustomerAuthenticated()
  const includeGuestToken = options.includeGuestToken ?? !authenticated
  const data = await apiRequest(path, {
    ...options,
    auth: authenticated ? 'customer' : 'none',
    headers: {
      ...wishlistHeaders({ includeGuestToken }),
      ...(options.headers || {}),
    },
  })
  return persistGuestToken(data)
}

export async function getWishlist() {
  if (isCustomerAuthenticated()) {
    return wishlistRequest('/api/wishlist', { includeGuestToken: false })
  }
  if (!getWishlistToken()) {
    return emptyWishlist()
  }
  return wishlistRequest('/api/wishlist')
}

export async function addToWishlist(productId) {
  return wishlistRequest('/api/wishlist/items', {
    method: 'POST',
    body: JSON.stringify({ productId }),
  })
}

export async function removeFromWishlist(productId) {
  return wishlistRequest(`/api/wishlist/items/${productId}`, {
    method: 'DELETE',
  })
}

export async function clearWishlist() {
  return wishlistRequest('/api/wishlist', {
    method: 'DELETE',
  })
}

/**
 * Merge guest wishlist into customer wishlist. Requires customer JWT.
 * Caller clears guest token only after success.
 */
export async function mergeGuestWishlist() {
  const data = await apiRequest('/api/wishlist/merge', {
    method: 'POST',
    auth: 'customer',
    headers: wishlistHeaders({ includeGuestToken: true }),
  })
  return mapWishlist(data)
}

export { emptyWishlist, WISHLIST_TOKEN_HEADER, clearWishlistToken }

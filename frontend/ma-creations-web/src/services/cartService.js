import { apiRequest, resolveMediaUrl } from './apiClient'
import { clearCartToken, getCartToken, setCartToken } from './cartStorage'
import { isCustomerAuthenticated } from './customerAuthStorage'

const CART_TOKEN_HEADER = 'X-Cart-Token'

function emptyCart() {
  return {
    id: null,
    guestToken: null,
    itemCount: 0,
    subtotal: 0,
    currency: 'INR',
    items: [],
  }
}

function mapCart(cart) {
  if (!cart) {
    return emptyCart()
  }
  return {
    ...cart,
    items: (cart.items || []).map((item) => ({
      ...item,
      imageUrl: resolveMediaUrl(item.imageUrl),
    })),
  }
}

function persistGuestToken(cart) {
  if (!isCustomerAuthenticated() && cart?.guestToken) {
    setCartToken(cart.guestToken)
  }
  return mapCart(cart)
}

function cartHeaders({ includeGuestToken = true } = {}) {
  const headers = {
    'Content-Type': 'application/json',
  }
  if (includeGuestToken) {
    const token = getCartToken()
    if (token) {
      headers[CART_TOKEN_HEADER] = token
    }
  }
  return headers
}

async function cartRequest(path, options = {}) {
  const authenticated = isCustomerAuthenticated()
  const includeGuestToken = options.includeGuestToken ?? !authenticated
  const data = await apiRequest(path, {
    ...options,
    auth: authenticated ? 'customer' : 'none',
    headers: {
      ...cartHeaders({ includeGuestToken }),
      ...(options.headers || {}),
    },
  })
  return persistGuestToken(data)
}

export async function getCart() {
  if (isCustomerAuthenticated()) {
    return cartRequest('/api/cart', { includeGuestToken: false })
  }
  if (!getCartToken()) {
    return emptyCart()
  }
  return cartRequest('/api/cart')
}

export async function addItem(productId, quantity = 1) {
  return cartRequest('/api/cart/items', {
    method: 'POST',
    body: JSON.stringify({ productId, quantity }),
  })
}

export async function updateQuantity(itemId, quantity) {
  return cartRequest(`/api/cart/items/${itemId}`, {
    method: 'PATCH',
    body: JSON.stringify({ quantity }),
  })
}

export async function removeItem(itemId) {
  return cartRequest(`/api/cart/items/${itemId}`, {
    method: 'DELETE',
  })
}

export async function clearCart() {
  return cartRequest('/api/cart', {
    method: 'DELETE',
  })
}

/**
 * Merge guest cart into customer cart. Requires customer JWT.
 * Sends X-Cart-Token; caller clears guest token only after success.
 */
export async function mergeGuestCart() {
  const data = await apiRequest('/api/cart/merge', {
    method: 'POST',
    auth: 'customer',
    headers: cartHeaders({ includeGuestToken: true }),
  })
  return mapCart(data)
}

export { emptyCart, CART_TOKEN_HEADER, clearCartToken }

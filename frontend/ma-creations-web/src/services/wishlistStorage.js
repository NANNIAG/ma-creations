const WISHLIST_TOKEN_KEY = 'ma_wishlist_token'

export function getWishlistToken() {
  return localStorage.getItem(WISHLIST_TOKEN_KEY)
}

export function setWishlistToken(token) {
  if (!token) {
    localStorage.removeItem(WISHLIST_TOKEN_KEY)
    return
  }
  localStorage.setItem(WISHLIST_TOKEN_KEY, token)
}

export function clearWishlistToken() {
  localStorage.removeItem(WISHLIST_TOKEN_KEY)
}

export { WISHLIST_TOKEN_KEY }

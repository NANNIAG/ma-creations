const CART_TOKEN_KEY = 'ma_cart_token'

export function getCartToken() {
  return localStorage.getItem(CART_TOKEN_KEY)
}

export function setCartToken(token) {
  if (!token) {
    localStorage.removeItem(CART_TOKEN_KEY)
    return
  }
  localStorage.setItem(CART_TOKEN_KEY, token)
}

export function clearCartToken() {
  localStorage.removeItem(CART_TOKEN_KEY)
}

export { CART_TOKEN_KEY }

const TOKEN_KEY = 'ma_customer_token'
const MOBILE_KEY = 'ma_customer_mobile'

export function getCustomerToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getCustomerMobile() {
  return localStorage.getItem(MOBILE_KEY)
}

export function isCustomerAuthenticated() {
  return Boolean(getCustomerToken())
}

export function setCustomerSession({ accessToken, mobileNumber }) {
  localStorage.setItem(TOKEN_KEY, accessToken)
  if (mobileNumber) {
    localStorage.setItem(MOBILE_KEY, mobileNumber)
  }
}

export function clearCustomerSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(MOBILE_KEY)
}

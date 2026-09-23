const TOKEN_KEY = 'ma_admin_token'
const EMAIL_KEY = 'ma_admin_email'

export function getAdminToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getAdminEmail() {
  return localStorage.getItem(EMAIL_KEY)
}

export function isAdminAuthenticated() {
  return Boolean(getAdminToken())
}

export function setAdminSession({ accessToken, email }) {
  localStorage.setItem(TOKEN_KEY, accessToken)
  if (email) {
    localStorage.setItem(EMAIL_KEY, email)
  }
}

export function clearAdminSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(EMAIL_KEY)
}

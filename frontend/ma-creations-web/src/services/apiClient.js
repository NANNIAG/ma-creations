import { getAdminToken } from './authStorage'
import { getCustomerToken } from './customerAuthStorage'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(
  /\/$/,
  '',
)

export class ApiError extends Error {
  constructor(code, message, status) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
  }
}

async function parseJson(response) {
  try {
    return await response.json()
  } catch {
    return null
  }
}

/**
 * Shared fetch wrapper for Spring Boot APIs.
 *
 * auth modes:
 * - 'none' — no Authorization (public + guest cart/wishlist)
 * - 'admin' — ma_admin_token when present
 * - 'customer' — ma_customer_token when present
 *
 * Legacy: skipAdminAuth: true → auth 'none'. Default remains 'admin' for CMS calls.
 */
export async function apiRequest(path, options = {}) {
  const { skipAdminAuth = false, auth, headers: optionHeaders, ...fetchOptions } = options

  const authMode = auth ?? (skipAdminAuth ? 'none' : 'admin')

  const headers = {
    Accept: 'application/json',
    ...(optionHeaders || {}),
  }

  // Do not set Content-Type for FormData — browser sets multipart boundary
  if (fetchOptions.body instanceof FormData) {
    delete headers['Content-Type']
  }

  if (authMode === 'admin') {
    const token = getAdminToken()
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }
  } else if (authMode === 'customer') {
    const token = getCustomerToken()
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...fetchOptions,
    headers,
  })

  const payload = await parseJson(response)

  if (!response.ok) {
    const code = payload?.error?.code || 'REQUEST_FAILED'
    const message = payload?.error?.message || `Request failed (${response.status})`
    throw new ApiError(code, message, response.status)
  }

  return payload?.data
}

export function resolveMediaUrl(url) {
  if (!url) {
    return null
  }
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  if (url.startsWith('/')) {
    return `${API_BASE_URL}${url}`
  }
  return `${API_BASE_URL}/api/media/${url}`
}

export { API_BASE_URL }

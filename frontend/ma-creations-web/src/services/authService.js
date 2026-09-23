import { apiRequest } from './apiClient'
import { clearAdminSession, setAdminSession } from './authStorage'

export async function loginAdmin({ email, password }) {
  const data = await apiRequest('/api/admin/auth/login', {
    method: 'POST',
    auth: 'none',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  setAdminSession({
    accessToken: data.accessToken,
    email: data.email,
  })
  return data
}

export function logoutAdmin() {
  clearAdminSession()
}

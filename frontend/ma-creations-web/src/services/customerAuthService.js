import { apiRequest } from './apiClient'
import { clearCustomerSession, setCustomerSession } from './customerAuthStorage'

export async function requestCustomerOtp(mobileNumber) {
  return apiRequest('/api/customer/auth/request-otp', {
    method: 'POST',
    auth: 'none',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ mobileNumber }),
  })
}

export async function verifyCustomerOtp(mobileNumber, otp) {
  const data = await apiRequest('/api/customer/auth/verify-otp', {
    method: 'POST',
    auth: 'none',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ mobileNumber, otp }),
  })
  setCustomerSession({
    accessToken: data.accessToken,
    mobileNumber: data.customer?.mobileNumber || mobileNumber,
  })
  return data
}

export async function fetchCurrentCustomer() {
  return apiRequest('/api/customer/auth/me', {
    method: 'GET',
    auth: 'customer',
  })
}

export async function logoutCustomer() {
  try {
    await apiRequest('/api/customer/auth/logout', {
      method: 'POST',
      auth: 'none',
    })
  } catch {
    /* client-side logout still proceeds */
  }
  clearCustomerSession()
}

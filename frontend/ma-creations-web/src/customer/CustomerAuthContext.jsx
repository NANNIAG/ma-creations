import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import {
  fetchCurrentCustomer,
  logoutCustomer as logoutCustomerApi,
  requestCustomerOtp,
  verifyCustomerOtp,
} from '../services/customerAuthService'
import { clearCustomerSession, isCustomerAuthenticated } from '../services/customerAuthStorage'

const CustomerAuthContext = createContext(null)

export function CustomerAuthProvider({ children }) {
  const [customer, setCustomer] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  const refreshCustomer = useCallback(async () => {
    if (!isCustomerAuthenticated()) {
      setCustomer(null)
      setIsLoading(false)
      return null
    }
    setIsLoading(true)
    setError('')
    try {
      const data = await fetchCurrentCustomer()
      setCustomer(data)
      return data
    } catch (err) {
      clearCustomerSession()
      setCustomer(null)
      setError(err.message || 'Session expired')
      return null
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    refreshCustomer()
  }, [refreshCustomer])

  const requestOtp = useCallback(async (mobileNumber) => {
    setError('')
    return requestCustomerOtp(mobileNumber)
  }, [])

  const verifyOtp = useCallback(async (mobileNumber, otp) => {
    setError('')
    setIsLoading(true)
    try {
      const data = await verifyCustomerOtp(mobileNumber, otp)
      setCustomer(data.customer)
      return data
    } catch (err) {
      setError(err.message || 'Invalid OTP')
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  const logout = useCallback(async () => {
    await logoutCustomerApi()
    setCustomer(null)
    setError('')
  }, [])

  const value = useMemo(
    () => ({
      customer,
      isAuthenticated: Boolean(customer),
      isLoading,
      error,
      requestOtp,
      verifyOtp,
      logout,
      refreshCustomer,
    }),
    [customer, isLoading, error, requestOtp, verifyOtp, logout, refreshCustomer],
  )

  return <CustomerAuthContext.Provider value={value}>{children}</CustomerAuthContext.Provider>
}

export function useCustomerAuth() {
  const ctx = useContext(CustomerAuthContext)
  if (!ctx) {
    throw new Error('useCustomerAuth must be used within a CustomerAuthProvider')
  }
  return ctx
}

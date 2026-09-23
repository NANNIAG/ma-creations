import { Navigate, Outlet, useLocation } from 'react-router-dom'
import LoadingState from '../Loading/LoadingState'
import { useCustomerAuth } from '../../customer/CustomerAuthContext'

export default function ProtectedCustomerRoute() {
  const { isAuthenticated, isLoading } = useCustomerAuth()
  const location = useLocation()

  if (isLoading) {
    return <LoadingState label="Checking account…" />
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <Outlet />
}

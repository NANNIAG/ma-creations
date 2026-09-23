import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import BrandMark from '../components/BrandMark/BrandMark'
import Button from '../components/Button/Button'
import { getAdminEmail } from '../services/authStorage'
import { logoutAdmin } from '../services/authService'

export default function AdminLayout() {
  const navigate = useNavigate()
  const email = getAdminEmail()

  function handleLogout() {
    logoutAdmin()
    navigate('/admin/login', { replace: true })
  }

  return (
    <div className="min-h-screen bg-ma-bg text-ma-text">
      <header className="border-b border-ma-border bg-ma-surface">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-3 px-4 py-4 md:px-6">
          <div>
            <BrandMark to="/admin" />
            <p className="mt-0.5 text-xs text-ma-muted">Admin CMS</p>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <nav className="flex flex-wrap gap-3 text-sm font-semibold">
              <NavLink
                to="/admin"
                end
                className={({ isActive }) =>
                  isActive ? 'text-ma-primary' : 'text-ma-text hover:text-ma-primary'
                }
              >
                Dashboard
              </NavLink>
              <NavLink
                to="/admin/products"
                end
                className={({ isActive }) =>
                  isActive ? 'text-ma-primary' : 'text-ma-text hover:text-ma-primary'
                }
              >
                Products
              </NavLink>
              <NavLink
                to="/admin/orders"
                className={({ isActive }) =>
                  isActive ? 'text-ma-primary' : 'text-ma-text hover:text-ma-primary'
                }
              >
                Orders
              </NavLink>
              <NavLink
                to="/admin/products/new"
                className={({ isActive }) =>
                  isActive ? 'text-ma-primary' : 'text-ma-text hover:text-ma-primary'
                }
              >
                Add Product
              </NavLink>
            </nav>
            {email && <span className="hidden text-xs text-ma-muted sm:inline">{email}</span>}
            <Button variant="secondary" size="sm" onClick={handleLogout}>
              Logout
            </Button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8 md:px-6">
        <Outlet />
      </main>
    </div>
  )
}

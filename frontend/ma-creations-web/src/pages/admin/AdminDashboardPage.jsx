import { Link } from 'react-router-dom'
import Button from '../../components/Button/Button'

export default function AdminDashboardPage() {
  return (
    <div>
      <h1 className="text-2xl font-bold md:text-3xl">Dashboard</h1>
      <p className="mt-2 max-w-2xl text-sm text-ma-muted">
        Simple CMS for MA CREATIONS. Add products without coding. Order analytics and inventory
        are not part of this release.
      </p>

      <div className="mt-8 grid gap-4 sm:grid-cols-2">
        <div className="rounded-xl bg-ma-surface p-5 ring-1 ring-ma-border">
          <h2 className="font-semibold">Products</h2>
          <p className="mt-2 text-sm text-ma-muted">
            View, edit, or hide catalog items from the storefront. Create new products with title,
            category, image, selling price, and MRP.
          </p>
          <div className="mt-4 flex flex-wrap gap-2">
            <Link to="/admin/products">
              <Button variant="secondary">View Products</Button>
            </Link>
            <Link to="/admin/products/new">
              <Button>Add Product</Button>
            </Link>
          </div>
        </div>
        <div className="rounded-xl bg-ma-surface p-5 ring-1 ring-ma-border">
          <h2 className="font-semibold">Storefront</h2>
          <p className="mt-2 text-sm text-ma-muted">
            View the public site to confirm new products appear in categories.
          </p>
          <Link to="/" className="mt-4 inline-block">
            <Button variant="secondary">Open storefront</Button>
          </Link>
        </div>
      </div>
    </div>
  )
}

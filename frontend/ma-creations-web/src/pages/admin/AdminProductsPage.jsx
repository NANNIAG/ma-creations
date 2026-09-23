import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Button from '../../components/Button/Button'
import ConfirmDialog from '../../components/ConfirmDialog/ConfirmDialog'
import ErrorState from '../../components/ErrorState/ErrorState'
import LoadingState from '../../components/Loading/LoadingState'
import { ApiError } from '../../services/apiClient'
import { fetchAdminProducts, updateProductStatus } from '../../services/adminProductService'

function formatMoney(value) {
  if (value === null || value === undefined || value === '') {
    return '—'
  }
  return `₹${Number(value).toFixed(2)}`
}

function formatDate(value) {
  if (!value) {
    return '—'
  }
  try {
    return new Date(value).toLocaleDateString()
  } catch {
    return '—'
  }
}

function StatusBadge({ published }) {
  if (published) {
    return (
      <span className="inline-flex rounded-full bg-green-50 px-2.5 py-0.5 text-xs font-medium text-green-800 ring-1 ring-green-200">
        Published
      </span>
    )
  }
  return (
    <span className="inline-flex rounded-full bg-amber-50 px-2.5 py-0.5 text-xs font-medium text-amber-900 ring-1 ring-amber-200">
      Hidden
    </span>
  )
}

export default function AdminProductsPage() {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [feedback, setFeedback] = useState('')
  const [statusUpdatingId, setStatusUpdatingId] = useState(null)
  const [pendingHide, setPendingHide] = useState(null)

  const loadProducts = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const data = await fetchAdminProducts()
      setProducts(data || [])
    } catch (err) {
      setError(err.message || 'Failed to load products')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadProducts()
  }, [loadProducts])

  async function applyStatus(product, published) {
    setStatusUpdatingId(product.id)
    setFeedback('')
    setError('')
    try {
      const updated = await updateProductStatus(product.id, published)
      setProducts((current) =>
        current.map((item) =>
          item.id === product.id ? { ...item, published: updated.published } : item,
        ),
      )
      setFeedback(
        published
          ? `Product “${product.title}” published.`
          : `Product “${product.title}” hidden from storefront.`,
      )
      setPendingHide(null)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        setError('Session expired. Please log in again.')
      } else {
        setError(err.message || 'Could not update product status')
      }
    } finally {
      setStatusUpdatingId(null)
    }
  }

  async function confirmHide() {
    if (!pendingHide) {
      return
    }
    await applyStatus(pendingHide, false)
  }

  if (loading) {
    return <LoadingState label="Loading products…" />
  }

  if (error && products.length === 0) {
    return <ErrorState title="Could not load products" message={error} />
  }

  return (
    <div>
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">Products</h1>
          <p className="mt-1 text-sm text-ma-muted">
            Manage catalog items. Edit products, or hide them from the storefront.
          </p>
        </div>
        <Link to="/admin/products/new">
          <Button>Add Product</Button>
        </Link>
      </div>

      {feedback && (
        <p className="mt-4 rounded-lg bg-green-50 px-3 py-2 text-sm text-green-800" role="status">
          {feedback}
        </p>
      )}
      {error && products.length > 0 && (
        <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800" role="alert">
          {error}
        </p>
      )}

      {products.length === 0 ? (
        <div className="mt-8 rounded-xl bg-ma-surface p-8 text-center ring-1 ring-ma-border">
          <p className="font-medium">No products yet</p>
          <p className="mt-2 text-sm text-ma-muted">Add your first product to populate the catalog.</p>
          <Link to="/admin/products/new" className="mt-4 inline-block">
            <Button>Add Product</Button>
          </Link>
        </div>
      ) : (
        <div className="mt-6 overflow-x-auto rounded-xl bg-ma-surface ring-1 ring-ma-border">
          <table className="min-w-[52rem] w-full text-left text-sm">
            <thead className="border-b border-ma-border text-xs uppercase tracking-wide text-ma-muted">
              <tr>
                <th className="px-4 py-3 font-semibold">Image</th>
                <th className="px-4 py-3 font-semibold">Title</th>
                <th className="px-4 py-3 font-semibold">Category</th>
                <th className="px-4 py-3 font-semibold">Status</th>
                <th className="px-4 py-3 font-semibold">Selling</th>
                <th className="px-4 py-3 font-semibold">MRP</th>
                <th className="px-4 py-3 font-semibold">Off</th>
                <th className="px-4 py-3 font-semibold">Rating</th>
                <th className="px-4 py-3 font-semibold">Created</th>
                <th className="px-4 py-3 font-semibold">Actions</th>
              </tr>
            </thead>
            <tbody>
              {products.map((product) => {
                const isPublished = product.published !== false
                const busy = statusUpdatingId === product.id
                return (
                  <tr key={product.id} className="border-b border-ma-border last:border-0">
                    <td className="px-4 py-3">
                      {product.primaryImageUrl ? (
                        <img
                          src={product.primaryImageUrl}
                          alt=""
                          className="h-12 w-12 rounded-md object-cover"
                        />
                      ) : (
                        <div className="h-12 w-12 rounded-md bg-ma-bg" />
                      )}
                    </td>
                    <td className="px-4 py-3 font-medium">{product.title}</td>
                    <td className="px-4 py-3 text-ma-muted">{product.category?.name || '—'}</td>
                    <td className="px-4 py-3">
                      <StatusBadge published={isPublished} />
                    </td>
                    <td className="px-4 py-3">{formatMoney(product.sellingPrice)}</td>
                    <td className="px-4 py-3">{formatMoney(product.mrp)}</td>
                    <td className="px-4 py-3">
                      {product.discountPercent != null ? `${product.discountPercent}%` : '—'}
                    </td>
                    <td className="px-4 py-3">
                      {product.averageRating != null
                        ? `${product.averageRating}${product.ratingCount != null ? ` (${product.ratingCount})` : ''}`
                        : '—'}
                    </td>
                    <td className="px-4 py-3 text-ma-muted">{formatDate(product.createdAt)}</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-2">
                        <Link to={`/admin/products/${product.id}/edit`}>
                          <Button variant="secondary" size="sm">
                            Edit
                          </Button>
                        </Link>
                        {isPublished ? (
                          <Button
                            variant="secondary"
                            size="sm"
                            disabled={busy}
                            onClick={() => setPendingHide(product)}
                          >
                            {busy ? 'Hiding…' : 'Hide'}
                          </Button>
                        ) : (
                          <Button
                            variant="secondary"
                            size="sm"
                            disabled={busy}
                            onClick={() => applyStatus(product, true)}
                          >
                            {busy ? 'Publishing…' : 'Publish'}
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      <ConfirmDialog
        open={Boolean(pendingHide)}
        title="Hide product?"
        message={
          pendingHide
            ? `Hide “${pendingHide.title}” from the storefront? You can publish it again later.`
            : ''
        }
        confirmLabel="Hide"
        cancelLabel="Cancel"
        busy={Boolean(statusUpdatingId)}
        onCancel={() => {
          if (!statusUpdatingId) {
            setPendingHide(null)
          }
        }}
        onConfirm={confirmHide}
      />
    </div>
  )
}

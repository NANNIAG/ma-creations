import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import ErrorState from '../../components/ErrorState/ErrorState'
import LoadingState from '../../components/Loading/LoadingState'
import ProductCard from '../../components/ProductCard/ProductCard'
import { fetchProducts } from '../../services/productService'

const SORT_OPTIONS = [
  { value: 'newest', label: 'Newest' },
  { value: 'price_asc', label: 'Price: Low to High' },
  { value: 'price_desc', label: 'Price: High to Low' },
]

export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const q = (searchParams.get('q') || '').trim()
  const sort = searchParams.get('sort') || 'newest'

  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(Boolean(q))
  const [error, setError] = useState(null)

  const title = useMemo(() => (q ? `Results for “${q}”` : 'Search'), [q])

  const loadProducts = useCallback(async () => {
    if (!q) {
      setProducts([])
      setLoading(false)
      setError(null)
      return
    }

    setLoading(true)
    setError(null)
    try {
      const data = await fetchProducts({ search: q, sort })
      setProducts(data || [])
    } catch (err) {
      setProducts([])
      setError(err.message || 'Failed to search products')
    } finally {
      setLoading(false)
    }
  }, [q, sort])

  useEffect(() => {
    loadProducts()
  }, [loadProducts])

  function handleSortChange(event) {
    const nextSort = event.target.value
    const params = new URLSearchParams()
    if (q) {
      params.set('q', q)
    }
    params.set('sort', nextSort)
    setSearchParams(params)
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-6 md:px-8 md:py-10">
      <nav className="text-sm text-ma-muted">
        <Link to="/" className="hover:text-ma-primary">
          Home
        </Link>
        <span className="mx-2">/</span>
        <span className="text-ma-text">Search</span>
      </nav>

      <div className="mt-4 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 className="text-2xl font-bold md:text-3xl">{title}</h1>
          {!q && (
            <p className="mt-1 text-sm text-ma-muted">
              Use the search icon in the header to find products.
            </p>
          )}
          {q && !loading && !error && (
            <p className="mt-1 text-sm text-ma-muted">
              {products.length} {products.length === 1 ? 'result' : 'results'}
            </p>
          )}
        </div>

        {q && (
          <label className="flex flex-col gap-1 text-sm md:min-w-56">
            <span className="font-medium text-ma-muted">Sort</span>
            <select
              className="rounded-lg border border-ma-border bg-ma-surface px-3 py-2 text-ma-text focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ma-primary"
              value={sort}
              onChange={handleSortChange}
              aria-label="Sort products"
            >
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        )}
      </div>

      {!q && (
        <div className="mt-10 rounded-xl bg-ma-surface p-8 text-center ring-1 ring-ma-border">
          <p className="font-medium text-ma-text">Enter a search term</p>
          <p className="mt-2 text-sm text-ma-muted">Try searching for a product name or category.</p>
          <button
            type="button"
            className="mt-4 text-sm font-semibold text-ma-primary hover:underline"
            onClick={() => navigate('/')}
          >
            Back to home
          </button>
        </div>
      )}

      {q && loading && <LoadingState label="Searching products…" />}

      {q && !loading && error && (
        <div className="mt-8">
          <ErrorState title="Could not search products" message={error} onRetry={loadProducts} />
        </div>
      )}

      {q && !loading && !error && products.length === 0 && (
        <div className="mt-10 rounded-xl bg-ma-surface p-8 text-center ring-1 ring-ma-border">
          <p className="font-medium text-ma-text">No products match “{q}”</p>
          <p className="mt-2 text-sm text-ma-muted">Try a different term, or browse the catalog.</p>
          <Link to="/" className="mt-4 inline-block text-sm font-semibold text-ma-primary hover:underline">
            Back to home
          </Link>
        </div>
      )}

      {q && !loading && !error && products.length > 0 && (
        <div className="mt-6 grid grid-cols-2 gap-3 md:grid-cols-3 md:gap-5 lg:grid-cols-4">
          {products.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}
    </div>
  )
}

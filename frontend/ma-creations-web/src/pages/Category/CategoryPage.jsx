import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import ErrorState from '../../components/ErrorState/ErrorState'
import LoadingState from '../../components/Loading/LoadingState'
import ProductCard from '../../components/ProductCard/ProductCard'
import { resolveCategoryBannerUrl } from '../../config/brand'
import { fetchCategories } from '../../services/categoryService'
import { fetchProducts } from '../../services/productService'

const SORT_OPTIONS = [
  { value: 'newest', label: 'Newest' },
  { value: 'price_asc', label: 'Price: Low to High' },
  { value: 'price_desc', label: 'Price: High to Low' },
]

export default function CategoryPage() {
  const { categoryId } = useParams()
  const [categories, setCategories] = useState([])
  const [products, setProducts] = useState([])
  const [sort, setSort] = useState('newest')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const category = useMemo(
    () => categories.find((item) => String(item.id) === String(categoryId)),
    [categories, categoryId],
  )
  const bannerUrl = resolveCategoryBannerUrl(category)

  const loadCategories = useCallback(async () => {
    const data = await fetchCategories()
    setCategories(data || [])
  }, [])

  const loadProducts = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchProducts({ categoryId, sort })
      setProducts(data || [])
    } catch (err) {
      setProducts([])
      setError(err.message || 'Failed to load products')
    } finally {
      setLoading(false)
    }
  }, [categoryId, sort])

  useEffect(() => {
    loadCategories().catch(() => {
      /* category name is optional enhancement; products still load */
    })
  }, [loadCategories])

  useEffect(() => {
    loadProducts()
  }, [loadProducts])

  return (
    <div className="mx-auto max-w-6xl px-4 py-6 md:px-8 md:py-10">
      <nav className="text-sm text-ma-muted">
        <Link to="/" className="hover:text-ma-primary">
          Home
        </Link>
        <span className="mx-2">/</span>
        <span className="text-ma-text">{category?.name || `Category ${categoryId}`}</span>
      </nav>

      {bannerUrl && (
        <div className="mt-4 overflow-hidden rounded-xl bg-ma-bg">
          <img
            src={bannerUrl}
            alt={category?.name || 'Category'}
            className="block h-auto w-full"
            loading="eager"
          />
        </div>
      )}

      <div className="mt-4 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 className="text-2xl font-bold md:text-3xl">
            {category?.name || 'Products'}
          </h1>
          {!loading && !error && (
            <p className="mt-1 text-sm text-ma-muted">{products.length} products</p>
          )}
        </div>

        <label className="flex flex-col gap-1 text-sm md:min-w-56">
          <span className="font-medium text-ma-muted">Sort</span>
          <select
            className="rounded-lg border border-ma-border bg-ma-surface px-3 py-2 text-ma-text focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ma-primary"
            value={sort}
            onChange={(event) => setSort(event.target.value)}
            aria-label="Sort products"
          >
            {SORT_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      {loading && <LoadingState label="Loading products…" />}

      {!loading && error && (
        <div className="mt-8">
          <ErrorState title="Could not load products" message={error} onRetry={loadProducts} />
        </div>
      )}

      {!loading && !error && products.length === 0 && (
        <div className="mt-10 rounded-xl bg-ma-surface p-8 text-center ring-1 ring-ma-border">
          <p className="font-medium text-ma-text">No products in this category yet</p>
          <p className="mt-2 text-sm text-ma-muted">
            Check back soon, or browse another category from the home page.
          </p>
          <Link to="/" className="mt-4 inline-block text-sm font-semibold text-ma-primary hover:underline">
            Back to home
          </Link>
        </div>
      )}

      {!loading && !error && products.length > 0 && (
        <div className="mt-6 grid grid-cols-2 gap-3 md:grid-cols-3 md:gap-5 lg:grid-cols-4">
          {products.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}
    </div>
  )
}

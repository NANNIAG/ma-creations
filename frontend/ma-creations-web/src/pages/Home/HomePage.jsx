import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import CategoryCard from '../../components/CategoryCard/CategoryCard'
import ErrorState from '../../components/ErrorState/ErrorState'
import HeroCarousel from '../../components/HeroCarousel/HeroCarousel'
import InstagramSection from '../../components/InstagramSection/InstagramSection'
import LoadingState from '../../components/Loading/LoadingState'
import ProductCard from '../../components/ProductCard/ProductCard'
import { fetchCategories } from '../../services/categoryService'
import { fetchProducts } from '../../services/productService'

export default function HomePage() {
  const [categories, setCategories] = useState([])
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [categoryData, productData] = await Promise.all([
        fetchCategories(),
        fetchProducts({ sort: 'newest' }),
      ])
      setCategories(categoryData || [])
      setProducts(productData || [])
    } catch (err) {
      setError(err.message || 'Failed to load catalog')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  return (
    <>
      <HeroCarousel />

      <section id="categories" className="mx-auto max-w-6xl px-4 py-8 md:px-8 md:py-10">
        <h2 className="text-xl font-bold md:text-2xl">Shop by category</h2>
        <p className="mt-1 text-sm text-ma-muted">
          Five fixed categories from the live catalog API.
        </p>

        {loading && <LoadingState label="Loading categories…" />}
        {!loading && error && (
          <div className="mt-6">
            <ErrorState title="Could not load catalog" message={error} onRetry={load} />
          </div>
        )}
        {!loading && !error && categories.length === 0 && (
          <div className="mt-6 rounded-xl bg-ma-surface p-6 text-center ring-1 ring-ma-border">
            <p className="font-medium">No categories yet</p>
            <p className="mt-1 text-sm text-ma-muted">
              Categories are seeded by the API. Check that the backend is running.
            </p>
          </div>
        )}
        {!loading && !error && categories.length > 0 && (
          <div className="mt-5 flex gap-4 overflow-x-auto pb-2 md:flex-wrap md:overflow-visible">
            {categories.map((category, index) => (
              <CategoryCard key={category.id} category={category} index={index} />
            ))}
          </div>
        )}
      </section>

      <section className="mx-auto max-w-6xl px-4 py-4 md:px-8 md:py-6" aria-labelledby="featured-heading">
        <div className="flex flex-wrap items-end justify-between gap-2">
          <div>
            <h2 id="featured-heading" className="text-xl font-bold md:text-2xl">
              Bestsellers & Featured
            </h2>
            <p className="mt-1 text-sm text-ma-muted">
              Temporary fallback: newest products until bestseller/featured rules are confirmed.
            </p>
          </div>
        </div>

        {loading && <LoadingState label="Loading products…" />}

        {!loading && !error && products.length === 0 && (
          <div className="mt-6 rounded-xl bg-ma-surface p-8 text-center ring-1 ring-ma-border">
            <p className="font-medium">No products yet</p>
            <p className="mt-2 text-sm text-ma-muted">
              Add products from the admin CMS. Nothing is invented for display.
            </p>
            <Link
              to="/admin/products/new"
              className="mt-4 inline-block text-sm font-semibold text-ma-primary hover:underline"
            >
              Go to Add Product
            </Link>
          </div>
        )}

        {!loading && !error && products.length > 0 && (
          <div className="mt-5 grid grid-cols-2 gap-3 md:grid-cols-3 md:gap-5 lg:grid-cols-4">
            {products.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        )}
      </section>

      <InstagramSection />
    </>
  )
}

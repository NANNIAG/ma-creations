import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import Button from '../../components/Button/Button'
import DiscountBadge from '../../components/DiscountBadge/DiscountBadge'
import ErrorState from '../../components/ErrorState/ErrorState'
import LoadingState from '../../components/Loading/LoadingState'
import PriceDisplay from '../../components/PriceDisplay/PriceDisplay'
import RatingDisplay from '../../components/RatingDisplay/RatingDisplay'
import { useCart } from '../../cart/CartContext'
import { useWishlist } from '../../wishlist/WishlistContext'
import { buildWhatsAppUrl, isWhatsAppConfigured } from '../../config/brand'
import { ApiError } from '../../services/apiClient'
import { fetchProductById } from '../../services/productService'

export default function ProductDetailsPage() {
  const { id } = useParams()
  const { addItem, mutating: cartMutating } = useCart()
  const { isWishlisted, toggleWishlist, mutating: wishlistMutating } = useWishlist()
  const [product, setProduct] = useState(null)
  const [activeImage, setActiveImage] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [notFound, setNotFound] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    setNotFound(false)
    try {
      const data = await fetchProductById(id)
      setProduct(data)
      setActiveImage(data.images?.[0]?.url || null)
    } catch (err) {
      setProduct(null)
      if (err instanceof ApiError && (err.status === 404 || err.code === 'PRODUCT_NOT_FOUND')) {
        setNotFound(true)
      } else {
        setError(err.message || 'Failed to load product')
      }
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    load()
  }, [load])

  const whatsappHref = useMemo(() => {
    if (!product || !isWhatsAppConfigured()) {
      return null
    }
    return buildWhatsAppUrl(
      `Hi MA CREATIONS, I'm interested in "${product.title}" (product #${product.id}).`,
    )
  }, [product])

  async function handleAddToCart() {
    if (!product) {
      return
    }
    try {
      await addItem(product.id, 1)
    } catch {
      /* notice shown by CartProvider */
    }
  }

  async function handleWishlistToggle() {
    if (!product || wishlistMutating) {
      return
    }
    try {
      await toggleWishlist(product.id)
    } catch {
      /* error kept in WishlistContext */
    }
  }

  const wishlisted = product ? isWishlisted(product.id) : false

  if (loading) {
    return <LoadingState label="Loading product…" />
  }

  if (notFound) {
    return (
      <div className="mx-auto max-w-6xl px-4 py-10 md:px-8">
        <ErrorState
          title="Product not found"
          message="This product does not exist or was removed."
        />
        <div className="mt-4 text-center">
          <Link to="/" className="text-sm font-semibold text-ma-primary">
            Back to home
          </Link>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="mx-auto max-w-6xl px-4 py-10 md:px-8">
        <ErrorState title="Could not load product" message={error} onRetry={load} />
      </div>
    )
  }

  if (!product) {
    return null
  }

  return (
    <div className="mx-auto max-w-6xl px-4 pb-28 pt-6 md:px-8 md:pb-12 md:pt-10">
      <nav className="text-sm text-ma-muted">
        <Link to="/" className="hover:text-ma-primary">
          Home
        </Link>
        {product.category && (
          <>
            <span className="mx-2">/</span>
            <Link
              to={`/categories/${product.category.id}`}
              className="hover:text-ma-primary"
            >
              {product.category.name}
            </Link>
          </>
        )}
        <span className="mx-2">/</span>
        <span className="text-ma-text">{product.title}</span>
      </nav>

      <div className="mt-6 grid gap-8 md:grid-cols-2">
        <div>
          <div className="aspect-square overflow-hidden rounded-xl bg-ma-border/40">
            {activeImage ? (
              <img
                src={activeImage}
                alt={product.title}
                className="h-full w-full object-cover"
              />
            ) : (
              <div className="flex h-full items-center justify-center text-ma-muted">No image</div>
            )}
          </div>
          {product.images?.length > 1 && (
            <div className="mt-3 flex gap-2 overflow-x-auto pb-1">
              {product.images.map((image) => (
                <button
                  key={image.id}
                  type="button"
                  onClick={() => setActiveImage(image.url)}
                  aria-label={`View image ${image.sortOrder + 1 || ''}`}
                  aria-pressed={activeImage === image.url}
                  className={`h-16 w-16 shrink-0 overflow-hidden rounded-lg border-2 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ma-primary ${
                    activeImage === image.url ? 'border-ma-primary' : 'border-transparent'
                  }`}
                >
                  <img src={image.url} alt="" className="h-full w-full object-cover" />
                </button>
              ))}
            </div>
          )}
        </div>

        <div>
          <h1 className="text-2xl font-bold md:text-3xl">{product.title}</h1>
          {product.category && (
            <p className="mt-2 text-sm text-ma-muted">{product.category.name}</p>
          )}

          <div className="mt-4">
            <RatingDisplay
              averageRating={product.averageRating}
              ratingCount={product.ratingCount}
              size="lg"
            />
          </div>

          <div className="mt-4 flex flex-wrap items-center gap-3">
            <PriceDisplay
              sellingPrice={product.sellingPrice}
              mrp={product.mrp}
              size="lg"
            />
            <DiscountBadge percent={product.discountPercent} />
          </div>

          <div className="mt-8 hidden flex-col gap-3 md:flex">
            <div className="flex gap-3">
              <Button className="flex-1" disabled={cartMutating} onClick={handleAddToCart}>
                Add to Cart
              </Button>
              {whatsappHref ? (
                <a href={whatsappHref} target="_blank" rel="noreferrer" className="flex-1">
                  <Button variant="secondary" fullWidth>
                    Buy via WhatsApp
                  </Button>
                </a>
              ) : (
                <Button
                  variant="secondary"
                  className="flex-1"
                  disabled
                  title="Set VITE_WHATSAPP_NUMBER in .env.local"
                >
                  Buy via WhatsApp (configure number)
                </Button>
              )}
            </div>
            <Button
              variant="ghost"
              disabled={wishlistMutating}
              aria-pressed={wishlisted}
              aria-label={wishlisted ? 'Remove from wishlist' : 'Add to wishlist'}
              onClick={handleWishlistToggle}
            >
              {wishlisted ? 'Remove from Wishlist' : 'Add to Wishlist'}
            </Button>
          </div>
        </div>
      </div>

      {/* Mobile sticky CTA — PDF requirement */}
      <div
        className="fixed inset-x-0 bottom-0 z-40 border-t border-ma-border bg-ma-bg/95 p-3 backdrop-blur md:hidden"
        role="region"
        aria-label="Product actions"
      >
        <div className="mx-auto flex max-w-6xl flex-col gap-2">
          <div className="flex gap-2">
            <Button className="flex-1" disabled={cartMutating} onClick={handleAddToCart}>
              Add to Cart
            </Button>
            {whatsappHref ? (
              <a href={whatsappHref} target="_blank" rel="noreferrer" className="flex-1">
                <Button variant="secondary" fullWidth>
                  Buy via WhatsApp
                </Button>
              </a>
            ) : (
              <Button
                variant="secondary"
                className="flex-1"
                disabled
                title="Set VITE_WHATSAPP_NUMBER in .env.local"
              >
                WhatsApp TBD
              </Button>
            )}
          </div>
          <Button
            variant="ghost"
            fullWidth
            disabled={wishlistMutating}
            aria-pressed={wishlisted}
            aria-label={wishlisted ? 'Remove from wishlist' : 'Add to wishlist'}
            onClick={handleWishlistToggle}
          >
            {wishlisted ? 'Remove from Wishlist' : 'Add to Wishlist'}
          </Button>
        </div>
      </div>
    </div>
  )
}

import { Link } from 'react-router-dom'
import Button from '../Button/Button'
import DiscountBadge from '../DiscountBadge/DiscountBadge'
import PriceDisplay from '../PriceDisplay/PriceDisplay'
import RatingDisplay from '../RatingDisplay/RatingDisplay'
import { useCart } from '../../cart/CartContext'
import { useWishlist } from '../../wishlist/WishlistContext'

export default function ProductCard({ product, showAddToCart = true }) {
  const { addItem, mutating: cartMutating } = useCart()
  const { isWishlisted, toggleWishlist, mutating: wishlistMutating } = useWishlist()

  if (!product) {
    return null
  }

  const wishlisted = isWishlisted(product.id)

  async function handleAddToCart(event) {
    event.preventDefault()
    try {
      await addItem(product.id, 1)
    } catch {
      /* notice shown by CartProvider */
    }
  }

  async function handleWishlist(event) {
    event.preventDefault()
    event.stopPropagation()
    if (wishlistMutating) {
      return
    }
    try {
      await toggleWishlist(product.id)
    } catch {
      /* error kept in WishlistContext */
    }
  }

  return (
    <article className="relative flex h-full flex-col overflow-hidden rounded-xl bg-ma-surface">
      <button
        type="button"
        className="absolute right-2 top-2 z-10 inline-flex h-9 w-9 items-center justify-center rounded-full bg-ma-bg/90 text-ma-text shadow-sm ring-1 ring-ma-border transition hover:bg-ma-bg focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ma-primary disabled:opacity-50"
        aria-label={wishlisted ? 'Remove from wishlist' : 'Add to wishlist'}
        aria-pressed={wishlisted}
        disabled={wishlistMutating}
        onClick={handleWishlist}
      >
        <HeartIcon filled={wishlisted} />
      </button>

      <Link to={`/products/${product.id}`} className="block overflow-hidden">
        <div className="aspect-square bg-ma-border/40">
          {product.primaryImageUrl ? (
            <img
              src={product.primaryImageUrl}
              alt={product.title}
              className="h-full w-full object-cover transition duration-300 hover:scale-[1.02]"
              loading="lazy"
            />
          ) : (
            <div className="flex h-full items-center justify-center text-sm text-ma-muted">
              No image
            </div>
          )}
        </div>
      </Link>

      <div className="flex flex-1 flex-col gap-2 p-3 md:p-4">
        <Link to={`/products/${product.id}`} className="line-clamp-2 font-semibold text-ma-text">
          {product.title}
        </Link>
        <div className="flex flex-wrap items-center gap-2">
          <PriceDisplay sellingPrice={product.sellingPrice} mrp={product.mrp} />
          <DiscountBadge percent={product.discountPercent} />
        </div>
        <RatingDisplay
          averageRating={product.averageRating}
          ratingCount={product.ratingCount}
        />
        {showAddToCart && (
          <Button
            variant="primary"
            size="sm"
            className="mt-auto"
            disabled={cartMutating}
            onClick={handleAddToCart}
          >
            Add to Cart
          </Button>
        )}
      </div>
    </article>
  )
}

function HeartIcon({ filled }) {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill={filled ? 'currentColor' : 'none'} aria-hidden>
      <path
        d="M12 20s-7-4.4-7-9.2A3.8 3.8 0 0 1 12 8a3.8 3.8 0 0 1 7 2.8C19 15.6 12 20 12 20Z"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinejoin="round"
        className={filled ? 'text-ma-primary' : 'text-ma-text'}
      />
    </svg>
  )
}

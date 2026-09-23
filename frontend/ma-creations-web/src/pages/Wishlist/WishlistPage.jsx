import { Link } from 'react-router-dom'
import ErrorState from '../../components/ErrorState/ErrorState'
import LoadingState from '../../components/Loading/LoadingState'
import ProductCard from '../../components/ProductCard/ProductCard'
import { useWishlist } from '../../wishlist/WishlistContext'

export default function WishlistPage() {
  const { items, itemCount, loading, error, refreshWishlist } = useWishlist()

  return (
    <div className="mx-auto max-w-6xl px-4 py-6 md:px-8 md:py-10">
      <nav className="text-sm text-ma-muted">
        <Link to="/" className="hover:text-ma-primary">
          Home
        </Link>
        <span className="mx-2">/</span>
        <span className="text-ma-text">Wishlist</span>
      </nav>

      <div className="mt-4">
        <h1 className="text-2xl font-bold md:text-3xl">Wishlist</h1>
        <p className="mt-1 text-sm text-ma-muted">
          {loading ? 'Loading…' : `${itemCount} ${itemCount === 1 ? 'item' : 'items'}`}
        </p>
      </div>

      {loading && <LoadingState label="Loading wishlist…" />}

      {!loading && error && items.length === 0 && (
        <div className="mt-8">
          <ErrorState title="Could not load wishlist" message={error} onRetry={refreshWishlist} />
        </div>
      )}

      {!loading && !error && items.length === 0 && (
        <div className="mt-10 rounded-xl bg-ma-surface p-8 text-center ring-1 ring-ma-border">
          <p className="font-medium text-ma-text">Your wishlist is empty</p>
          <p className="mt-2 text-sm text-ma-muted">
            Tap the heart on products to save them here.
          </p>
          <Link
            to="/"
            className="mt-4 inline-block text-sm font-semibold text-ma-primary hover:underline"
          >
            Continue shopping
          </Link>
        </div>
      )}

      {!loading && items.length > 0 && (
        <div className="mt-6 grid grid-cols-2 gap-3 md:grid-cols-3 md:gap-5 lg:grid-cols-4">
          {items.map((item) => (
            <ProductCard
              key={item.productId}
              product={{
                id: item.productId,
                title: item.title,
                sellingPrice: item.unitPrice ?? item.sellingPrice,
                mrp: item.mrp,
                discountPercent: item.discountPercent,
                primaryImageUrl: item.primaryImageUrl || item.imageUrl,
              }}
            />
          ))}
        </div>
      )}
    </div>
  )
}

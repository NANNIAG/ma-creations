import { Link, useNavigate } from 'react-router-dom'
import Button from '../../components/Button/Button'
import ErrorState from '../../components/ErrorState/ErrorState'
import LoadingState from '../../components/Loading/LoadingState'
import PriceDisplay from '../../components/PriceDisplay/PriceDisplay'
import { useCart } from '../../cart/CartContext'

function formatInr(value) {
  const number = Number(value)
  if (Number.isNaN(number)) {
    return String(value ?? '')
  }
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(number)
}

export default function CartPage() {
  const navigate = useNavigate()
  const {
    cart,
    loading,
    mutating,
    error,
    notice,
    refreshCart,
    updateQuantity,
    removeItem,
    clearCart,
  } = useCart()

  const items = cart?.items || []
  const isEmpty = !loading && items.length === 0

  async function handleQuantityChange(item, nextQuantity) {
    if (nextQuantity < 1 || nextQuantity > 99 || mutating) {
      return
    }
    try {
      await updateQuantity(item.id, nextQuantity)
    } catch {
      /* notice shown by context */
    }
  }

  async function handleRemove(itemId) {
    if (mutating) {
      return
    }
    try {
      await removeItem(itemId)
    } catch {
      /* notice shown by context */
    }
  }

  async function handleClear() {
    if (mutating || items.length === 0) {
      return
    }
    try {
      await clearCart()
    } catch {
      /* notice shown by context */
    }
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-6 md:px-8 md:py-10">
      <nav className="text-sm text-ma-muted">
        <Link to="/" className="hover:text-ma-primary">
          Home
        </Link>
        <span className="mx-2">/</span>
        <span className="text-ma-text">Cart</span>
      </nav>

      <div className="mt-4 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold md:text-3xl">Your cart</h1>
          <p className="mt-1 text-sm text-ma-muted">
            {loading ? 'Loading…' : `${cart?.itemCount ?? 0} items`}
          </p>
        </div>
        {!isEmpty && (
          <Button variant="ghost" size="sm" disabled={mutating} onClick={handleClear}>
            Clear cart
          </Button>
        )}
      </div>

      {notice && (
        <p className="mt-4 rounded-lg bg-ma-primary/15 px-3 py-2 text-sm" role="status">
          {notice}
        </p>
      )}

      {loading && <LoadingState label="Loading cart…" />}

      {!loading && error && items.length === 0 && (
        <div className="mt-8">
          <ErrorState title="Could not load cart" message={error} onRetry={refreshCart} />
        </div>
      )}

      {isEmpty && !error && (
        <div className="mt-10 rounded-xl bg-ma-surface p-8 text-center ring-1 ring-ma-border">
          <p className="font-medium text-ma-text">Your cart is empty</p>
          <p className="mt-2 text-sm text-ma-muted">Add products from the catalog to get started.</p>
          <Link
            to="/"
            className="mt-4 inline-block text-sm font-semibold text-ma-primary hover:underline"
          >
            Continue shopping
          </Link>
        </div>
      )}

      {!loading && items.length > 0 && (
        <div className="mt-6 grid gap-8 lg:grid-cols-[minmax(0,1fr)_280px]">
          <ul className="space-y-4" aria-label="Cart items">
            {items.map((item) => (
              <li
                key={item.id}
                className="flex gap-3 rounded-xl bg-ma-surface p-3 ring-1 ring-ma-border md:gap-4 md:p-4"
              >
                <Link
                  to={`/products/${item.productId}`}
                  className="h-24 w-24 shrink-0 overflow-hidden rounded-lg bg-ma-border/40 md:h-28 md:w-28"
                >
                  {item.imageUrl ? (
                    <img
                      src={item.imageUrl}
                      alt={item.title}
                      className="h-full w-full object-cover"
                      loading="lazy"
                    />
                  ) : (
                    <div className="flex h-full items-center justify-center text-xs text-ma-muted">
                      No image
                    </div>
                  )}
                </Link>

                <div className="flex min-w-0 flex-1 flex-col gap-2">
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <Link
                      to={`/products/${item.productId}`}
                      className="font-semibold text-ma-text hover:text-ma-primary"
                    >
                      {item.title}
                    </Link>
                    <p className="font-semibold">{formatInr(item.lineTotal)}</p>
                  </div>

                  <PriceDisplay sellingPrice={item.unitPrice} mrp={item.mrp} />

                  <div className="mt-auto flex flex-wrap items-center gap-3">
                    <div
                      className="inline-flex items-center rounded-lg ring-1 ring-ma-border"
                      role="group"
                      aria-label={`Quantity for ${item.title}`}
                    >
                      <button
                        type="button"
                        className="h-9 w-9 text-lg disabled:opacity-40"
                        aria-label="Decrease quantity"
                        disabled={mutating || item.quantity <= 1}
                        onClick={() => handleQuantityChange(item, item.quantity - 1)}
                      >
                        −
                      </button>
                      <span className="min-w-8 text-center text-sm font-semibold" aria-live="polite">
                        {item.quantity}
                      </span>
                      <button
                        type="button"
                        className="h-9 w-9 text-lg disabled:opacity-40"
                        aria-label="Increase quantity"
                        disabled={mutating || item.quantity >= 99}
                        onClick={() => handleQuantityChange(item, item.quantity + 1)}
                      >
                        +
                      </button>
                    </div>

                    <Button
                      variant="ghost"
                      size="sm"
                      disabled={mutating}
                      onClick={() => handleRemove(item.id)}
                    >
                      Remove
                    </Button>
                  </div>
                </div>
              </li>
            ))}
          </ul>

          <aside className="h-fit rounded-xl bg-ma-surface p-5 ring-1 ring-ma-border">
            <h2 className="text-lg font-bold">Order summary</h2>
            <div className="mt-4 flex items-center justify-between text-sm">
              <span className="text-ma-muted">Subtotal</span>
              <span className="font-semibold">{formatInr(cart.subtotal)}</span>
            </div>
            <p className="mt-2 text-xs text-ma-muted">Taxes and shipping calculated at checkout.</p>
            <Button
              className="mt-5 w-full"
              disabled={mutating || items.length === 0}
              onClick={() => navigate('/checkout')}
            >
              Proceed to checkout
            </Button>
            <Link
              to="/"
              className="mt-3 block text-center text-sm font-semibold text-ma-primary hover:underline"
            >
              Continue shopping
            </Link>
          </aside>
        </div>
      )}
    </div>
  )
}

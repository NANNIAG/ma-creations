import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useCustomerAuth } from '../../customer/CustomerAuthContext'
import BrandMark from '../BrandMark/BrandMark'

function IconButton({ label, children, disabled = false, onClick, as: Component = 'button', to }) {
  const className =
    'relative inline-flex h-10 w-10 items-center justify-center rounded-full text-ma-text transition hover:bg-ma-border/50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ma-primary disabled:cursor-not-allowed disabled:opacity-55'

  if (Component === Link) {
    return (
      <Link to={to} aria-label={label} title={label} className={className}>
        {children}
      </Link>
    )
  }

  return (
    <button
      type="button"
      aria-label={label}
      title={disabled ? `${label} — pending confirmation` : label}
      disabled={disabled}
      aria-disabled={disabled || undefined}
      onClick={onClick}
      className={className}
    >
      {children}
    </button>
  )
}

/**
 * Storefront header. Search, wishlist, and cart are live.
 */
export default function Header({ cartCount = 0, wishlistCount = 0 }) {
  const navigate = useNavigate()
  const { isAuthenticated } = useCustomerAuth()
  const [searchOpen, setSearchOpen] = useState(false)
  const [query, setQuery] = useState('')
  const inputRef = useRef(null)
  const cartBadge = Number(cartCount) > 99 ? '99+' : cartCount
  const wishlistBadge = Number(wishlistCount) > 99 ? '99+' : wishlistCount

  useEffect(() => {
    if (searchOpen) {
      inputRef.current?.focus()
    }
  }, [searchOpen])

  function submitSearch(event) {
    event.preventDefault()
    const trimmed = query.trim()
    if (!trimmed) {
      return
    }
    setSearchOpen(false)
    navigate(`/search?q=${encodeURIComponent(trimmed)}`)
  }

  return (
    <header className="sticky top-0 z-40 border-b border-ma-border/80 bg-ma-bg/95 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-6xl items-center justify-between gap-3 px-4 md:h-16 md:px-8">
        <BrandMark />

        <div className="flex min-w-0 flex-1 items-center justify-end gap-1 md:gap-2" aria-label="Store utilities">
          {searchOpen && (
            <form
              onSubmit={submitSearch}
              className="mr-1 flex min-w-0 flex-1 items-center gap-1 md:max-w-xs md:flex-none"
              role="search"
            >
              <label htmlFor="header-search" className="sr-only">
                Search products
              </label>
              <input
                ref={inputRef}
                id="header-search"
                type="search"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Search products"
                maxLength={100}
                className="min-w-0 flex-1 rounded-lg border border-ma-border bg-ma-surface px-3 py-2 text-sm text-ma-text placeholder:text-ma-muted focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ma-primary"
              />
              <button
                type="submit"
                className="shrink-0 rounded-lg bg-ma-primary px-3 py-2 text-sm font-semibold text-white transition hover:brightness-95 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ma-primary"
              >
                Search
              </button>
            </form>
          )}

          <IconButton
            label={searchOpen ? 'Close search' : 'Search'}
            onClick={() => setSearchOpen((open) => !open)}
          >
            <SearchIcon />
          </IconButton>
          <IconButton
            as={Link}
            to={isAuthenticated ? '/account' : '/login'}
            label={isAuthenticated ? 'Account' : 'Login'}
          >
            <AccountIcon />
          </IconButton>
          <IconButton as={Link} to="/wishlist" label="Wishlist">
            <HeartIcon />
            <span
              className="absolute right-0.5 top-0.5 inline-flex min-w-4 items-center justify-center rounded-full bg-ma-primary px-1 text-[10px] font-bold text-white"
              aria-label={`${wishlistCount} items in wishlist`}
            >
              {wishlistBadge}
            </span>
          </IconButton>
          <IconButton as={Link} to="/cart" label="Cart">
            <CartIcon />
            <span
              className="absolute right-0.5 top-0.5 inline-flex min-w-4 items-center justify-center rounded-full bg-ma-primary px-1 text-[10px] font-bold text-white"
              aria-label={`${cartCount} items in cart`}
            >
              {cartBadge}
            </span>
          </IconButton>
        </div>
      </div>
    </header>
  )
}

function SearchIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden>
      <circle cx="11" cy="11" r="7" stroke="currentColor" strokeWidth="2" />
      <path d="M20 20l-3.5-3.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  )
}

function AccountIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden>
      <circle cx="12" cy="8" r="3.5" stroke="currentColor" strokeWidth="2" />
      <path
        d="M5 19c1.5-3 4-4.5 7-4.5s5.5 1.5 7 4.5"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
    </svg>
  )
}

function HeartIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M12 20s-7-4.4-7-9.2A3.8 3.8 0 0 1 12 8a3.8 3.8 0 0 1 7 2.8C19 15.6 12 20 12 20Z"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function CartIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M4 5h2l1.5 11h11L21 8H7"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="10" cy="20" r="1.5" fill="currentColor" />
      <circle cx="17" cy="20" r="1.5" fill="currentColor" />
    </svg>
  )
}

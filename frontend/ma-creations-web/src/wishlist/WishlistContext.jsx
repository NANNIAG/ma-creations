import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { useCustomerAuth } from '../customer/CustomerAuthContext'
import { isCustomerAuthenticated } from '../services/customerAuthStorage'
import * as wishlistService from '../services/wishlistService'
import { clearWishlistToken, getWishlistToken } from '../services/wishlistStorage'

const WishlistContext = createContext(null)

export function WishlistProvider({ children }) {
  const { isAuthenticated, isLoading: authLoading } = useCustomerAuth()
  const [wishlist, setWishlist] = useState(() => wishlistService.emptyWishlist())
  const [loading, setLoading] = useState(
    () => Boolean(getWishlistToken()) || isCustomerAuthenticated(),
  )
  const [mutating, setMutating] = useState(false)
  const [error, setError] = useState(null)
  const [mergeError, setMergeError] = useState(null)
  const prevAuthenticated = useRef(false)
  const mergeInFlight = useRef(false)

  const productIds = useMemo(
    () => new Set((wishlist?.items || []).map((item) => item.productId)),
    [wishlist],
  )

  const refreshWishlist = useCallback(async () => {
    if (!isCustomerAuthenticated() && !getWishlistToken()) {
      setWishlist(wishlistService.emptyWishlist())
      setLoading(false)
      return wishlistService.emptyWishlist()
    }
    setLoading(true)
    setError(null)
    try {
      const data = await wishlistService.getWishlist()
      setWishlist(data)
      return data
    } catch (err) {
      setError(err.message || 'Failed to load wishlist')
      setWishlist(wishlistService.emptyWishlist())
      throw err
    } finally {
      setLoading(false)
    }
  }, [])

  const mergeGuestWishlistIfNeeded = useCallback(async () => {
    if (mergeInFlight.current) {
      return
    }
    const guestToken = getWishlistToken()
    if (!guestToken) {
      setMergeError(null)
      return refreshWishlist()
    }
    mergeInFlight.current = true
    setLoading(true)
    setMergeError(null)
    try {
      const data = await wishlistService.mergeGuestWishlist()
      clearWishlistToken()
      setWishlist(data)
      setMergeError(null)
      return data
    } catch (err) {
      const message = err.message || 'Could not merge wishlist. Your guest wishlist was kept.'
      setMergeError(message)
      setError(message)
      // Keep guest token — do not discard guest data
      throw err
    } finally {
      mergeInFlight.current = false
      setLoading(false)
    }
  }, [refreshWishlist])

  useEffect(() => {
    if (authLoading) {
      return
    }

    const wasAuthenticated = prevAuthenticated.current
    prevAuthenticated.current = isAuthenticated

    if (isAuthenticated && !wasAuthenticated) {
      mergeGuestWishlistIfNeeded().catch(() => {})
      return
    }

    if (!isAuthenticated && wasAuthenticated) {
      setWishlist(wishlistService.emptyWishlist())
      setMergeError(null)
      setLoading(false)
      if (getWishlistToken()) {
        refreshWishlist().catch(() => {})
      }
      return
    }

    if (isAuthenticated) {
      if (getWishlistToken()) {
        mergeGuestWishlistIfNeeded().catch(() => {})
      } else {
        refreshWishlist().catch(() => {})
      }
      return
    }

    if (getWishlistToken()) {
      refreshWishlist().catch(() => {})
    } else {
      setLoading(false)
    }
  }, [authLoading, isAuthenticated, mergeGuestWishlistIfNeeded, refreshWishlist])

  const runMutation = useCallback(async (action) => {
    setMutating(true)
    setError(null)
    try {
      const data = await action()
      setWishlist(data)
      return data
    } catch (err) {
      setError(err.message || 'Wishlist update failed')
      throw err
    } finally {
      setMutating(false)
    }
  }, [])

  const addToWishlist = useCallback(
    (productId) => runMutation(() => wishlistService.addToWishlist(productId)),
    [runMutation],
  )

  const removeFromWishlist = useCallback(
    (productId) => runMutation(() => wishlistService.removeFromWishlist(productId)),
    [runMutation],
  )

  const toggleWishlist = useCallback(
    async (productId) => {
      if (productIds.has(productId)) {
        return removeFromWishlist(productId)
      }
      return addToWishlist(productId)
    },
    [productIds, addToWishlist, removeFromWishlist],
  )

  const clearWishlist = useCallback(
    () => runMutation(() => wishlistService.clearWishlist()),
    [runMutation],
  )

  const isWishlisted = useCallback((productId) => productIds.has(productId), [productIds])

  const retryMerge = useCallback(() => mergeGuestWishlistIfNeeded(), [mergeGuestWishlistIfNeeded])

  const value = useMemo(
    () => ({
      wishlist,
      items: wishlist?.items || [],
      itemCount: wishlist?.itemCount ?? 0,
      loading,
      mutating,
      error,
      mergeError,
      isWishlisted,
      addToWishlist,
      removeFromWishlist,
      toggleWishlist,
      clearWishlist,
      refreshWishlist,
      mergeGuestWishlistIfNeeded,
      retryMerge,
    }),
    [
      wishlist,
      loading,
      mutating,
      error,
      mergeError,
      isWishlisted,
      addToWishlist,
      removeFromWishlist,
      toggleWishlist,
      clearWishlist,
      refreshWishlist,
      mergeGuestWishlistIfNeeded,
      retryMerge,
    ],
  )

  return <WishlistContext.Provider value={value}>{children}</WishlistContext.Provider>
}

export function useWishlist() {
  const context = useContext(WishlistContext)
  if (!context) {
    throw new Error('useWishlist must be used within a WishlistProvider')
  }
  return context
}

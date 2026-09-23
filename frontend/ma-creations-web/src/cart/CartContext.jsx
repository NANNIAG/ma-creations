import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { useCustomerAuth } from '../customer/CustomerAuthContext'
import * as cartService from '../services/cartService'
import { clearCartToken, getCartToken } from '../services/cartStorage'
import { isCustomerAuthenticated } from '../services/customerAuthStorage'

const CartContext = createContext(null)

export function CartProvider({ children }) {
  const { isAuthenticated, isLoading: authLoading } = useCustomerAuth()
  const [cart, setCart] = useState(() => cartService.emptyCart())
  const [loading, setLoading] = useState(
    () => Boolean(getCartToken()) || isCustomerAuthenticated(),
  )
  const [mutating, setMutating] = useState(false)
  const [error, setError] = useState(null)
  const [notice, setNotice] = useState('')
  const [mergeError, setMergeError] = useState(null)
  const prevAuthenticated = useRef(false)
  const mergeInFlight = useRef(false)

  const showNotice = useCallback((message) => {
    setNotice(message)
    window.setTimeout(() => setNotice(''), 2800)
  }, [])

  const refreshCart = useCallback(async () => {
    if (!isCustomerAuthenticated() && !getCartToken()) {
      setCart(cartService.emptyCart())
      setLoading(false)
      return cartService.emptyCart()
    }
    setLoading(true)
    setError(null)
    try {
      const data = await cartService.getCart()
      setCart(data)
      return data
    } catch (err) {
      setError(err.message || 'Failed to load cart')
      setCart(cartService.emptyCart())
      throw err
    } finally {
      setLoading(false)
    }
  }, [])

  const mergeGuestCartIfNeeded = useCallback(async () => {
    if (mergeInFlight.current) {
      return
    }
    const guestToken = getCartToken()
    if (!guestToken) {
      setMergeError(null)
      return refreshCart()
    }
    mergeInFlight.current = true
    setLoading(true)
    setMergeError(null)
    try {
      const data = await cartService.mergeGuestCart()
      clearCartToken()
      setCart(data)
      setMergeError(null)
      return data
    } catch (err) {
      const message = err.message || 'Could not merge cart. Your guest cart was kept.'
      setMergeError(message)
      setError(message)
      showNotice(message)
      // Keep guest token — do not discard guest data
      throw err
    } finally {
      mergeInFlight.current = false
      setLoading(false)
    }
  }, [refreshCart, showNotice])

  useEffect(() => {
    if (authLoading) {
      return
    }

    const wasAuthenticated = prevAuthenticated.current
    prevAuthenticated.current = isAuthenticated

    if (isAuthenticated && !wasAuthenticated) {
      mergeGuestCartIfNeeded().catch(() => {
        /* mergeError already set; guest token preserved */
      })
      return
    }

    if (!isAuthenticated && wasAuthenticated) {
      // Logout: drop local cart view only — server customer cart remains
      setCart(cartService.emptyCart())
      setMergeError(null)
      setLoading(false)
      if (getCartToken()) {
        refreshCart().catch(() => {})
      }
      return
    }

    if (isAuthenticated) {
      if (getCartToken()) {
        mergeGuestCartIfNeeded().catch(() => {})
      } else {
        refreshCart().catch(() => {})
      }
      return
    }

    if (getCartToken()) {
      refreshCart().catch(() => {})
    } else {
      setLoading(false)
    }
  }, [authLoading, isAuthenticated, mergeGuestCartIfNeeded, refreshCart])

  const runMutation = useCallback(
    async (action, { successMessage } = {}) => {
      setMutating(true)
      setError(null)
      try {
        const data = await action()
        setCart(data)
        if (successMessage) {
          showNotice(successMessage)
        }
        return data
      } catch (err) {
        const message = err.message || 'Cart update failed'
        setError(message)
        showNotice(message)
        throw err
      } finally {
        setMutating(false)
      }
    },
    [showNotice],
  )

  const addItem = useCallback(
    (productId, quantity = 1) =>
      runMutation(() => cartService.addItem(productId, quantity), {
        successMessage: 'Added to cart',
      }),
    [runMutation],
  )

  const updateQuantity = useCallback(
    (itemId, quantity) => runMutation(() => cartService.updateQuantity(itemId, quantity)),
    [runMutation],
  )

  const removeItem = useCallback(
    (itemId) =>
      runMutation(() => cartService.removeItem(itemId), {
        successMessage: 'Item removed',
      }),
    [runMutation],
  )

  const clearCart = useCallback(
    () =>
      runMutation(() => cartService.clearCart(), {
        successMessage: 'Cart cleared',
      }),
    [runMutation],
  )

  const retryMerge = useCallback(() => mergeGuestCartIfNeeded(), [mergeGuestCartIfNeeded])

  const value = useMemo(
    () => ({
      cart,
      itemCount: cart?.itemCount ?? 0,
      loading,
      mutating,
      error,
      notice,
      mergeError,
      refreshCart,
      mergeGuestCartIfNeeded,
      retryMerge,
      addItem,
      updateQuantity,
      removeItem,
      clearCart,
    }),
    [
      cart,
      loading,
      mutating,
      error,
      notice,
      mergeError,
      refreshCart,
      mergeGuestCartIfNeeded,
      retryMerge,
      addItem,
      updateQuantity,
      removeItem,
      clearCart,
    ],
  )

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export function useCart() {
  const context = useContext(CartContext)
  if (!context) {
    throw new Error('useCart must be used within a CartProvider')
  }
  return context
}

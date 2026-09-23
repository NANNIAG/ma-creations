import { useEffect } from 'react'
import { BrowserRouter } from 'react-router-dom'
import { CartProvider } from './cart/CartContext'
import { CustomerAuthProvider } from './customer/CustomerAuthContext'
import { WishlistProvider } from './wishlist/WishlistContext'
import { brandFontFamily } from './config/brand'
import AppRoutes from './routes/AppRoutes'

function App() {
  useEffect(() => {
    document.documentElement.style.setProperty('--ma-brand-font', brandFontFamily)
  }, [])

  return (
    <BrowserRouter>
      <CustomerAuthProvider>
        <CartProvider>
          <WishlistProvider>
            <AppRoutes />
          </WishlistProvider>
        </CartProvider>
      </CustomerAuthProvider>
    </BrowserRouter>
  )
}

export default App

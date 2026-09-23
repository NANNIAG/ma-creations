import Footer from '../components/Footer/Footer'
import Header from '../components/Header/Header'
import WhatsAppWidget from '../components/WhatsAppWidget/WhatsAppWidget'
import { useCart } from '../cart/CartContext'
import { useWishlist } from '../wishlist/WishlistContext'

export default function StorefrontLayout({ children }) {
  const { itemCount: cartCount, notice } = useCart()
  const { itemCount: wishlistCount } = useWishlist()

  return (
    <div className="min-h-screen bg-ma-bg text-ma-text">
      <Header cartCount={cartCount} wishlistCount={wishlistCount} />
      {notice && (
        <div className="mx-auto max-w-6xl px-4 pt-3 md:px-8" role="status">
          <p className="rounded-lg bg-ma-primary/15 px-3 py-2 text-sm text-ma-text">{notice}</p>
        </div>
      )}
      <main>{children}</main>
      <Footer />
      <WhatsAppWidget />
    </div>
  )
}

import { Route, Routes } from 'react-router-dom'
import ProtectedAdminRoute from '../components/admin/ProtectedAdminRoute'
import ProtectedCustomerRoute from '../components/account/ProtectedCustomerRoute'
import AdminLayout from '../layouts/AdminLayout'
import StorefrontLayout from '../layouts/StorefrontLayout'
import CustomerAccountPage from '../pages/account/CustomerAccountPage'
import CustomerLoginPage from '../pages/account/CustomerLoginPage'
import CustomerOrderDetailPage from '../pages/account/CustomerOrderDetailPage'
import CustomerOrdersPage from '../pages/account/CustomerOrdersPage'
import CartPage from '../pages/Cart/CartPage'
import CategoryPage from '../pages/Category/CategoryPage'
import CheckoutPage from '../pages/Checkout/CheckoutPage'
import OrderPaymentFailedPage from '../pages/Checkout/OrderPaymentFailedPage'
import OrderSuccessPage from '../pages/Checkout/OrderSuccessPage'
import HomePage from '../pages/Home/HomePage'
import ProductDetailsPage from '../pages/ProductDetails/ProductDetailsPage'
import SearchPage from '../pages/Search/SearchPage'
import WishlistPage from '../pages/Wishlist/WishlistPage'
import AdminAddProductPage from '../pages/admin/AdminAddProductPage'
import AdminDashboardPage from '../pages/admin/AdminDashboardPage'
import AdminEditProductPage from '../pages/admin/AdminEditProductPage'
import AdminLoginPage from '../pages/admin/AdminLoginPage'
import AdminOrderDetailPage from '../pages/admin/AdminOrderDetailPage'
import AdminOrdersPage from '../pages/admin/AdminOrdersPage'
import AdminProductsPage from '../pages/admin/AdminProductsPage'
import TrackOrderPage from '../pages/TrackOrder/TrackOrderPage'

export default function AppRoutes() {
  return (
    <Routes>
      <Route
        path="/"
        element={
          <StorefrontLayout>
            <HomePage />
          </StorefrontLayout>
        }
      />
      <Route
        path="/categories/:categoryId"
        element={
          <StorefrontLayout>
            <CategoryPage />
          </StorefrontLayout>
        }
      />
      <Route
        path="/products/:id"
        element={
          <StorefrontLayout>
            <ProductDetailsPage />
          </StorefrontLayout>
        }
      />
      <Route
        path="/cart"
        element={
          <StorefrontLayout>
            <CartPage />
          </StorefrontLayout>
        }
      />
      <Route
        path="/checkout"
        element={
          <StorefrontLayout>
            <CheckoutPage />
          </StorefrontLayout>
        }
      />
      <Route
        path="/order-success/:orderNumber"
        element={
          <StorefrontLayout>
            <OrderSuccessPage />
          </StorefrontLayout>
        }
      />
      <Route
        path="/order-payment-failed/:orderNumber"
        element={
          <StorefrontLayout>
            <OrderPaymentFailedPage />
          </StorefrontLayout>
        }
      />
      <Route
        path="/search"
        element={
          <StorefrontLayout>
            <SearchPage />
          </StorefrontLayout>
        }
      />
      <Route
        path="/wishlist"
        element={
          <StorefrontLayout>
            <WishlistPage />
          </StorefrontLayout>
        }
      />
      <Route
        path="/track-order"
        element={
          <StorefrontLayout>
            <TrackOrderPage />
          </StorefrontLayout>
        }
      />
      <Route
        path="/login"
        element={
          <StorefrontLayout>
            <CustomerLoginPage />
          </StorefrontLayout>
        }
      />
      <Route
        path="/account"
        element={
          <StorefrontLayout>
            <CustomerAccountPage />
          </StorefrontLayout>
        }
      />
      <Route element={<ProtectedCustomerRoute />}>
        <Route
          path="/account/orders"
          element={
            <StorefrontLayout>
              <CustomerOrdersPage />
            </StorefrontLayout>
          }
        />
        <Route
          path="/account/orders/:orderNumber"
          element={
            <StorefrontLayout>
              <CustomerOrderDetailPage />
            </StorefrontLayout>
          }
        />
      </Route>

      <Route path="/admin/login" element={<AdminLoginPage />} />
      <Route element={<ProtectedAdminRoute />}>
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<AdminDashboardPage />} />
          <Route path="products" element={<AdminProductsPage />} />
          <Route path="products/new" element={<AdminAddProductPage />} />
          <Route path="products/:id/edit" element={<AdminEditProductPage />} />
          <Route path="orders" element={<AdminOrdersPage />} />
          <Route path="orders/:orderNumber" element={<AdminOrderDetailPage />} />
        </Route>
      </Route>
    </Routes>
  )
}

# Flyway migrations (canonical)

SQL files in this folder are packaged into the Spring Boot app as `classpath:db/migration`
via the Maven `pom.xml` resource configuration.

| Version | File | Purpose |
|---|---|---|
| V1 | `V1__baseline.sql` | Validates Flyway setup only |
| V2 | `V2__create_v1_schema.sql` | Creates `category`, `product`, `product_image`, `admin_user` |
| V3 | `V3__seed_categories.sql` | Seeds the five PDF categories |
| V4 | `V4__create_cart.sql` | Guest cart |
| V5 | `V5__create_wishlist.sql` | Guest wishlist |
| V6 | `V6__add_product_published.sql` | Product Hide/Publish |
| V7 | `V7__create_customer.sql` | Customer + OTP challenge (mobile OTP auth) |
| V8 | `V8__add_customer_ownership_to_cart_wishlist.sql` | Nullable `customer_id` on cart/wishlist; guest_token nullable |
| V9 | `V9__create_order_core.sql` | `orders`, `order_items`, `order_addresses` (order core; no payment tables) |
| V10 | `V10__create_payment_transaction.sql` | `payment_transaction` (Razorpay / provider-agnostic payment attempts) |

See `docs/DATABASE_IMPLEMENTATION.md` for full column/constraint documentation.

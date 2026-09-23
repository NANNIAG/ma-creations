# MA CREATIONS — Database Implementation

**Step:** 5 — Confirmed CURRENT V1 schema via Flyway + JPA entities/repositories  
**Source of truth for DDL:** Flyway (`database/migrations/`)  
**JPA:** `spring.jpa.hibernate.ddl-auto=validate` (does not create tables)

**This document is an as-built snapshot of CURRENT V1** (four tables). It does **not** implement FINAL commerce schema.

**IMPLEMENTED also:** product Hide/Unpublish (`published`); customer mobile OTP; customer-owned cart/wishlist + merge (V8); **order core schema (V9)** + checkout preview API; **payment_transaction (V10)** + Razorpay initiate/verify/webhook. Place-order + checkout UI PENDING. See `CHECKOUT_ORDER_ANALYSIS.md` and `PAYMENT_PROVIDER_ANALYSIS.md`.

**FINAL confirmed:** orders (guest + registered), payment methods (**UPI**, **Credit/Debit Card**, **Net Banking**, **Pay Later**, **COD**); gateway **Razorpay**; Pay Later merchant enablement PENDING; COD charge rule PENDING.

Aligned with:

- `DATABASE_ER_DESIGN.md` (CURRENT V1 freeze)
- `DATABASE_REVIEW.md` section **A (Safe to implement now)**

Proposed fields from section **B** were **not** added (`tile_image_url`, `focus_notes`, `alt_text`, `display_name`, `created_by_admin_id`, UPI/Pay Later product columns, featured flags).

---

## 1. Tables created

| Table | Purpose |
|---|---|
| `category` | Five fixed product categories |
| `product` | Catalog products |
| `product_image` | Product gallery / card images |
| `admin_user` | CMS admin credentials (auth logic not implemented yet) |

---

## 2. Columns

### `category`

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | BIGINT PK AI | NOT NULL | |
| `name` | VARCHAR(100) | NOT NULL | Unique |
| `slug` | VARCHAR(120) | NOT NULL | Unique; PLP path |
| `display_order` | INT | NOT NULL | PDF order 1–5 |
| `created_at` | DATETIME(6) | NOT NULL | |
| `updated_at` | DATETIME(6) | NOT NULL | |

### `product`

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | BIGINT PK AI | NOT NULL | |
| `title` | VARCHAR(255) | NOT NULL | |
| `category_id` | BIGINT FK | NOT NULL | → `category.id` |
| `selling_price` | DECIMAL(10,2) | NOT NULL | INR |
| `mrp` | DECIMAL(10,2) | NOT NULL | INR |
| `slug` | VARCHAR(255) | NOT NULL | Unique; PDP path |
| `average_rating` | DECIMAL(3,2) | **NULL** | Display only; no Review table |
| `rating_count` | INT | **NULL** | Display only |
| `created_at` | DATETIME(6) | NOT NULL | Newest sort |
| `updated_at` | DATETIME(6) | NOT NULL | |

`percent_off` is **not** stored (derived in API later).

### `product_image`

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | BIGINT PK AI | NOT NULL | |
| `product_id` | BIGINT FK | NOT NULL | → `product.id` |
| `storage_path` | VARCHAR(512) | NOT NULL | Path or URL string; storage backend TBD |
| `sort_order` | INT | NOT NULL | `0` = primary |
| `created_at` | DATETIME(6) | NOT NULL | |

### `admin_user`

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | BIGINT PK AI | NOT NULL | |
| `login_identifier` | VARCHAR(255) | NOT NULL | Unique; email **or** username (client TBD) |
| `password_hash` | VARCHAR(255) | NOT NULL | Never plain text |
| `enabled` | BIT(1) | NOT NULL | Default true |
| `created_at` | DATETIME(6) | NOT NULL | |
| `updated_at` | DATETIME(6) | NOT NULL | |

---

## 3. Relationships

```
category (1) ──< (N) product (1) ──< (N) product_image
admin_user     (table exists; no product audit FK in V1 DDL)
```

| Parent | Child | Cardinality | FK |
|---|---|---|---|
| `category` | `product` | 1:N | `product.category_id` |
| `product` | `product_image` | 1:N | `product_image.product_id` |

---

## 4. Constraints

| Constraint | Detail |
|---|---|
| `uk_category_name` | Unique category name |
| `uk_category_slug` | Unique category slug |
| `uk_product_slug` | Unique product slug |
| `uk_admin_user_login_identifier` | Unique admin login |
| `fk_product_category` | `ON DELETE RESTRICT` — product requires existing category |
| `fk_product_image_product` | `ON DELETE CASCADE` — images removed with product |

---

## 5. Indexes

| Index | Table | Columns |
|---|---|---|
| `idx_category_display_order` | category | `display_order` |
| `idx_product_category_id` | product | `category_id` |
| `idx_product_created_at` | product | `created_at` (Newest) |
| `idx_product_selling_price` | product | `selling_price` (price sort) |
| `idx_product_image_product_id` | product_image | `product_id` |
| `idx_product_image_product_sort` | product_image | `(product_id, sort_order)` |

Plus unique indexes listed above.

---

## 6. Flyway migration versions

| Version | File | Purpose |
|---|---|---|
| V1 | `V1__baseline.sql` | Baseline / Flyway wiring (`SELECT 1`) |
| V2 | `V2__create_v1_schema.sql` | Create four V1 tables |
| V3 | `V3__seed_categories.sql` | Seed five categories |
| V4 | `V4__create_cart.sql` | Guest cart |
| V5 | `V5__create_wishlist.sql` | Guest wishlist |
| V6 | `V6__add_product_published.sql` | Product Hide/Publish |
| V7 | `V7__create_customer.sql` | `customer` + `customer_otp_challenge` (mobile OTP; hashed OTP only) |
| V8 | `V8__add_customer_ownership_to_cart_wishlist.sql` | `cart.customer_id` / `wishlist.customer_id` (nullable FK); `guest_token` nullable — guest XOR customer ownership |
| V9 | `V9__create_order_core.sql` | `orders`, `order_items` (price/title snapshots), `order_addresses` (immutable). |
| V10 | `V10__create_payment_transaction.sql` | `payment_transaction` — Razorpay/provider payment attempts (no card/CVV storage). |

Canonical path: `database/migrations/`  
Packaged to classpath `db/migration` by Maven resources in `pom.xml`.

---

## 7. Seeded categories

| display_order | name | slug |
|---|---|---|
| 1 | Hydration & Drinkware | `hydration-drinkware` |
| 2 | Lunch & Meal Prep | `lunch-meal-prep` |
| 3 | Kitchen Gadgets & Prep | `kitchen-gadgets-prep` |
| 4 | Storage & Kitchenware | `storage-kitchenware` |
| 5 | Home Decor & Festivity | `home-decor-festivity` |

No products. No admin users (no plain-text passwords).

---

## 8. How to run migrations

Migrations run automatically on Spring Boot startup when Flyway is enabled (default).

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:DB_PASSWORD = "your_mysql_password"

cd D:\Muskan\MA-CREATIONS\backend\ma-creations-api
mvn spring-boot:run
```

Ensure MySQL is running and `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` are set (see `PROJECT_SETUP.md`).

---

## 9. How to verify the database

```sql
SHOW TABLES;
-- expect: category, product, product_image, admin_user, flyway_schema_history

SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
-- expect V1, V2, V3 success

SELECT id, name, slug, display_order FROM category ORDER BY display_order;
-- expect 5 rows

SELECT COUNT(*) FROM product;        -- 0
SELECT COUNT(*) FROM product_image;  -- 0
SELECT COUNT(*) FROM admin_user;     -- 0
```

Also: `GET http://localhost:8080/api/health` → `{"status":"UP"}` after successful start (schema validate + Flyway OK).

---

## 10. Remaining client decisions

Still open (do **not** change schema until answered):

| Topic | Impact |
|---|---|
| Admin login email vs username | Naming/UX of `login_identifier` values |
| Rating data source | How `average_rating` / `rating_count` get populated |
| Image storage disk vs S3 | Meaning of `storage_path` |
| Max images per product | App validation only |
| Online payment (UPI / Card / Net Banking / Pay Later) | **IMPLEMENTED foundation** — Razorpay V10; place-order/UI PENDING |
| Cash on Delivery (COD) | **FINAL:** REQUIRED; additional charge; charge amount/rule PENDING — no COD fee config invented |
| Pay Later display / UPI badge columns | Not added — product-level flags PENDING |
| Bestsellers / Featured | Selection rules **PENDING** — no flags yet |
| Hide / Unpublish | **IMPLEMENTED** — V6 `product.published` TINYINT(1) NOT NULL DEFAULT 1; index `idx_product_published` |
| Customer mobile OTP | **IMPLEMENTED** — V7 `customer` (unique `mobile_number`, no password) + `customer_otp_challenge` (hashed OTP, expiry, attempts). Real SMS provider PENDING. |
| Cart / Wishlist ownership | **IMPLEMENTED** — V8 nullable `customer_id`; guest (`guest_token`) XOR customer; merge marks guest `MERGED` |
| Order core | **IMPLEMENTED** — V9 `orders` / `order_items` / `order_addresses`; checkout preview only (no place-order yet) |
| Optional audit `created_by_admin_id` | Not in V1 DDL |
| Category tile images | Static assets vs later column |

---

## JPA / repository mapping

| Entity | Repository |
|---|---|
| `Category` | `CategoryRepository` |
| `Product` | `ProductRepository` |
| `ProductImage` | `ProductImageRepository` |
| `AdminUser` | `AdminUserRepository` |
| `Customer` | `CustomerRepository` |
| `CustomerOtpChallenge` | `CustomerOtpChallengeRepository` |
| `Order` | `OrderRepository` |
| `OrderItem` | `OrderItemRepository` |
| `OrderAddress` | `OrderAddressRepository` |

Entities are **not** exposed via REST in this step.

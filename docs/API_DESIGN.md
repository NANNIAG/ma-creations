# MA CREATIONS — API Design

REST JSON APIs for the React storefront and admin CMS. Paths are **proposed** (the PDF does not define URLs).

**Scope layers:** **CURRENT V1** catalog/admin APIs may already exist (see `CATALOG_API_IMPLEMENTATION.md`, `ADMIN_IMPLEMENTATION.md`). **FINAL confirmed** modules (cart, wishlist, search, orders, payment) are approved business scope but **not designed as concrete contracts here** until PENDING storage/provider decisions land. Do not invent gateway-specific endpoints.

**Conventions (proposed, not in PDF)**

- Prefix: `/api/v1`
- JSON request/response
- Public GETs for catalog (**published products only** — Hide/Unpublish IMPLEMENTED)
- Admin mutating APIs behind Spring Security
- Money in INR, major units (rupees) unless later confirmed as paise
- `% off` calculated by the server: `(mrp - sellingPrice) / mrp`

---

## 1. Module status

| Module | Needed for PDF UI? | FINAL scope | CURRENT V1 / notes |
|---|---|---|---|
| Categories | Homepage tiles, CMS dropdown, PLP | Required | Implemented |
| Products | Home grid, PLP, PDP, CMS | Required + **Hide/Unpublish IMPLEMENTED** | Admin Hide/Publish; no hard delete in normal flow |
| Cart | Guest + customer persistent cart + merge | **IMPLEMENTED** | `/api/cart`, `X-Cart-Token` / CUSTOMER JWT, `POST /api/cart/merge` |
| Wishlist | Guest + customer + merge | **IMPLEMENTED** | `/api/wishlist`, `X-Wishlist-Token` / CUSTOMER JWT, `POST /api/wishlist/merge` |
| Orders | Checkout loop | **Order placement + checkout UI IMPLEMENTED** (Step 24); payment foundation IMPLEMENTED (V10 / Razorpay) | `POST /api/checkout/preview`; `POST /api/orders`; `GET /api/orders/{orderNumber}`; `POST /api/payments/*` |
| Authentication | Admin JWT; customer **mobile OTP** JWT | Admin required; customer accounts **IMPLEMENTED** (OTP provider PENDING) | Separate customer JWT |
| Reviews | PDP display; write | Display yes; write PENDING | Display fields only |
| Admin / CMS | Login + add/edit | Required; prefer hide over delete | Implemented |
| Payment | UPI + Card + Net Banking + **Pay Later** + COD | Place-order + Razorpay + V1 charges (**₹20 shipping**, **₹20 COD**, GST not charged) **IMPLEMENTED** | `POST /api/orders`, `/checkout` |
| Search | Header icon | **CONFIRMED** product search | **IMPLEMENTED** — `GET /api/products?search=` |
| WhatsApp / Instagram helpers | Named channels | Thin helpers; contacts **CONFIRMED** | Env-driven frontend |

---

## 2. Categories

**Required.**

| Method | Proposed path | Purpose | Source |
|---|---|---|---|
| GET | `/api/v1/categories` | 5 category tiles + CMS dropdown | PDF-explicit |
| GET | `/api/v1/categories/{slug}` | Category header for PLP | Inferred from PLP |

**Proposed response fields:** `id`, `name`, `slug`, `tileImageUrl`, `displayOrder`.

No public create/update/delete. Changing the set of 5 is **CLIENT CONFIRMATION REQUIRED**.

---

## 3. Products

**Required.**

| Method | Proposed path | Purpose | Source |
|---|---|---|---|
| GET | `/api/v1/products` | PLP and homepage grids | PDF-explicit |
| GET | `/api/v1/products/{slug}` | PDP | PDF-explicit |

**Query parameters for GET list (PDF-explicit sort)**

| Param | Values | Source |
|---|---|---|
| `category` | slug of one of 5 | Inferred from PLP + tiles |
| `sort` | `price_asc`, `price_desc`, `newest` | PDF-explicit |
| `featured` / `bestseller` | boolean | Inferred from homepage section; CLIENT CONFIRMATION REQUIRED |

Pagination is not in the PDF. **CLIENT CONFIRMATION REQUIRED** (`page`, `size`). A small catalog can return all items until then.

**Proposed list/card payload (matches PDF card)**

- `title`
- `imageUrl`
- `sellingPrice`
- `mrp`
- `percentOff`
- `upiBadge` (visual language; **UPI is a REQUIRED real checkout method** — FINAL; per-product flag vs always-on UI still PENDING)
- `averageRating`, `ratingCount` (PDP required; cards show them in screenshots)
- `payLaterPrice` (PDP visual; Pay Later provider PENDING if offered; not in current required-method list)

**Proposed PDP extra payload**

- `images[]` (gallery + zoom)
- Same pricing fields including Pay Later tag
- Category name

Product description, stock, variants: **PENDING** (omit from CURRENT V1 contract until confirmed).

Homepage `featured` / `bestseller` query params: **PENDING** selection rules — do not assume.

---

## 4. Cart — IMPLEMENTED (guest + customer)

Persistent cart APIs with ownership isolation:

| Method | Path | Auth |
|---|---|---|
| GET | `/api/cart` | Guest token **or** CUSTOMER JWT |
| POST | `/api/cart/items` | Guest token **or** CUSTOMER JWT |
| PATCH | `/api/cart/items/{itemId}` | Guest token **or** CUSTOMER JWT |
| DELETE | `/api/cart/items/{itemId}` | Guest token **or** CUSTOMER JWT |
| DELETE | `/api/cart` | Guest token **or** CUSTOMER JWT |
| POST | `/api/cart/merge` | **CUSTOMER JWT required** + `X-Cart-Token` for guest source |

Guest: `X-Cart-Token` / `ma_cart_token`. Customer: JWT only (no client-supplied `customer_id`). Merge attaches or merges quantities (cap 99), skips unpublished/missing products, marks guest cart `MERGED`.

---

## 5. Wishlist — IMPLEMENTED (guest + customer)

| Method | Path | Auth |
|---|---|---|
| GET | `/api/wishlist` | Guest token **or** CUSTOMER JWT |
| POST | `/api/wishlist/items` | Guest token **or** CUSTOMER JWT |
| DELETE | `/api/wishlist/items/{productId}` | Guest token **or** CUSTOMER JWT |
| DELETE | `/api/wishlist` | Guest token **or** CUSTOMER JWT |
| POST | `/api/wishlist/merge` | **CUSTOMER JWT required** + `X-Wishlist-Token` |

Guest: `X-Wishlist-Token` / `ma_wishlist_token`. Merge uses product union; duplicates once; skips unpublished/missing; marks guest wishlist `MERGED`.

---

## 6. Orders / Checkout — PARTIAL (preview foundation)

**IMPLEMENTED (Step 21):**

| Method | Path | Status |
|---|---|---|
| POST | `/api/checkout/preview` | **IMPLEMENTED** — validates cart, live Product prices, totals via pluggable calculators, preview hash |

**IMPLEMENTED (Step 24 — place-order):**

| Method | Path | Status |
|---|---|---|
| POST | `/api/orders` | **IMPLEMENTED** — guest + customer; preview hash validation; item/address snapshots; COD vs online; idempotency |
| GET | `/api/orders/{orderNumber}` | **IMPLEMENTED** — confirmation summary only (not history/tracking) |

**Schema (V9):** `orders`, `order_items` (price/title snapshots), `order_addresses`.  
**Schema (V10):** `payment_transaction` (Razorpay / provider-agnostic payment attempts).

**IMPLEMENTED (Step 23 — payment foundation):**

| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/payments/initiate` | Creates Razorpay order from server `orders.grand_total`; returns public key id + Razorpay order id + amount (paise). No amount/customerId from client. |
| POST | `/api/payments/verify` | Server-side checkout signature verification (`Utils.verifyPaymentSignature`). Frontend success is not authoritative. |
| POST | `/api/payments/fail` | Marks open payment attempt failed after checkout cancel/error; does not delete order. |
| POST | `/api/payments/webhook/razorpay` | Public; verifies `X-Razorpay-Signature`; idempotent event handling. |

**Storefront UI (Step 24):** `/checkout`, `/order-success/:orderNumber`, `/order-payment-failed/:orderNumber`.

**IMPLEMENTED (Step 26A — customer + admin order management):**

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/api/customer/orders` | CUSTOMER JWT | Paginated own orders; newest first; customer_id from JWT only |
| GET | `/api/customer/orders/{orderNumber}` | CUSTOMER JWT | Own order detail from snapshots (+ `trackingNumber` when set) |
| GET | `/api/admin/orders` | ADMIN JWT | All orders; `q` (order number / mobile), `status`, `paymentStatus`; paginated |
| GET | `/api/admin/orders/{orderNumber}` | ADMIN JWT | Full detail + safe payment transaction summary (+ `trackingNumber`) |
| PATCH | `/api/admin/orders/{orderNumber}/status` | ADMIN JWT | Fulfillment transitions only (`PLACED`→`PROCESSING`→`SHIPPED`→`DELIVERED`); never sets `PaymentStatus` |

**IMPLEMENTED (Step 26B — manual tracking + guest tracking):**

| Method | Path | Auth | Notes |
|---|---|---|---|
| PATCH | `/api/admin/orders/{orderNumber}/tracking` | ADMIN JWT | Sets/updates `trackingNumber` (AWB only); does not change order/payment status |
| GET | `/api/orders/track?orderNumber=&mobileNumber=` | Public | Guest orders only (`customer_id` NULL); mobile must match; wrong mobile → same not-found |

**Schema (V11):** `orders.tracking_number` nullable VARCHAR(64).  
**V1 tracking is manual and stores only a tracking/AWB number. Courier integration is intentionally deferred.**

**UI:** `/account/orders`, `/account/orders/:orderNumber` (shows AWB), `/track-order` (guest), `/admin/orders`, `/admin/orders/:orderNumber` (edit AWB).

**NOT implemented yet:** refunds/cancel/return, courier provider/API, tracking URL, invoices, notifications, COD collected → PAID.

**Gateway:** Razorpay **CONFIRMED** (Step 23). Pay Later is required; Razorpay Pay Later/BNPL merchant enablement remains PENDING (`RAZORPAY_PAY_LATER_ENABLED`) — do not invent a BNPL brand name.

Payment methods represented in preview enum: `UPI`, `CARD`, `NET_BANKING`, `PAY_LATER`, `COD`.  
COD → resulting statuses `PLACED` + `COD_PENDING`. Prepaid → `PENDING_PAYMENT` + `PENDING`.  
**V1 charges (Step 25):** shipping **₹20** flat (`FIXED`); COD fee **₹20** when method=COD else **₹0**; tax **₹0** (GST not charged; prices GST-inclusive). Preview hash binds these amounts. No GST line on checkout UI.

See `docs/CHECKOUT_ORDER_ANALYSIS.md` and `docs/PAYMENT_PROVIDER_ANALYSIS.md`.

---
## 7. Authentication

### 7.1 Admin (required)

PDF: owner logs into CMS without coding knowledge.

| Method | Proposed path | Purpose |
|---|---|---|
| POST | `/api/v1/admin/auth/login` | Admin login |
| POST | `/api/v1/admin/auth/logout` | Logout |
| GET | `/api/v1/admin/auth/me` | Session check |

Mechanism (session cookie vs JWT) is an implementation detail; session cookie is enough for this site.

### 7.2 Customer — IMPLEMENTED (mobile OTP; SMS provider PENDING)

**CONFIRMED FINAL:** optional customer accounts via **Mobile Number + OTP** (no password; email is not the login ID) **and** guest checkout. Real SMS/OTP provider **PENDING**.

| Method | Path | Status |
|---|---|---|
| POST | `/api/customer/auth/request-otp` | **IMPLEMENTED** |
| POST | `/api/customer/auth/verify-otp` | **IMPLEMENTED** — creates customer if new; returns customer JWT (`ROLE_CUSTOMER`) |
| GET | `/api/customer/auth/me` | **IMPLEMENTED** — requires customer JWT |
| POST | `/api/customer/auth/logout` | **IMPLEMENTED** — client discards JWT (stateless) |

Separate customer JWT secret/config from admin JWT. Guest→customer cart/wishlist merge is **IMPLEMENTED**. Order history and guest order tracking remain **PENDING**.

---

## 8. Reviews

### 8.1 Read (required for PDP)

Rating average and count must appear on PDP. They can be **fields on the product GET** rather than a separate API.

Optional:

| Method | Proposed path | Status |
|---|---|---|
| GET | `/api/v1/products/{slug}/reviews` | CLIENT CONFIRMATION REQUIRED if a review list UI is added (not wireframed) |

### 8.2 Write (CLIENT CONFIRMATION REQUIRED)

| Method | Proposed path | Status |
|---|---|---|
| POST | `/api/v1/products/{slug}/reviews` | Not in PDF |

---

## 9. Admin / CMS

**Required subset**

| Method | Proposed path | Purpose | Source |
|---|---|---|---|
| POST | `/api/v1/admin/auth/login` | Login | PDF-explicit |
| POST | `/api/v1/admin/products` | Create product | PDF-explicit form |
| POST | `/api/v1/admin/products/{id}/images` | Image upload | PDF-explicit |
| GET | `/api/v1/admin/categories` | Dropdown of 5 | PDF-explicit |

**Create product body (PDF-explicit fields only)**

- `title`
- `categoryId` (must be one of the 5)
- `sellingPrice`
- `mrp`
- image via multipart (together or as a follow-up upload)

**Named but unspecified**

| Method | Proposed path | Status |
|---|---|---|
| GET | `/api/v1/admin/dashboard` | “Dashboard Overview” has no widgets | CLIENT CONFIRMATION REQUIRED |

**Common admin APIs**

| Method | Proposed path | Status |
|---|---|---|
| GET | `/api/admin/products` | Product list (published + hidden) | **IMPLEMENTED** |
| PUT | `/api/admin/products/{id}` | Edit | **IMPLEMENTED** |
| PATCH | `/api/admin/products/{id}/status` | Hide (`published:false`) / Publish (`published:true`) | **IMPLEMENTED** |
| ~~DELETE~~ | `/api/admin/products/{id}` | Hard delete | **Removed** from normal Admin API |
| CRUD | banners, settings, orders, inventory | PENDING |

---

## 10. Payment — CONFIRMED METHODS / RAZORPAY (Step 23)

**CONFIRMED REQUIRED methods:** **UPI**, **Credit/Debit Card**, **Net Banking**, **Pay Later**, and **Cash on Delivery (COD)**.

**COD:** required; additional charge **₹20** when method=COD (**IMPLEMENTED** Step 25). COD does **not** use Razorpay.

**Gateway:** **Razorpay** (Step 23). Pay Later is required; Razorpay Pay Later/BNPL merchant enablement / rail remain **PENDING** — do not invent a BNPL provider name.

| Method | Path | Status |
|---|---|---|
| POST | `/api/payments/initiate` | **IMPLEMENTED** |
| POST | `/api/payments/verify` | **IMPLEMENTED** |
| POST | `/api/payments/webhook/razorpay` | **IMPLEMENTED** |

Refunds UI remains PENDING.

---

## 11. Search — IMPLEMENTED

Header search icon is PDF-explicit; product search is **CONFIRMED required** and **implemented**.

| Method | Path | Status |
|---|---|---|
| GET | `/api/products?search=` | **IMPLEMENTED** (optional; works with `categoryId` + `sort`) |

**Behavior:** case-insensitive substring match on `product.title`, `category.name`, and `product.slug`. Blank/whitespace search is ignored. Max length 100. Storefront route: `/search?q=` maps to API `search`.

**Not in this release:** autocomplete, typo correction, relevance ranking, synonyms, dedicated `/api/search`.

---

## 12. Social helpers (thin)

Not third-party platforms as backends; small config endpoints if settings are stored.

| Method | Proposed path | Purpose | Status |
|---|---|---|---|
| GET | `/api/v1/site/whatsapp-link?productSlug=` | Returns `wa.me` URL for Buy via WhatsApp | Number **CONFIRMED** `6395700831` (intl format = config verify) |
| GET | `/api/v1/site/instagram-feed` | Proxies live posts | Follow URL **CONFIRMED**; feed API vs embed PENDING |
| GET | `/api/v1/site/footer` | Return policy link, payment badge assets, quick links | Content PENDING |

Storefront can also use env-configured `wa.me` / Instagram Follow URL (CURRENT V1 approach).

---

## 13. Error model (proposed)

Not in PDF. Use standard HTTP codes: `400` validation, `401` admin, `404` unknown product, `409` if later stock rules appear.

Do not add extra modules (notifications, coupons, shipping rates, recommendations) until confirmed.

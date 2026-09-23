# MA CREATIONS — Checkout & Order Architecture Analysis

**Step:** 20 — ANALYSIS / DESIGN ONLY (baseline)  
**Step 21 status:** Order core schema (V9) + `POST /api/checkout/preview` **IMPLEMENTED**. Place-order, payment gateway, and checkout UI are **NOT** implemented.

**Date context:** After Steps 1–19 (catalog, admin, cart, wishlist, search, hide/unpublish, customer OTP auth, guest→customer merge). Step 21 adds order foundation + preview.

**Sources:** codebase inspection; `CLIENT_CONFIRMATIONS.md`; `REQUIREMENTS.md`; `API_DESIGN.md`; `DATABASE_DESIGN.md`.

### Step 21 implementation notes (summary)

| Item | Status |
|---|---|
| V9 `orders` / `order_items` / `order_addresses` | **IMPLEMENTED** |
| Order / payment status enums + payment method enum (incl. **Pay Later REQUIRED**) | **IMPLEMENTED** |
| Item price/title snapshots (entity ready) | **IMPLEMENTED** |
| Immutable order address entity | **IMPLEMENTED** |
| `POST /api/checkout/preview` | **IMPLEMENTED** |
| Preview HMAC binding | **IMPLEMENTED** |
| Shipping / COD / tax calculators | **IMPLEMENTED** V1: shipping ₹20 FIXED, COD ₹20 FIXED, tax ZERO (GST not charged) |
| `POST /api/orders` / payment / gateway | **IMPLEMENTED** (Step 24 place-order + Step 23 Razorpay) |
| Checkout UI / order success/failure | **IMPLEMENTED** (Step 24); order history / guest tracking **PENDING** |

---
**Legend**

| Tag | Meaning |
|---|---|
| **IMPLEMENTED** | Exists in code today |
| **CONFIRMED** | Client business requirement |
| **PENDING** | Client decision not made — do not invent |
| **PROPOSED** | Recommended design for a future implementation step |

---

## 0. Scope of this document

This document proposes an implementation-ready architecture for:

- Guest checkout
- Registered-customer checkout
- Order + line-item + address snapshots
- Payment method handling (UPI, Card, Net Banking, Pay Later*, COD)
- Server-authoritative totals
- Future payment-gateway abstraction

It does **not** create migrations, Java/React code, gateway integrations, shipping/GST/COD amounts, invoices, or notifications.

\* **Pay Later:** **REQUIRED** checkout method. Only the **provider/integration** is PENDING. `PaymentMethod.PAY_LATER` is included in checkout preview.

---

## 1. Current checkout readiness

### 1.1 What already exists (reusable)

#### Backend — catalog & pricing

| Asset | Path / note | Reuse for checkout |
|---|---|---|
| `Product` | `sellingPrice`, `mrp`, `title`, `slug`, `published` | Live price/title source at checkout validation; **not** historical source after order |
| `ProductService` / public catalog | Published-only storefront filters | Same `published` gate at place-order |
| Hide/Unpublish | V6 `product.published` | Must re-check before order creation |

#### Backend — cart

| Asset | Note |
|---|---|
| `Cart` / `CartItem` | Guest (`guest_token`) XOR customer (`customer_id`); status `ACTIVE` / `MERGED` |
| `CartService` | Get/add/update/remove/clear; qty 1–99; unpublished blocked on add |
| `POST /api/cart/merge` | Guest → customer merge (Step 19) |
| `CartMapper` | **Live** prices from `Product.sellingPrice` at response time — `CartItem` stores **no** unit price |

#### Backend — customer auth

| Asset | Note |
|---|---|
| `Customer` | `mobileNumber` (login ID), optional name/email, no password |
| Customer JWT | Separate secret; role `CUSTOMER`; ID from JWT claims |
| `SecurityUtils.currentCustomerId()` | Server-side ownership — never trust body `customerId` |
| OTP | Dev abstraction only; real SMS provider PENDING |

#### Backend — security pattern

- Guest cart/wishlist: public + guest headers
- Customer-owned ops / merge: `ROLE_CUSTOMER`
- Admin: `ROLE_ADMIN`
- Pattern to mirror: public guest checkout create; customer checkout under CUSTOMER JWT; admin order management under ADMIN

#### Frontend

| Asset | Note |
|---|---|
| `/cart` | Qty, remove, clear, subtotal; **Checkout button disabled** (“coming soon”) |
| `CartContext` | Guest token + customer JWT; merge on login; merge-failure preserves guest data |
| `CustomerAuthContext` | OTP login; `/login`, `/account` |
| `apiClient` | Auth modes `none` / `admin` / `customer` |
| Header | Cart/wishlist counts, Login/Account |

#### Database (Flyway V1–V8)

| Version | Purpose |
|---|---|
| V1–V3 | Baseline, catalog schema, category seed |
| V4–V5 | Guest cart / wishlist |
| V6 | `product.published` |
| V7 | Customer + OTP challenge |
| V8 | Cart/wishlist `customer_id` ownership |

### 1.2 What is missing (exact gaps)

| Area | Gap |
|---|---|
| Domain | No `Order`, `OrderItem`, address snapshot, payment entities |
| APIs | No `/api/checkout/*`, `/api/orders/*`, payment initiate/webhook |
| Security | No order ownership rules; no guest order lookup design implemented |
| Frontend | No `/checkout`, payment method UI, order success/failure, customer order history |
| Money rules | No shipping, COD fee, GST/tax calculators (rules PENDING) |
| Integrations | No payment gateway, courier, invoice, notification adapters |
| Admin | No order list/detail/status UI |
| Cart post-order | No `CONVERTED` / clear-on-success policy coded yet |
| Tracking | Guest order tracking verification mechanism PENDING |

### 1.3 Critical readiness insight

**Cart totals are display-time live prices.** Checkout must **re-validate and snapshot** prices server-side at order creation. Never persist an order that trusts the cart DTO amounts from the browser.

---

## 2. Proposed order data model

Goal: support **guest** and **registered** checkout with **immutable commercial history**.

### 2.1 Design principles

1. **Snapshots win** — titles, unit prices, address, method charges frozen on the order.
2. **`product_id` is optional reference** — keep FK where useful for admin links; order must remain valid if product is later unpublished/deleted (prefer soft-hide; if hard-delete ever appears, use nullable FK or RESTRICT + no delete).
3. **Separate payment vs fulfillment** — do not overload one enum for both.
4. **One active commercial order per successful placement** — cart identity is not the order identity.
5. **No client-supplied money** — all amounts computed on server.

### 2.2 `orders` (proposed)

| Field | Type | Null | Purpose |
|---|---|---|---|
| `id` | BIGINT PK | NO | Internal surrogate |
| `order_number` | VARCHAR(32) | NO | Customer-facing ID (unique) |
| `customer_id` | BIGINT FK → customer | YES | NULL = guest order |
| `status` | VARCHAR(32) | NO | Fulfillment lifecycle (see §4) |
| `payment_status` | VARCHAR(32) | NO | Payment lifecycle (see §5) |
| `payment_method` | VARCHAR(32) | NO | `UPI` / `CARD` / `NET_BANKING` / `PAY_LATER` / `COD` |
| `currency` | CHAR(3) | NO | `INR` |
| `items_subtotal` | DECIMAL(12,2) | NO | Sum of line snapshots |
| `shipping_charge` | DECIMAL(12,2) | NO | From shipping rule (0 until rules exist) |
| `cod_charge` | DECIMAL(12,2) | NO | 0 unless COD |
| `tax_amount` | DECIMAL(12,2) | NO | 0 until GST rules confirmed |
| `discount_amount` | DECIMAL(12,2) | NO | Reserved; 0 for V1 |
| `grand_total` | DECIMAL(12,2) | NO | Authoritative payable/collectible total |
| `guest_email` | VARCHAR(255) | YES | Guest contact (optional vs required = PENDING) |
| `guest_mobile` | VARCHAR(20) | YES | Guest contact (mobile strongly recommended) |
| `contact_name` | VARCHAR(150) | NO | Snapshot of buyer name at checkout |
| `contact_mobile` | VARCHAR(20) | NO | Snapshot (may equal guest/customer mobile) |
| `contact_email` | VARCHAR(255) | YES | Snapshot |
| `idempotency_key` | VARCHAR(64) | YES | Unique when present (anti double-submit) |
| `cart_id` | BIGINT | YES | Source cart reference (audit; not required for history) |
| `notes` | VARCHAR(500) | YES | Optional customer note |
| `placed_at` | DATETIME(6) | NO | Order creation instant |
| `confirmed_at` | DATETIME(6) | YES | When payment confirmed / COD accepted |
| `cancelled_at` | DATETIME(6) | YES | |
| `created_at` / `updated_at` | DATETIME(6) | NO | Audit |

**Relationships:** 1 Order → * OrderItem; 1 Order → 1 OrderAddress (shipping); 1 Order → * PaymentTransaction (optional over time).

**Indexes / constraints (proposed):**

- `UNIQUE (order_number)`
- `UNIQUE (idempotency_key)` where not null (MySQL: unique allows multiple NULLs)
- `INDEX (customer_id, placed_at)`
- `INDEX (status)`, `INDEX (payment_status)`
- `INDEX (guest_mobile)`, `INDEX (contact_mobile)` for future lookup support (not public by itself)

### 2.3 `order_items` (proposed)

| Field | Type | Null | Purpose |
|---|---|---|---|
| `id` | BIGINT PK | NO | |
| `order_id` | BIGINT FK | NO | Parent order |
| `product_id` | BIGINT FK | YES | Soft reference; nullable if product removed later |
| `product_title` | VARCHAR(255) | NO | **Snapshot** |
| `product_slug` | VARCHAR(255) | YES | Snapshot for links |
| `unit_selling_price` | DECIMAL(12,2) | NO | **Snapshot** of selling price at checkout |
| `unit_mrp` | DECIMAL(12,2) | YES | Snapshot |
| `quantity` | INT | NO | ≥ 1 |
| `line_subtotal` | DECIMAL(12,2) | NO | `unit_selling_price * quantity` at checkout |
| `created_at` | DATETIME(6) | NO | |

**Constraints:** `UNIQUE (order_id, product_id)` only if one line per product (recommended for V1, matching cart). `CHECK quantity >= 1`.

### 2.4 `order_addresses` (shipping snapshot) (proposed)

| Field | Type | Null | Purpose |
|---|---|---|---|
| `id` | BIGINT PK | NO | |
| `order_id` | BIGINT FK UNIQUE | NO | 1:1 with order for V1 |
| `full_name` | VARCHAR(150) | NO | |
| `mobile` | VARCHAR(20) | NO | |
| `email` | VARCHAR(255) | YES | |
| `line1` | VARCHAR(255) | NO | |
| `line2` | VARCHAR(255) | YES | |
| `city` | VARCHAR(100) | NO | |
| `state` | VARCHAR(100) | NO | |
| `postal_code` | VARCHAR(20) | NO | |
| `country` | VARCHAR(100) | NO | Default `India` until international confirmed |
| `created_at` | DATETIME(6) | NO | |

Immutable after place-order (updates only via explicit admin correction policy — PENDING).

### 2.5 `payment_transactions` (proposed)

| Field | Type | Null | Purpose |
|---|---|---|---|
| `id` | BIGINT PK | NO | |
| `order_id` | BIGINT FK | NO | |
| `provider` | VARCHAR(64) | YES | Null for COD / until gateway chosen |
| `provider_payment_id` | VARCHAR(128) | YES | Gateway payment/order id |
| `provider_refund_id` | VARCHAR(128) | YES | Future |
| `method` | VARCHAR(32) | NO | Mirrors order payment method |
| `amount` | DECIMAL(12,2) | NO | Must equal order grand_total for successful capture |
| `currency` | CHAR(3) | NO | INR |
| `status` | VARCHAR(32) | NO | See §5 |
| `idempotency_key` | VARCHAR(64) | YES | Initiate / webhook dedupe |
| `raw_event_ref` | VARCHAR(128) | YES | Pointer to stored webhook log id (not card data) |
| `created_at` / `updated_at` | DATETIME(6) | NO | |

**Do not store** card numbers, CVV, UPI PINs, or full account numbers.

### 2.6 Optional later (not required for first order slice)

| Entity | When |
|---|---|
| `customer_address` | If client wants saved addresses (§8 recommends **defer**) |
| `order_status_history` | Audit trail of transitions |
| `webhook_event` | Raw signed payload store for idempotency/replay |
| `shipment` | Courier AWB / tracking when shipping provider chosen |

---

## 3. Order identifier (customer-facing)

### 3.1 Recommendation

**Do not** expose internal `orders.id` as the primary customer-facing identifier.

**Proposed format:** `MAC-YYYYMMDD-XXXXXX`

| Part | Meaning |
|---|---|
| `MAC` | Brand prefix |
| `YYYYMMDD` | UTC or IST calendar date of placement (document timezone once) |
| `XXXXXX` | Zero-padded daily sequence **or** cryptographically strong random base32 segment |

### 3.2 Uniqueness strategy (concurrency-safe)

**Preferred for V1:**  
`order_number = 'MAC-' + yyyyMMdd + '-' + <12-char Crockford base32 from SecureRandom>`

- Uniqueness via **DB unique constraint** + insert retry on rare collision
- No shared counter lock; scales under concurrent checkouts
- Unpredictable enough to discourage casual enumeration (still require auth/lookup secret for guest tracking)

**Alternative (if sequential numbers required by client):**  
Dedicated `order_number_seq` table or MySQL sequence with transactional allocation — higher design cost; only if business insists on dense sequential IDs.

### 3.3 Constraint

```text
UNIQUE KEY uk_orders_order_number (order_number)
```

---

## 4. Order status model (fulfillment)

Keep **fulfillment** separate from **payment**.

### 4.1 Recommended V1 fulfillment statuses

| Status | Meaning |
|---|---|
| `PENDING_PAYMENT` | Prepaid order created; awaiting successful online payment |
| `PLACED` | Order accepted for fulfillment (COD accepted, or prepaid paid) |
| `PROCESSING` | Packing / prep |
| `SHIPPED` | Handed to courier (tracking fields later) |
| `DELIVERED` | Completed |
| `CANCELLED` | Terminal cancel |
| `PAYMENT_FAILED` | Terminal or retryable failed prepaid attempt (see transitions) |

`CONFIRMED` is intentionally omitted as redundant with `PLACED` once payment rules are clear.

### 4.2 Allowed transitions (proposed)

```text
PENDING_PAYMENT → PLACED          (payment success webhook / verify)
PENDING_PAYMENT → PAYMENT_FAILED  (hard failure / expiry policy PENDING)
PENDING_PAYMENT → CANCELLED       (customer/admin cancel before pay — policy PENDING)
PAYMENT_FAILED  → PENDING_PAYMENT (retry payment — if allowed)
PAYMENT_FAILED  → CANCELLED

COD create:     → PLACED          (directly; payment_status = COD_PENDING)

PLACED → PROCESSING → SHIPPED → DELIVERED
PLACED | PROCESSING → CANCELLED   (rules PENDING)
SHIPPED → DELIVERED
(Delivered/Cancelled generally terminal for V1)
```

### 4.3 Who causes transitions

| Transition | Actor |
|---|---|
| Create → `PENDING_PAYMENT` / `PLACED` | System at checkout |
| `PENDING_PAYMENT` → `PLACED` | Payment webhook / server verify |
| Fulfillment advances | Admin (V1) |
| Cancel | Admin and/or customer — **policy PENDING** |

### 4.4 COD vs prepaid

| Flow | Fulfillment at create | Payment status at create |
|---|---|---|
| Prepaid (UPI/Card/NetBanking/Pay Later) | `PENDING_PAYMENT` | `PENDING` |
| COD | `PLACED` | `COD_PENDING` |

Cart clearing: only after **accepted** order (`PLACED` for COD; payment success → `PLACED` for prepaid). Failed prepaid keeps cart (or restores) so customer can retry.

---

## 5. Payment status model

### 5.1 Recommended statuses

| Status | Use |
|---|---|
| `PENDING` | Online payment initiated / awaiting |
| `PAID` | Captured / success confirmed server-side |
| `FAILED` | Declined / timed out / error |
| `COD_PENDING` | Collect on delivery |
| `REFUNDED` | Full refund (future) |
| `PARTIALLY_REFUNDED` | Partial (future) |

`AUTHORIZED` omitted for V1 unless the chosen gateway uses auth-capture; add when provider selected.

### 5.2 Method mapping

| Method | Typical path |
|---|---|
| UPI / Card / Net Banking | `PENDING` → `PAID` or `FAILED` via gateway |
| Pay Later | Same online path if offered through gateway/BNPL provider (**provider PENDING**) |
| COD | `COD_PENDING` → later `PAID` when cash collected (admin mark — process PENDING) |

Gateway-specific statuses map into this enum inside `PaymentGateway` adapter — business code never switches on Razorpay/PayU strings.

---

## 6. Price snapshots

### 6.1 Why cart prices are insufficient

Today `CartItem` has **no** stored price; `CartMapper` reads `Product.sellingPrice` live. If checkout used current product prices weeks later for “order history,” totals would **rewrite history** when admin changes MRP/selling price or unpublishes products.

### 6.2 OrderItem snapshot fields (required)

At successful validation / place-order:

- `product_id` (reference)
- `product_title` (copy)
- `product_slug` (copy, optional)
- `unit_selling_price` (copy of price used for charge)
- `unit_mrp` (copy)
- `quantity`
- `line_subtotal` = unit_selling_price × quantity

Order header stores rolled-up `items_subtotal` and `grand_total` as frozen numbers.

### 6.3 Price-change policy at checkout

If live product price ≠ last cart display:

1. **Do not** silently charge the old display price.
2. **Do not** silently charge the new price without consent.
3. Return structured `CART_STALE` / `PRICE_CHANGED` from preview/place with per-line old vs new; frontend must refresh cart and ask user to continue.

Exact UX copy is implementation detail; contract must force re-preview.

---

## 7. Checkout total calculation (server-authoritative)

### 7.1 Formula (proposed)

```text
items_subtotal   = Σ order_item.line_subtotal
+ shipping_charge  (ShippingChargeCalculator — rules PENDING)
+ cod_charge       (CodChargeCalculator — amount/rule PENDING; 0 if not COD)
+ tax_amount       (TaxCalculator — GST rules PENDING; 0 until confirmed)
- discount_amount  (0 in V1; reserved)
= grand_total
```

Currency: `INR`. Scale: 2 decimal places, consistent rounding (document HALF_UP).

### 7.2 Pluggable calculators (proposed)

| Component | Responsibility | Until rules exist |
|---|---|---|
| `CheckoutPricingService` | Orchestrates line snapshots + totals | — |
| `ShippingChargeCalculator` | Interface | Return 0 **or** fail closed if shipping required without config (**decision**) |
| `CodChargeCalculator` | Interface | Fail closed or block COD method until configured |
| `TaxCalculator` | Interface | 0 if tax not yet applicable; never invent GST % |

**Recommendation:** Until shipping/COD/GST rules are confirmed, **preview/place should expose** which components are `CONFIGURED` vs `PENDING`. Prefer **blocking COD** and **blocking shippable checkout** if shipping is mandatory and unconfigured, rather than silently shipping free forever. Exact fail-closed vs zero is an open decision (see §23).

Frontend totals on `/checkout` are **display-only** echoes of preview response.

---

## 8. Shipping address

### 8.1 Collected fields (proposed minimum)

Aligned with CONFIRMED guest checkout needs (name, mobile, email, delivery address):

- Full name
- Mobile
- Email (required vs optional = **PENDING**; collect field anyway if optional)
- Address line1, line2 (optional)
- City, state, postal code
- Country (default India until confirmed otherwise)

### 8.2 Guest vs registered

| Actor | Behavior |
|---|---|
| Guest | Enter all contact + address; stored only on order snapshots |
| Customer | Prefill from `Customer` name/email/mobile when present; still write **order address snapshot**; never “live join” customer profile for history |

### 8.3 `CustomerAddress` now or later?

**Recommendation: B — defer `CustomerAddress`; use order address snapshots first.**

| Reason |
|---|
| Unblocks guest + customer checkout without new account-address UX |
| Avoids inventing multi-address rules, default address, edit/delete |
| Order history remains correct without a saved-address table |
| Saved addresses can be added later and optionally seed checkout forms |

---

## 9. Guest checkout

### 9.1 Cart ownership

- Guest continues using `ACTIVE` cart via `X-Cart-Token`.
- Checkout APIs accept guest cart token (public create with validation) **or** require preview first with same token.
- No account creation implied.

### 9.2 Contact data

- Require: name, mobile, shipping address (email per PENDING).
- Store on `orders` contact_* and `order_addresses`.

### 9.3 Order creation

1. Preview (recommended mandatory) validates cart + computes totals.
2. `POST /api/orders` with guest token + address + payment method + idempotency key.
3. Server creates order + items snapshots + address; payment initiate if prepaid.
4. On COD `PLACED`: mark cart converted/cleared (policy below).
5. On prepaid: keep cart until `PAID`.

### 9.4 Future tracking (mechanism PENDING)

**Do not invent** OTP/magic-link/email-code details.

Secure design envelope (mechanism TBD):

- Lookup must require **order_number + verifier** (e.g. mobile last digits + one-time code, or signed email link).
- Never list guest orders by mobile alone on a public endpoint.
- Never allow `GET /api/orders/{orderNumber}` without proof of possession.

---

## 10. Registered customer checkout

### 10.1 Identity

- `customer_id` from CUSTOMER JWT / `SecurityUtils` only.
- Reject body `customerId`.
- Associate order with customer; still snapshot contact/address.

### 10.2 Cart after outcomes

| Outcome | Cart handling (proposed) |
|---|---|
| COD success (`PLACED`) | Clear items or set cart status `CONVERTED` (new status) / keep empty ACTIVE cart |
| Prepaid payment success | Same as COD success |
| Prepaid payment failure | **Do not** clear cart |
| Browser close after pay but before redirect | Webhook still completes order; cart cleared by webhook side-effect or next login reconcile |
| Logout mid-checkout | Guest token may be absent; draft address is client-only until place-order — no server order yet |

**Recommendation:** Add cart status `CONVERTED` in a future migration when implementing orders, **or** clear items and leave `ACTIVE` empty cart. Prefer `CONVERTED` for audit linkage via `orders.cart_id`.

---

## 11. Product validation at checkout

### 11.1 Mandatory server checks (preview + place)

For each cart line:

| Check | On failure |
|---|---|
| Product exists | Remove/unavailable report |
| `published == true` | Unavailable (hidden) |
| Quantity 1–99 | Invalid quantity |
| Selling price present/valid | Error |
| Price equals last preview snapshot (on place) | `PRICE_CHANGED` / force re-preview |

### 11.2 Recommended API error shape (conceptual)

```json
{
  "code": "CHECKOUT_VALIDATION_FAILED",
  "issues": [
    { "productId": 12, "code": "PRODUCT_UNAVAILABLE" },
    { "productId": 15, "code": "PRICE_CHANGED", "previousPrice": 199.00, "currentPrice": 179.00 }
  ]
}
```

Frontend: refresh cart, show review UI, require new preview before place.

**Never** silently swap prices or drop lines without telling the client.

---

## 12. Inventory limitation

**Current system has no stock/inventory.**

Implications:

- Checkout cannot reserve or decrement stock.
- Overselling is possible if two buyers checkout the same physical unit concurrently.
- Do **not** invent inventory for Step 21+ unless client requires it.

Later plug-in points:

- `InventoryService.reserve(orderId, lines)` before payment
- `release` on payment failure / cancel
- `commit` on PLACED/PAID
- Admin stock fields on `product` or `inventory` table

Document limitation in checkout UI (“availability not guaranteed”) only if client asks for messaging — copy PENDING.

---

## 13. Checkout API design (proposed, not implemented)

### 13.1 Endpoints

| Method | Path | Auth | Responsibility |
|---|---|---|---|
| POST | `/api/checkout/preview` | Guest token and/or CUSTOMER JWT | Validate cart; snapshot candidate prices; compute totals; return issues |
| POST | `/api/orders` | Guest token and/or CUSTOMER JWT | Create order (idempotent); start payment or accept COD |
| GET | `/api/orders/{orderNumber}` | Customer owner **or** future guest verifier | Order detail for success page / tracking |
| POST | `/api/payments/{orderNumber}/initiate` | Same as order owner | Start/retry gateway session if needed |
| POST | `/api/payments/webhook/{provider}` | Provider signature | Update payment + fulfillment |
| GET | `/api/customer/orders` | CUSTOMER | List own orders |
| GET | `/api/customer/orders/{orderNumber}` | CUSTOMER | Own order detail |

Admin (future):

| Method | Path | Auth |
|---|---|---|
| GET | `/api/admin/orders` | ADMIN |
| GET | `/api/admin/orders/{orderNumber}` | ADMIN |
| PATCH | `/api/admin/orders/{orderNumber}/status` | ADMIN |

### 13.2 Ownership rules

- Customer A cannot read Customer B’s order by guessing `orderNumber` (check `customer_id`).
- Guest GET requires verifier (PENDING mechanism) — until then, success page may use short-lived signed token returned only at create/payment success.
- Admin JWT cannot be used as customer checkout identity.

### 13.3 Request responsibilities (high level)

**Preview:** cart identity only (+ optional payment method to estimate COD).  
**Place order:** address snapshot, contact, payment method, idempotency-key, optional preview-token/hash binding prices.

---

## 14. Checkout preview

### 14.1 Should it exist?

**Yes — strongly recommended and effectively mandatory before place-order.**

Preview should:

- Validate all cart lines
- Recalculate live prices
- Compute shipping / COD / tax via calculators
- Return grand total
- Report unavailable / price-changed lines
- Return a `previewId` or signed `previewHash` bound to line prices + totals + expiry (e.g. 15–30 minutes)

### 14.2 Place-order binding

`POST /api/orders` accepts `previewId`/`previewHash`. If hash mismatch → reject with re-preview required. Prevents TOCTOU where user previews ₹X and pays ₹Y after admin price edit.

---

## 15. Idempotency

| Operation | Strategy (proposed) |
|---|---|
| Place order | Client `Idempotency-Key` header (UUID); unique on `orders.idempotency_key`; replay returns same order |
| Payment initiate | Key per order+attempt; return existing provider session if PENDING |
| Webhook | Store `provider_event_id` unique; ignore duplicates; process at-least-once safely |

Double-click Place Order → one order.  
Network retry with same key → same order.  
Duplicate webhook → no double `PAID` transition.

---

## 16. Payment provider abstraction

### 16.1 Proposed interfaces (conceptual)

```text
PaymentGateway
  initiate(PaymentRequest) → PaymentSession (redirect URL / SDK payload)
  verify(providerPaymentId) → PaymentResult
  parseWebhook(headers, body) → PaymentWebhookEvent

PaymentService
  startPayment(order)
  handleWebhook(event)
  markCodPending(order)

PaymentRequest / PaymentResult / PaymentWebhookEvent
  amount, currency, orderNumber, method family, status
```

No Razorpay/PayU/Cashfree class names in domain layer.

### 16.2 Method fit

| Method | Through gateway? |
|---|---|
| UPI, Card, Net Banking | Yes — single online gateway typically exposes all three |
| Pay Later | Often same gateway BNPL or separate provider adapter |
| COD | **No** online charge; `PaymentService` records `COD_PENDING` only |

---

## 17. Payment security

| Rule | Detail |
|---|---|
| Backend creates payment | Frontend never invents payment IDs as success |
| Frontend cannot declare paid | Only shows “processing”; trust webhook/verify |
| Server verifies | Signature + amount == `order.grand_total` + currency |
| Webhook auth | HMAC/signature verification; reject invalid |
| Idempotent webhooks | Dedupe event IDs |
| No card data | PCI scope stays with provider; no PAN/CVV in MA CREATIONS DB/logs |
| Amount trust | Browser-sent amounts ignored |
| Secrets | Provider keys in env only |

---

## 18. Admin order management (future V1 minimum)

**Recommended minimum when Admin Orders step runs:**

| Feature | Why |
|---|---|
| Order list (filter by status/payment/date) | Ops |
| Order detail (items snapshots, address, contacts, totals) | Support |
| Update fulfillment status | Processing → Shipped → Delivered |
| View payment status / method | Finance |
| Optional: mark COD collected | If COD_PENDING → PAID |

Defer: refunds UI, partial ship, multi-warehouse, invoice PDF (until invoice requirements confirmed).

---

## 19. Frontend checkout flow (routes only)

### 19.1 Happy path

```text
/cart
  → /checkout                          (auth optional)
      → Contact + shipping address
      → Payment method (UPI/Card/NetBanking/PayLater?/COD)
      → POST /api/checkout/preview     (show totals + issues)
      → Place order
          → if online: redirect/SDK → provider
          → if COD: confirm
  → /order-success/:orderNumber
```

### 19.2 Additional states/routes (proposed)

| Route / state | Purpose |
|---|---|
| `/checkout` | Main form |
| `/order-success/:orderNumber` | Confirmation + Order ID |
| `/order-payment-failed/:orderNumber` | Retry payment CTA |
| Cart review banner | PRICE_CHANGED / UNAVAILABLE from preview |
| Guest vs logged-in | Same `/checkout`; prefill if authenticated |

WhatsApp remains contact/alternate buy — **not** a checkout step.

---

## 20. Failure scenarios

| Scenario | Proposed behavior |
|---|---|
| Product hidden after add | Preview/place → unavailable issue; remove or block |
| Price changed | Preview reports; place rejected until re-preview |
| Invalid quantity | 400 validation |
| Empty cart | Preview/place rejected |
| Duplicate Place Order | Idempotency returns original order |
| Payment initiate failure | Order stays `PENDING_PAYMENT`; user can retry |
| Payment failure | `payment_status=FAILED`; cart retained; retry allowed per policy |
| Paid but browser closed | Webhook marks PAID/PLACED; success page recoverable via order number + auth/verifier |
| Duplicate webhook | No-op after first success |
| Invalid webhook | 401/400; no state change |
| Logout during checkout | Local form may reset; no order until place; customer cart remains server-side |
| Guest token missing | Cannot bind cart; prompt return to cart / restore token |
| COD selected | No gateway; `PLACED` + `COD_PENDING` + cod_charge from calculator |
| Shipping/tax config unavailable | Block checkout or block components per §7 decision — do not invent rates |

---

## 21. Migration plan (future — do not create yet)

Suggested sequence after V8:

| Version | Contents | Depends on |
|---|---|---|
| **V9** | `orders`, `order_items`, `order_addresses` (+ indexes, FKs) | V7 customer, V2 product |
| **V10** | `payment_transactions` (+ webhook idempotency unique keys) | V9 |
| **V11** (optional) | Cart status `CONVERTED` or order↔cart link refinements | V4/V8, V9 |
| **V12+** | `customer_address`, `shipments`, invoice tables | Only when confirmed |

V9 can ship with payment columns on `orders` even before V10 if transactions table is deferred — prefer V9+V10 together before enabling prepaid in production.

---

## 22. Test plan (no implementation yet)

### 22.1 Backend

| Case | Assert |
|---|---|
| Guest order | `customer_id` null; address snapshot; order_number set |
| Customer order | `customer_id` from JWT; ignore spoofed id |
| Price snapshot | Change product price after order; history unchanged |
| Totals | items + shipping + COD + tax math |
| Hidden product | Preview/place fails structured |
| Price change mid-checkout | Place rejected without matching preview |
| Ownership | Customer B 403 on A’s order |
| Duplicate submission | Same idempotency key → one order |
| COD | PLACED + COD_PENDING + charge calculator invoked |
| Prepaid | PENDING_PAYMENT until webhook |
| Webhook success | PAID + PLACED; amount must match |
| Webhook invalid signature | Rejected |
| Unauthorized access | No public order leak by number alone |

### 22.2 Frontend

| Case | Assert |
|---|---|
| Checkout flow | Cart → checkout → preview → place |
| Preview issues | Price/unavailable messaging |
| Payment selection | Methods shown per confirmation |
| COD | Shows extra charge from preview (when configured) |
| Errors | Empty cart, validation, payment fail |
| Order success | Displays order number |
| Guest vs auth | Prefill / token headers correct |
| Logout mid-flow | Safe handling |

---

## 23. Open decisions / blockers

| Decision | Current status | Blocks | Must decide before | Tier |
|---|---|---|---|---|
| Online payment gateway/provider | PENDING | Prepaid UPI/Card/NetBanking/Pay Later charge | Payment implementation step | **B — BLOCKS PAYMENT** |
| Pay Later as **required** checkout method | Re-confirm (§6.2 vs Step 20 list) | Whether to show Pay Later in UI | Checkout UI payment list | **C / B** if required |
| Pay Later provider | PENDING | Pay Later initiation | Pay Later enablement | **B** |
| Shipping/courier provider | PENDING | Real shipping integration | Shipping step | **C** for core order if charge can be 0/manual |
| Shipping charge rules / free-shipping threshold | PENDING | Accurate shipping_charge | Checkout totals go-live | **A** if shipping fee mandatory; else configurable 0 |
| GST/tax rules; tax-inclusive display prices? | PENDING | tax_amount / PDP truth | Tax step | **C** for core order if tax=0 allowed |
| COD extra charge amount/rule | PENDING | cod_charge | Enabling COD in production | **A** for COD path (COD is CONFIRMED required) |
| COD availability restrictions (PIN, order min) | PENDING | Eligibility checks | COD hardening | **C** |
| Guest order tracking verification | PENDING design | Secure guest GET | Guest tracking feature | **C** for place-order; **A** for guest tracking UX |
| Invoice / GST invoice requirements | PENDING | Invoice entities/PDF | Invoicing step | **C** |
| Notification channels (SMS/email) | PENDING | Notification adapters | Notify step | **C** |
| Cancellation / refund / return policy | PENDING | Cancel/refund transitions | Post-order care | **C** |
| Email required at checkout? | Soft PENDING | Validation rules | Checkout form | **C** (collect optional until decided) |
| Fail-closed vs zero when shipping unconfigured | Not decided | Preview behavior | First checkout slice | **A** (engineering policy; confirm with client) |
| Inventory/stock | Not required today | — | Only if client requests | **C** |

### Tier summary

| Tier | Meaning |
|---|---|
| **A — BLOCKS CORE ORDER IMPLEMENTATION** | Needed to accept real COD (charge rule) and/or to avoid wrong money if shipping must be charged; also guest-tracking verifier blocks *tracking*, not necessarily *create*. Engineering can still build schema + guest/customer create with feature flags. |
| **B — BLOCKS PAYMENT IMPLEMENTATION** | Gateway/provider (and Pay Later provider if offered). |
| **C — CAN BE DECIDED LATER** | Invoices, notifications, returns, courier integration, saved addresses, inventory. |

**Practical sequencing recommendation**

1. **Order core (guest + customer + snapshots + COD stub)** can start once COD charge rule is known **or** COD is feature-flagged off until rule lands.  
2. **Online payment** waits on gateway selection.  
3. **Shipping/tax** plug into calculators when rules arrive without redesigning Order tables.

---

## 24. End report

### 1. Current readiness

Cart (guest + customer + merge), customer OTP auth, published-product gating, and live cart pricing exist. **No** order/payment/checkout APIs or UI. CartPage checkout CTA is disabled. Totals today are **not** payment-safe snapshots.

### 2. Proposed entities

`orders`, `order_items` (price/title snapshots), `order_addresses` (immutable shipping), `payment_transactions`. Defer `customer_address` and inventory.

### 3. Proposed APIs

`POST /api/checkout/preview`, `POST /api/orders`, owner-scoped `GET` order(s), payment initiate + webhook, future admin order APIs. Guest tracking verifier **PENDING**.

### 4. Status models

- **Fulfillment:** `PENDING_PAYMENT` → `PLACED` → `PROCESSING` → `SHIPPED` → `DELIVERED` (+ `CANCELLED`, `PAYMENT_FAILED`).  
- **Payment:** `PENDING` / `PAID` / `FAILED` / `COD_PENDING` (+ future refunds).

### 5. Checkout total architecture

Server-authoritative: items subtotal + shipping + COD + tax − discount = grand total via pluggable calculators; frontend display-only; preview hash binds place-order.

### 6. Guest / customer flow

Guest: token cart + contact/address snapshots, `customer_id` null. Customer: JWT-derived `customer_id` + snapshots. Cart cleared/converted only after accepted order (COD place or prepaid paid).

### 7. Payment abstraction

`PaymentGateway` / `PaymentService` interfaces; COD bypasses gateway; no provider-specific domain types until selection.

### 8. Migration sequence

V9 Order core → V10 Payment transactions → optional V11 cart `CONVERTED` / later address & shipment tables.

### 9. Testing strategy

Backend: guest/customer ownership, snapshots, stale price, idempotency, COD vs prepaid, webhook security. Frontend: flow, preview issues, payment method, success/failure.

### 10. Exact remaining business decisions

Gateway; Pay Later required? + provider; shipping provider & charge rules; GST/tax & inclusive pricing; **COD charge rule**; COD restrictions; guest tracking verification; invoices; notifications; cancel/refund/return; email required?; fail-closed shipping policy.

---

**Reminder:** Checkout and Orders are **not implemented**. This file is analysis only.

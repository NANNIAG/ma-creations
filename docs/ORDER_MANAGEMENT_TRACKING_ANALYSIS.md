# MA CREATIONS — Order Management & Tracking Analysis

**Step:** 26 analysis → **26A implementation (partial)**  
**Date context:** After Steps 1–25 (catalog, cart, wishlist, auth, checkout, place-order, Razorpay, V1 shipping/COD/GST rules).  
**Status of this document:** Design capture + **Step 26A implementation status**. Tracking / guest / cancel remain PENDING.

**Sources inspected (read-only for analysis; 26A implemented against this design):**

| Source | Notes |
|---|---|
| `docs/REQUIREMENTS.md`, `CLIENT_CONFIRMATIONS.md`, `API_DESIGN.md` | Business scope |
| `docs/CHECKOUT_ORDER_ANALYSIS.md`, `SHIPPING_TAX_COD_ANALYSIS.md` | Order/payment/charge architecture |
| `Order`, `OrderItem`, `OrderAddress`, `OrderStatus`, `PaymentStatus` | Domain model |
| `OrderService`, `OrderPlacementService`, `OrderController` | Current APIs |
| `PaymentService` / Razorpay webhook | Payment ↔ order updates |
| `SecurityConfig`, customer/admin JWT patterns | Authz |
| Frontend `AppRoutes`, `CustomerAccountPage`, admin pages | UI gaps |
| Flyway V1–V10 | Schema readiness |

**Legend**

| Tag | Meaning |
|---|---|
| **CONFIRMED** | Client business requirement |
| **IMPLEMENTED** | Exists in code today |
| **PENDING** | Known need; client or design answer missing — do not invent |
| **NOT IMPLEMENTED** | Required or useful later; not built |
| **PROPOSED** | Recommended design for a future implementation step |

---

## Step 26A status (this slice)

| Capability | Status |
|---|---|
| Customer order history list API + UI | **IMPLEMENTED** |
| Customer order detail API + UI | **IMPLEMENTED** |
| Admin order list API + UI | **IMPLEMENTED** |
| Admin order detail API + UI | **IMPLEMENTED** |
| Admin order status update | **IMPLEMENTED** (fulfillment transitions only) |
| Shipping provider | **PENDING** |
| Tracking provider / number / URL | AWB number **IMPLEMENTED** (26B); provider/URL **PENDING** |
| Guest order tracking | **IMPLEMENTED** (26B — order number + mobile) |
| Guest verification | **IMPLEMENTED** (order number + mobile) |
| Cancellation / refund / return | **PENDING** |
| COD collected → PAID | **PENDING** |
| Invoices | **PENDING** |
| Notifications | **PENDING** |

No Flyway migration added in 26A (V9/V10 reused).

---

## 1. Current order capabilities

### 1.1 What works today (**IMPLEMENTED**)

| Capability | Evidence |
|---|---|
| Order creation | `POST /api/orders` via `OrderPlacementService` |
| Server-generated order number | `OrderNumberGenerator` / `orders.order_number` |
| Guest order (`customer_id` NULL) | Place-order + contact/guest fields |
| Registered customer order | `customer_id` from CUSTOMER JWT only |
| Order item snapshots | Title, slug, unit selling, MRP, qty, line subtotal |
| Address snapshot | `order_addresses` |
| Money snapshots | shipping ₹20, COD ₹20 if COD, tax ₹0 |
| Payment / order status enums | As in V9 |
| Razorpay + COD placement | Unchanged by 26A |
| Customer order history | `GET /api/customer/orders` + `/account/orders` |
| Customer order detail | `GET /api/customer/orders/{orderNumber}` + detail page |
| Admin order list / detail / status | `/api/admin/orders` (+ PATCH status) + admin UI |

### 1.2 Caveats

| Item | Caveat |
|---|---|
| `GET /api/orders/{orderNumber}` | Confirmation summary only — not guest tracking |
| Tracking | Status labels only; no courier/AWB/URL |

### 1.3 Still PENDING / NOT IMPLEMENTED

Tracking fields, guest tracking/verification, cancel/refund/return, invoices, notifications, shipping provider, COD collected → PAID.

---

## 2. Customer order history (**IMPLEMENTED** Step 26A)

| Method | Path | Auth |
|---|---|---|
| GET | `/api/customer/orders` | CUSTOMER JWT |
| GET | `/api/customer/orders/{orderNumber}` | CUSTOMER JWT + ownership |

Ownership from JWT only. Snapshots only (never live Product price/title).

UI: `/account/orders`, `/account/orders/:orderNumber`.

---

## 3. Admin order management (**IMPLEMENTED** Step 26A)

| Method | Path | Auth |
|---|---|---|
| GET | `/api/admin/orders` | ADMIN JWT |
| GET | `/api/admin/orders/{orderNumber}` | ADMIN JWT |
| PATCH | `/api/admin/orders/{orderNumber}/status` | ADMIN JWT |

Filters: `q` (order number / mobile), `status`, `paymentStatus`. Newest first. Pagination.

Status transitions: `PLACED` → `PROCESSING` → `SHIPPED` → `DELIVERED`.  
Rejected: cancel invent, payment-driven statuses as admin targets. **PaymentStatus never set by this API.**

UI: `/admin/orders`, `/admin/orders/:orderNumber`.

Safe payment summary only (provider refs/status/amount) — never Razorpay secrets or card data.

---

## 16. Decision table (updated)

| Decision | Current status |
|---|---|
| Registered customer order history | **IMPLEMENTED** (26A) |
| Registered customer order tracking display | data/fields **PENDING** |
| Guest order tracking | mechanism **PENDING** |
| Guest verification | **PENDING** |
| Shipping / tracking provider | **PENDING** |
| Cancellation / refund / return | **PENDING** |
| Admin mark COD collected | **PENDING** |
| Admin fulfillment status matrix | **IMPLEMENTED** |

---

*End of Step 26 / 26A tracking analysis update.*

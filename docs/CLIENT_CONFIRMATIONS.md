# MA CREATIONS — Client Confirmations

Items the PDF does **not** decide, plus **FINAL client decisions** from the requirement-clarification phase.

**Legend**

| Status | Meaning |
|---|---|
| **CONFIRMED** | Final client decision. Treat as approved business scope for the **next implementation phase**. Not necessarily built yet. |
| **PENDING** | Still needs a client or provider answer. Do **not** invent a default in code or docs. |
| **CURRENT V1** | What the codebase already implements today (catalog + admin CMS). Separate from FINAL scope. |

**Do not invent:** Pay Later BNPL brand name, shipping-provider/courier details (integration deferred), Shop Now destination, or bestseller/featured selection rules. Customer auth (Mobile + OTP), guest+customer cart/wishlist merge, **order core + checkout preview (V9)**, **Razorpay payment foundation (V10)**, **place-order + checkout UI (Step 24)**, **V1 flat ₹20 shipping + ₹20 COD fee + GST not charged (Step 25)**, **customer + admin order management (Step 26A)**, and **manual AWB tracking + guest track via order number + mobile (Step 26B)** are **IMPLEMENTED**. Remaining PENDING: shipping provider/courier API, tracking URL, cancellation/refund/return, COD collected → PAID, invoices, notifications, Pay Later merchant rail.

**V1 tracking is manual and stores only a tracking/AWB number. Courier integration is intentionally deferred.**
Guest verification for tracking: **order number + mobile number** (**IMPLEMENTED** 26B).

---

## Status snapshot (FINAL vs CURRENT)

| Topic | FINAL business scope | CURRENT V1 build |
|---|---|---|
| Selling model | Website shopping + checkout + online payment; WhatsApp = contact / alternate buy | Catalog + WhatsApp CTAs; no checkout |
| Search | Required | Header icon only (disabled / no results) |
| Wishlist | Required (add/remove) | Header icon only |
| Cart | Real persistent cart required | Add to Cart UI-only notice |
| Online payment methods | **REQUIRED:** UPI, Credit/Debit Card, Net Banking (via selected gateway) | Not implemented |
| Cash on Delivery (COD) | **REQUIRED**; **₹20** additional charge (**IMPLEMENTED** Step 25) | Charge applied only for COD |
| Payment gateway/provider | **CONFIRMED** — **Razorpay** (Step 23) | V10 + initiate/verify/webhook foundation |
| Pay Later | **REQUIRED** checkout method; Razorpay Pay Later/BNPL **merchant enablement / rail PENDING** | Enum + preview; gated by `RAZORPAY_PAY_LATER_ENABLED` |
| Additional payment methods | May be added later if the client requests them | — |
| Guest checkout | **CONFIRMED** — order without account; provide name, mobile, email, address; receive Order ID; track via order number + mobile | Guest tracking **IMPLEMENTED** (26B) |
| Customer accounts | **CONFIRMED** — optional register/login via **mobile OTP**; order history (**IMPLEMENTED** 26A); order tracking AWB display (**IMPLEMENTED** 26B); wishlist + saved info where applicable | Auth APIs **IMPLEMENTED** (OTP provider PENDING) |
| Customer auth mechanism (OTP/password/etc.) | **CONFIRMED** — Mobile + OTP | Real SMS provider still **PENDING** |
| Guest order-tracking mechanism | **CONFIRMED** — order number + mobile | **IMPLEMENTED** (26B) |
| Product visibility | Hide/Unpublish (not permanent delete in normal admin flow) | **IMPLEMENTED** (`published`) |
| Product images | Admin Dashboard upload/manage | Admin upload (local disk) |
| WhatsApp number | Client-provided `6395700831` | Configured via `VITE_WHATSAPP_NUMBER` (local env) |
| Instagram | `https://www.instagram.com/macreations.living/` | Configured via env |
| Shop Now destination | **PENDING** | Temporary default `#categories` |
| Bestsellers / Featured rules | **PENDING** | Newest products fallback |

---

## 1. Selling model — CONFIRMED

| # | Decision |
|---|---|
| 1.1 | **CONFIRMED:** v1 FINAL scope is **web checkout** — customers place orders through the website with a complete shopping and checkout flow and **online payment**. |
| 1.2 | **CONFIRMED:** Primary conversion is the website cart/checkout path. **WhatsApp** remains available as **contact / alternate buying**, not the primary order-placement flow. |
| 1.3 | **CONFIRMED:** Add to Cart must become a real cart action (replacing CURRENT V1 UI-only behavior). Cart page and persistence are required (see §8). |

**Still PENDING:** shipping **provider/courier**, invoices format, notifications (see §9). Guest + account checkout are **CONFIRMED**. V1 shipping **rate** (₹20 flat) and COD **fee** (₹20) and **GST not charged** are **IMPLEMENTED** (Step 25).

---

## 2. WhatsApp — PARTIALLY CONFIRMED

| # | Status | Answer / note |
|---|---|---|
| 2.1 | **CONFIRMED** | Client-provided WhatsApp Business number: **`6395700831`**. Do not invent or change this number in documentation. If international formatting (e.g. country code `91`) is required for `wa.me` links, treat that as a **configuration / verification** item — do not silently rewrite the documented client number. |
| 2.2 | **PENDING** | Pre-filled message for **Buy via WhatsApp** (if any). |
| 2.3 | **CONFIRMED (intent)** | WhatsApp is **not** the primary order-placement flow. Whether Buy via WhatsApp also creates a website order record remains **PENDING**. |
| 2.4 | **PENDING** | Who answers chat, and during what hours. |

---

## 3. Instagram / Facebook — PARTIALLY CONFIRMED

| # | Status | Answer / note |
|---|---|---|
| 3.1 | **CONFIRMED** | Official profile: **[macreations.living](https://www.instagram.com/macreations.living/)** — clean URL only (no tracking/query parameters). Do **not** use `@she_lift__heavy`. |
| 3.2 | **PENDING** | Live grid via embed vs Instagram Graph API (Meta app + token). |
| 3.3 | **CONFIRMED** | Follow URL: `https://www.instagram.com/macreations.living/` |
| 3.4 | **PENDING** | Tapping a grid post: open Instagram vs site product. |
| 3.5 | **PENDING** | Facebook on-site UI or pixel (traffic source only in PDF). |

---

## 4. Brand assets and UI tokens

| # | Status | Notes |
|---|---|---|
| 4.1 | **PENDING** | Font: Inter or Montserrat. |
| 4.2 | **PARTIAL** | Client logo provided for storefront; confirm final production asset if needed. |
| 4.3 | **PENDING** | Favicon and social share image. |
| 4.4 | **PARTIAL / PENDING** | Hero/category creatives provided. **Shop Now final destination remains PENDING** — do not assume all products, a category, or a specific product. |
| 4.5 | **PARTIAL** | Category promotional banners provided for all 5 categories. |
| 4.6 | **PENDING** | Footer quick-link list and destinations. |
| 4.7 | **PENDING** | Return policy content. |
| 4.8 | **CONFIRMED (methods) / PARTIAL** | Checkout supports **UPI**, **Card**, **Net Banking**, **Pay Later**, **COD**. Gateway: **Razorpay**. COD fee **₹20 IMPLEMENTED**. Flat shipping **₹20 IMPLEMENTED**. Footer trust-badge image set remains **PENDING**. |

| 4.9 | **PENDING** | Admin UI theme (cream/terracotta vs plain functional). |

---

## 5. Catalog completeness

| # | Status | Notes |
|---|---|---|
| 5.1–5.9 | **PENDING** | Full SKU list, descriptions, variants, inventory, etc. |
| 5.10 | **PENDING** | How **Bestsellers** and **Featured** are chosen (manual flags, sales, newest, or other). Do not assume a rule. |
| 5.11–5.12 | **PENDING** | Sub-category navigation; one category per product (PDF dropdown implies one). |

**Product images (CONFIRMED approach):** Images are uploaded/managed by the client through the **Admin Dashboard**. Do **not** create fake/static product images for the catalog. Existing Admin Add/Edit Product image upload remains the intended approach.

---

## 6. Pricing display vs real payment — PARTIALLY CONFIRMED

| # | Status | Answer / note |
|---|---|---|
| 6.1 | **CONFIRMED REQUIRED** | **UPI** — real payment method at checkout (not a UI badge only). Processed through the selected gateway once chosen. |
| 6.1a | **CONFIRMED REQUIRED** | **Credit/Debit Card** — real payment method at checkout. |
| 6.1b | **CONFIRMED REQUIRED** | **Net Banking** — real payment method at checkout. |
| 6.1c | **CONFIRMED REQUIRED + IMPLEMENTED fee** | **Cash on Delivery (COD)** — required. Additional charge **₹20** (flat) when payment method = COD (Step 25). |
| 6.1d | **CONFIRMED** | **Payment gateway/provider** — **Razorpay** selected (Step 23). Use test/sandbox credentials in local; never commit secrets. |
| 6.1e | **CONFIRMED (policy)** | **Additional payment methods** may be added later if the client requests them. |
| 6.2 | **CONFIRMED REQUIRED** | **Pay Later** is a required checkout payment method. Razorpay is the online gateway; **Pay Later/BNPL merchant enablement and specific rail remain PENDING** — do not invent a BNPL provider name. |
| 6.3 | **PENDING** | Pay Later provider name and how any lower displayed price is calculated (PDF PDP tag). |
| 6.4 | **CONFIRMED + IMPLEMENTED** | Product prices are **GST-inclusive**. GST is **not charged** separately by the application (`taxAmount = 0`). |
| 6.5 | **PENDING** | Coupons / bank offers beyond MRP vs selling price. |
| 6.6 | **CONFIRMED + IMPLEMENTED** | **COD charge** = flat **₹20** when method = COD; otherwise ₹0. |

---

## 7. Customer accounts & authentication — PARTIALLY CONFIRMED

| # | Status | Notes |
|---|---|---|
| 7.1 | **CONFIRMED** | The site supports **both Guest Checkout and optional Customer Accounts** (not guest-only, not account-only). |
| 7.1a | **CONFIRMED** | **Guest Checkout:** customers can place an order **without** creating an account. They provide required checkout info (e.g. name, mobile number, email, delivery address). After successful payment/order creation they receive an **Order ID**. Guests must be able to **track** the order via an order-information / verification flow — **mechanism PENDING design** (do not invent OTP/link/magic details yet). |
| 7.1b | **CONFIRMED** | **Customer Accounts:** customers may **optionally** create an account and log in; place orders while logged in; view **order history**; **track orders**; use account-based features such as **wishlist** and **saved customer information** where applicable. |
| 7.2 | **CONFIRMED + IMPLEMENTED** | Authentication: **Mobile Number + OTP** (no password). Email is not the login identifier. APIs: `/api/customer/auth/*`. Real SMS provider **PENDING**. |
| 7.3 | **PARTIAL** | Admin login exists in CURRENT V1 (email used as temporary identifier). Final admin identifier shape may still need confirmation. |
| 7.4 | **PENDING** | Multiple admin roles. |

Wishlist and cart remain **CONFIRMED required** (§8). Guest + customer ownership and guest→customer merge are **IMPLEMENTED**.

---

## 8. Cart and wishlist — CONFIRMED (required)

| # | Status | Answer / note |
|---|---|---|
| 8.1 | **CONFIRMED** | Cart page / shopping flow is part of FINAL scope. |
| 8.2 | **CONFIRMED + IMPLEMENTED** | Real persistent cart (guest token + customer JWT). Guest→customer merge via `POST /api/cart/merge`. |
| 8.3 | **PENDING** | Exact quantity-change / remove UX details (required capability implied by a real cart; wireframe TBD). |
| 8.4 | **CONFIRMED + IMPLEMENTED** | Wishlist add/remove for guest and customer; merge via `POST /api/wishlist/merge`. |

---

## 9. Checkout, orders, shipping, payment — PARTIALLY CONFIRMED

| # | Status | Answer / note |
|---|---|---|
| 9.0 | **CONFIRMED** | Website has a **complete shopping and checkout flow** and **online payment**. Customers place orders through the website. |
| 9.1 | **CONFIRMED** | **Guest checkout is required** (see §7.1a). Collect name, mobile, email, delivery address; issue **Order ID** after successful payment/order creation; guest order tracking via a later-designed verification flow (**PENDING design**). |
| 9.1a | **CONFIRMED** | **Registered-account checkout** is also required (see §7.1b): order placement, order history, order tracking. |
| 9.2 | **CONFIRMED methods / Razorpay selected** | **Required methods:** UPI, Credit/Debit Card, Net Banking, **Pay Later**, and COD. **Gateway:** Razorpay (Step 23). Pay Later merchant/rail remain **PENDING**. COD fee **₹20 IMPLEMENTED**. |
| 9.3 | **CONFIRMED + IMPLEMENTED fee** | **Cash on Delivery is REQUIRED.** COD additional charge = **₹20** (Step 25). |
| 9.4 | **PARTIAL** | V1 shipping **rate** = flat **₹20** (**IMPLEMENTED**). Shipping **provider**, PIN zones, free-shipping threshold: **PENDING**. |
| 9.5 | **PENDING** | Who packs and ships? |
| 9.6 | **PARTIAL** | **Order ID** required. Invoice format details remain **PENDING** (GST not charged on site). |
| 9.7 | **PENDING** | Email or SMS notifications. |
| 9.8 | **PENDING** | Cancellation / return process. |

---

## 10. Reviews — PENDING

| # | Status | Notes |
|---|---|---|
| 10.1–10.3 | **PENDING** | Display-only vs on-site reviews; write access; moderation. PDP rating **display** remains PDF-explicit. |

---

## 11. Search and PLP extras — PARTIALLY CONFIRMED

| # | Status | Answer / note |
|---|---|---|
| 11.1 | **CONFIRMED** | Product search is **required**. Customers must be able to search products from the storefront. Results UX (page layout, autocomplete) remains **PENDING** detail. |
| 11.2–11.5 | **PENDING** | Extra PLP filters, Gender ignore, column count, pagination vs infinite scroll. |

---

## 12. CMS beyond Add Product — PARTIALLY CONFIRMED

| # | Status | Answer / note |
|---|---|---|
| 12.1 | **CONFIRMED** | Product **list** and **edit** are required (already in CURRENT V1). **Hide/Unpublish** is the normal visibility control: a hidden product must **not** appear on the customer storefront, but must remain available in the Admin Dashboard to publish again. Products should **not** be permanently deleted from the normal admin workflow. |
| 12.2 | **PARTIAL** | Multiple images supported in CURRENT V1; max count **PENDING**. |
| 12.3–12.6 | **PENDING** | Hero CMS, settings CMS, dashboard widgets, 6th category. |

**IMPLEMENTED:** Admin Hide/Publish via `PATCH /api/admin/products/{id}/status` and `product.published` (V6). Hard delete removed from the normal Admin API/UI.

---

## 13–15. Legal, hosting, screenshot-only items

Unchanged in substance: still largely **PENDING** unless separately answered. Screenshot-only Meesho chrome defaults in §15 remain “do not implement” until told otherwise. Header wishlist and cart badge remain specified; they now map to **CONFIRMED** required features (§8, §11).

---

## 16. How to use this list

1. Treat **CONFIRMED** rows as approved **FINAL business scope** for planning the next implementation phase.
2. Keep **CURRENT V1** behavior documented separately in implementation docs (`*_IMPLEMENTATION.md`, `GO_LIVE_CHECKLIST.md`) until that phase ships.
3. Do **not** invent PENDING providers, schemas, or UX details.
4. Remaining **blocking for next commerce phase:** payment gateway/provider (online methods confirmed: UPI, Card, Net Banking); **COD charge amount/rule**; cart/wishlist technical storage; shipping; Shop Now destination; bestseller/featured rules; international WhatsApp formatting verification; **design** of customer auth mechanism and guest order-tracking flow (requirements CONFIRMED — mechanisms PENDING); Pay Later provider if Pay Later is included as a checkout option.

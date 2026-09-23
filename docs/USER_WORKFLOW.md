# MA CREATIONS — User Workflows

Customer journeys are written for **social traffic** (Instagram, Facebook, WhatsApp), which the PDF names as the conversion sources.

**Legend**

- Solid steps are **PDF-explicit** (Screens 1–3, WhatsApp, Instagram) and/or **CONFIRMED FINAL** business scope.
- Steps marked **PENDING** still need client/provider detail — do not invent them.
- **CURRENT V1** may not yet implement CONFIRMED FINAL steps (e.g. checkout). See `CLIENT_CONFIRMATIONS.md`.

**FINAL selling model (CONFIRMED):** customers place orders through the website (cart → checkout → online payment). WhatsApp is **contact / alternate buy**, not the primary order-placement flow.

---

## 1. Customer journey (CONFIRMED path)

```
Social Media → Homepage → Category → Product → Cart → Checkout → Order Confirmation
```

### 1.1 Social Media → Homepage

1. Customer taps a link or bio URL from Instagram, Facebook, or WhatsApp.
2. Site opens **mobile-first** on cream background `#FDFBF7`.
3. **Homepage (Screen 1)**
   - Header: `MA CREATIONS` logo, Search, Wishlist, Cart badge
   - Hero carousel
   - Customer taps terracotta **Shop Now** (`#D4A373`)
4. **PENDING:** Shop Now target (first category, all products, or a campaign PLP). Until confirmed, treat it as “enter shop” only (CURRENT V1 may use a temporary `#categories` default).

Alternate homepage actions:

- Tap one of **5 category tiles** → PLP for that category
- Tap **Add to Cart** on a Bestseller/Featured card → cart updates (**FINAL:** persistent cart; **CURRENT V1:** UI-only notice)
- Scroll **Instagram** section → **Follow on Instagram** → `https://www.instagram.com/macreations.living/`
- Use **floating WhatsApp widget** → inquiry chat (not primary purchase)

### 1.2 Homepage → Category (PLP)

1. Customer selects a tile among the 5 PDF categories.
2. **PLP (Screen 2)** shows a responsive product grid (only **published** products — FINAL hide/unpublish).
3. Customer may sort: Price Low→High, High→Low, Newest.
4. Each card shows photo, title, selling price, MRP, % off, UPI badge (PDF visual language; **UPI** is a **required** real checkout method — FINAL; gateway PENDING).
5. **CONFIRMED:** Search from header is required (results UX **PENDING**). Extra filters / Gender remain PENDING / out of Meesho chrome.

### 1.3 Category → Product (PDP)

1. Customer opens a product.
2. **PDP (Screen 3):** gallery, title, rating display, pricing + **with Pay Later** tag (PDF visual; Pay Later provider PENDING if offered as checkout). Checkout required methods: **UPI**, **Credit/Debit Card**, **Net Banking**, **COD**.
3. Customer chooses:
   - **Add to Cart** (primary website path), or
   - **Buy via WhatsApp** (secondary / alternate; see Section 2)

### 1.4 Product → Cart — CONFIRMED FINAL

PDF-explicit: Add to Cart + header cart badge.

**CONFIRMED / IMPLEMENTED:** real persistent cart (guest + logged-in customer) and guest→customer merge on login. Checkout preview (`POST /api/checkout/preview`), place-order (`POST /api/orders`), `/checkout` UI, Razorpay Checkout wiring, COD placement, and order success/failure pages are **IMPLEMENTED** (Step 24). Guest order tracking and account order history remain PENDING.

Typical steps (wireframe TBD):

1. Customer opens the cart.
2. Sees line items; can change qty / remove (**PENDING** exact UX).
3. Proceeds to checkout.

### 1.5 Cart → Checkout → Order Confirmation — CONFIRMED FINAL

| Step | What happens | Status |
|---|---|---|
| Checkout | Contact, address, payment method selection | **CONFIRMED required**; guest **or** logged-in |
| Payment | Online: **UPI**, **Credit/Debit Card**, **Net Banking**; plus **COD** | **CONFIRMED REQUIRED**; gateway/provider **PENDING**; COD charge rule **PENDING** |
| Order confirmation | Order ID, summary, next steps | **CONFIRMED required**; Order ID required for guest and registered |
| Guest checkout | No account; name, mobile, email, address; Order ID; track via later-designed verification | **CONFIRMED**; tracking mechanism **PENDING design** |
| Customer accounts | Optional mobile OTP login; order history later | **IMPLEMENTED** (OTP provider PENDING) |
| COD | Required payment option; additional charge | **CONFIRMED**; charge amount/rule **PENDING** |

**CURRENT V1:** this branch is **not implemented yet**. It is approved FINAL scope for the next commerce phase.

### 1.6 Other storefront actions

| Action | Start | End |
|---|---|---|
| Search | Header | **CONFIRMED** product search (results UX PENDING) |
| Wishlist | Header | **CONFIRMED** add/remove (page/storage PENDING) |
| Return policy | Footer | Content PENDING |
| Quick links | Footer | Targets PENDING |
| Payment trust badges | Footer | Badge set PENDING; required methods UPI / Card / Net Banking / COD CONFIRMED |

---

## 2. WhatsApp flows (PDF-explicit; alternate to web checkout)

### 2.1 Inquiry (floating widget)

```
Any page → floating WhatsApp widget → WhatsApp chat with the business
```

- Purpose: instant customer inquiries.
- **CONFIRMED number:** `6395700831` (intl formatting for links = config verification if needed).
- **PENDING:** greeting template, business hours.

### 2.2 Buy via WhatsApp (PDP) — alternate path

```
PDP → Buy via WhatsApp → WhatsApp with product context → seller assists
```

**CONFIRMED:** this is **not** the primary order-placement flow. Website checkout is primary.

**PENDING:** pre-filled message; whether a website order record is also created; payment-in-chat vs on-site.

### 2.3 WhatsApp as traffic source

Customer follows a catalog/link → Homepage or PDP → continues with Section 1 (primary) or 2.2 (alternate).

---

## 3. Instagram and Facebook flows

### 3.1 Instagram

```
Homepage → Instagram section → Follow on Instagram
```

**CONFIRMED profile:** `https://www.instagram.com/macreations.living/`

Live grid tech (embed vs Graph API) and post-tap behavior remain **PENDING**. Do not use `@she_lift__heavy`.

### 3.2 Facebook

Named as traffic source only. No on-site Facebook widget is specified.

---

## 4. Admin / CMS workflow

### 4.1 Login (PDF-explicit)

```
Admin opens CMS URL → logs in → Dashboard Overview
```

Dashboard widgets are **not** specified (PENDING).

### 4.2 Add / edit product

```
Dashboard → Add or Edit Product
  → Title, Category (5), Image upload, Selling Price, MRP → Save
```

**CONFIRMED:** images managed via Admin upload only — no fake catalog product images.

After save, published products appear on matching PLP. Homepage Bestsellers & Featured membership rules remain **PENDING**.

### 4.3 Visibility — CONFIRMED FINAL

| Flow | Status |
|---|---|
| Product list / edit | **CONFIRMED** (CURRENT V1 also has these) |
| **Hide / Unpublish** | **CONFIRMED** normal workflow — hidden off storefront, kept in Admin to publish again |
| Permanent delete | **Not** the normal admin workflow (FINAL) |
| CURRENT V1 hard delete | Exists today; to be superseded by hide/unpublish in a future phase |
| Replace images | Supported via edit (CURRENT V1) |
| Reorder homepage featured | PENDING (rules PENDING) |
| Upload hero carousel | PENDING CMS (assets can be env/static today) |
| Change WhatsApp / Instagram via CMS | PENDING (values CONFIRMED for env) |
| View or update orders | Required once checkout ships; UI PENDING |
| Moderate reviews | PENDING |
| Edit return policy | PENDING |

---

## 5. Role summary

| Role | Work |
|---|---|
| Shopper | Browse; search; wishlist; persistent cart; **guest or account** checkout + online payment; order history/tracking (account) or guest tracking (PENDING design); WhatsApp inquire/alternate buy; Instagram follow |
| Store owner | Log in; add/edit products; hide/unpublish; manage catalog images |
| Developer | Not a runtime role; CMS must not require the owner to code |

---

## 6. Journey status (explicit)

| Path | FINAL | CURRENT V1 |
|---|---|---|
| Browse Home / PLP / PDP | Yes | Implemented |
| Website cart → checkout → paid order | **CONFIRMED** (guest **and** optional account) | Not implemented |
| WhatsApp as primary checkout | **No** | WhatsApp CTAs exist as contact/alternate |
| Search / Wishlist | **CONFIRMED** | Icons only / not functional |
| Hide/Unpublish | **CONFIRMED** | Hard delete only |

Former A/B/C selling-model fork is **closed**: client chose website checkout + online payment; WhatsApp remains contact/alternate.

# MA CREATIONS — System Architecture

This architecture supports a **small-to-medium custom e-commerce website**. It uses only the stack the client named, plus the external channels the PDF names (WhatsApp, Instagram). It does not add microservices, message queues, search engines, or cache layers.

**Related docs:** `REQUIREMENTS.md`, `DATABASE_DESIGN.md`, `API_DESIGN.md`, `USER_WORKFLOW.md`, `CLIENT_CONFIRMATIONS.md`.

**Scope layers**

| Layer | Meaning |
|---|---|
| **CURRENT V1** | Catalog storefront + admin CMS (as built) |
| **FINAL confirmed** | Website checkout + online payment (**UPI**, **Credit/Debit Card**, **Net Banking**) + **COD**; search; wishlist; persistent cart; hide/unpublish — **not all implemented yet**. Gateway/provider **PENDING**; COD charge rule **PENDING**. |
| **PENDING** | Gateway/provider names, **COD charge rule**, shipping, **auth/tracking mechanism design** (guest+accounts CONFIRMED), cart/wishlist storage strategy, Shop Now destination, bestseller rules |

---

## 1. Architecture goals

- Match the PDF: mobile-first storefront, 5-category catalog, cart/wishlist entry points, WhatsApp contact/alternate buy, Instagram proof, simple admin.
- Keep one backend and one database.
- Do not implement Shopify or WordPress. The PDF uses those names as examples of a **simple, non-technical admin**. The custom admin must feel that simple.
- **FINAL confirmed:** attach payment, checkout, orders, search, wishlist, and persistent cart in a later phase. Leave **provider selection and storage strategy PENDING** — show **where** they attach, do not invent integrations.
- **CURRENT V1:** payment/checkout remain unwired until that phase ships.

---

## 2. System context

```
Instagram / Facebook / WhatsApp traffic
                 |
                 v
        React storefront (browser)
                 |
                 | HTTPS JSON REST
                 v
        Spring Boot API
          |            |
          |            +--> WhatsApp (wa.me / click-to-chat; contact / alternate buy)
          |            +--> Instagram (Follow URL confirmed; live grid embed/API PENDING)
          |            +--> Payment provider (**CONFIRMED needed**; gateway name PENDING)
          v
        MySQL
```

Two user applications share one API:

| Application | Who | Purpose |
|---|---|---|
| Customer storefront | Shoppers from social media | Browse; **FINAL:** search, wishlist, cart, checkout, online payment; WhatsApp inquire/alternate buy |
| Admin CMS | Store owner | Log in without coding; add/edit products; **FINAL:** hide/unpublish |

For this size of site, both UIs live in **one React app** with separate route groups (`/` vs `/admin`). Two repos or two deploys are unnecessary unless the client later requires them.

---

## 3. React frontend architecture

### 3.1 Technologies (client-stated)

| Library | Role |
|---|---|
| React.js | UI |
| Tailwind CSS | Styling against the PDF tokens (`#FDFBF7`, `#D4A373`, `#2C2C2C`; Inter or Montserrat) |
| React Router | Storefront and admin routes |
| Redux Toolkit | Use only for state that crosses many pages: cart badge, wishlist, and customer auth session when accounts are implemented |

Do **not** add Next.js, MUI, Bootstrap, or extra CSS frameworks. The PDF already defines the visual system.

### 3.2 Suggested folder shape (logical, not created yet)

```
src/
  app/                 # router, store, providers
  styles/              # Tailwind tokens from the PDF style guide
  layouts/
    StorefrontLayout   # header, footer, WhatsApp widget
    AdminLayout        # simple CMS shell
  pages/
    HomePage
    CategoryPage       # PLP
    ProductPage        # PDP
    SearchPage         # CONFIRMED FINAL (UX PENDING); not CURRENT V1
    WishlistPage       # CONFIRMED FINAL (storage PENDING); not CURRENT V1
    CartPage           # CONFIRMED FINAL (persistent cart); not CURRENT V1
    CheckoutPage       # CONFIRMED FINAL; not CURRENT V1
    OrderConfirmation  # CONFIRMED FINAL; not CURRENT V1
    admin/
      LoginPage
      DashboardPage    # overview widgets unspecified
      AddProductPage
  components/
    header/            # logo, search icon, wishlist icon, cart badge
    hero/              # carousel + Shop Now
    categoryTiles/
    productCard/       # photo, title, price, MRP, % off, UPI badge, Add to Cart
    instagramGrid/
    whatsAppWidget/
    pdp/               # gallery, zoom, sticky CTA, Buy via WhatsApp
  services/            # REST client (fetch/axios)
  store/               # Redux slices if used
```

### 3.3 Rendering and routing (storefront)

PDF-specified routes:

| Route (proposed path) | Screen | PDF |
|---|---|---|
| `/` | Homepage | Screen 1 |
| `/category/:slug` | PLP | Screen 2 |
| `/product/:slug` | PDP | Screen 3 |

Proposed only because the header icons exist; pages are not wireframed:

| Route | Reason | Status |
|---|---|---|
| `/search` | Search icon | **CONFIRMED FINAL** (not CURRENT V1) |
| `/wishlist` | Wishlist icon | **CONFIRMED FINAL** (not CURRENT V1) |
| `/cart` | Cart badge / Add to Cart | **CONFIRMED FINAL** persistent cart (not CURRENT V1) |
| `/checkout` | Cart → pay (guest or logged-in) | **CONFIRMED FINAL** (not CURRENT V1) |
| `/order-confirmation` | After payment (Order ID) | **CONFIRMED FINAL** (not CURRENT V1) |
| `/account/*` / login-register | Optional customer accounts | **CONFIRMED FINAL**; auth UX PENDING design |
| `/orders/track` (or equivalent) | Guest + account order tracking | **CONFIRMED need**; verification flow PENDING design |

Admin:

| Route | Screen | PDF |
|---|---|---|
| `/admin/login` | Admin login | Screen 4 |
| `/admin` | Dashboard overview | Named, contents unspecified |
| `/admin/products/new` | Add New Product | Screen 4 |

### 3.4 Client state

| State | Recommendation |
|---|---|
| Catalog, PLP, PDP | Server data via REST; local component/page state |
| Cart count badge | Shared client state and/or API — **FINAL** requires persistent cart (storage strategy PENDING) |
| Wishlist icon state | Same — **FINAL** wishlist required |
| Admin session | HTTP-only cookie from Spring Security, not localStorage tokens if avoidable |
| Instagram posts | Fetch from backend proxy or official embed; do not call Instagram from the browser with secrets |

### 3.5 Responsive behavior (PDF-explicit)

- Mobile-first layouts.
- Homepage featured grid is **2-column**.
- Category tiles: horizontal scroll **or** grid.
- PDP: sticky **bottom** CTA bar (mobile).
- Desktop must still work; the PDF says Mobile & Desktop for the homepage.

---

## 4. Spring Boot backend architecture

### 4.1 Technologies (client-stated)

| Library | Role |
|---|---|
| Java + Spring Boot | REST API |
| Spring Data JPA / Hibernate | Persistence |
| Spring Security | Admin login (PDF-explicit). Customer auth only if confirmed |
| REST JSON | Frontend communication |
| MySQL | System of record |

Do **not** add Kafka, Redis, Elasticsearch, GraphQL, or a second “BFF” service.

### 4.2 Suggested module shape (logical, not created yet)

Single deployable Spring Boot application:

```
api/                 # REST controllers
  catalog/
  cart/              # if cart persistence is confirmed
  wishlist/
  admin/
  auth/
  social/            # WhatsApp link helpers; Instagram proxy if needed
service/
domain/              # JPA entities
repository/
security/
config/
```

### 4.3 Layers

1. **Controller** — HTTP, validation, DTO mapping.
2. **Service** — catalog rules, price % off calculation, admin product create.
3. **Repository / JPA** — MySQL.
4. **Security** — `/admin/**` authenticated; public catalog GETs anonymous.

### 4.4 Security baseline (only what is justified)

PDF-explicit: admin must log in.

Proposed (standard for this stack, still confirmable):

- Form login or JWT for admin only in v1.
- CORS limited to the React origin.
- HTTPS in production.
- Image upload type/size checks.

**FINAL confirmed:** screens for guest checkout, optional customer register/login, order history, and order tracking. Exact OTP/password/social auth and guest order-verification UX are **PENDING design** — do not invent them in architecture.

### 4.5 File storage

The PDF requires **image upload** and does not specify storage.

**Proposed:** store files on local disk or a simple object folder; save the path on the product record. Cloud object storage is **CLIENT CONFIRMATION REQUIRED**.

---

## 5. MySQL architecture

One MySQL schema, one application user.

**PDF-backed core:** Category, Product, ProductImage, AdminUser.

**Proposed only to support PDF UI that needs data:** ratings display fields on product (or a Review table if customer reviews are confirmed), cart/wishlist tables if those must persist.

**Not in CURRENT V1 schema:** Order, Payment, Coupon, Address, Customer, Shipment, Cart, Wishlist.

**FINAL confirmed:** Order/Payment (and cart/wishlist persistence) are required for the next commerce phase — **table designs PENDING**; do not invent DDL here. **Customer accounts and guest checkout are both CONFIRMED**; Customer entity/auth schema shapes remain **PENDING design**.

See `DATABASE_DESIGN.md` for entities and PDF vs inferred fields.

No read replicas, sharding, or extra databases.

---

## 6. Communication between frontend, backend, and database

```
[Browser React]
    HTTPS REST (JSON)
[Spring Boot Controller]
    Service calls
[JPA Repository]
    JDBC / Hibernate
[MySQL]
```

Rules:

- The browser never talks to MySQL.
- Product images are uploaded to the API, then served as static URLs or a download endpoint.
- `% off` is calculated in the API (or as a DB generated value) from Selling Price and MRP so the CMS does not ask the owner to type it.
- Cart badge count comes from API and/or client state. **FINAL** requires a **persistent** cart; storage strategy (guest/server) remains **PENDING**. CURRENT V1 has no cart API.

---

## 7. External integrations

Only channels named in the PDF are in this section.

### 7.1 WhatsApp (PDF-explicit)

- **Inquiry widget:** floating button opening WhatsApp chat.
- **Buy via WhatsApp:** PDP secondary button opening WhatsApp, preferably with a pre-filled product name and URL — **alternate** buy path (not primary order flow).

Implementation for a small site: `https://wa.me/<number>?text=...` (or `https://api.whatsapp.com/send`). No WhatsApp Business API is required unless the client later wants automated messages.

**CONFIRMED number:** client-provided `6395700831`. International/`wa.me` digit formatting is a **configuration/verification** item if needed — do not silently change the documented client number.

**PENDING:** message template; whether a site order is also created from WhatsApp.

### 7.2 Instagram (PDF-explicit)

Homepage live grid + Follow CTA.

**CONFIRMED profile / Follow URL:** `https://www.instagram.com/macreations.living/` (clean URL). Do not use `@she_lift__heavy`.

Options for live grid (tech still **PENDING**):

1. Official Instagram embed / oEmbed (simplest).
2. Instagram Graph API via the Spring Boot backend (true “live” feed; needs Meta app and token).

The backend should proxy any token-based API so secrets stay off the client.

### 7.3 Facebook

Named only as a **traffic source**. No Facebook plugin, shop, or pixel is specified. **PENDING.**

### 7.4 Payment

**CONFIRMED FINAL — required checkout payment methods:** **UPI**, **Credit/Debit Card**, **Net Banking**, and **Cash on Delivery (COD)**.

**COD:** required option; orders incur an **additional charge**. Exact charge amount/calculation rule is **PENDING** — do not invent a fixed amount or percentage.

**PENDING:** payment gateway/provider (not selected — do not invent or integrate a specific provider for online methods). Additional payment methods may be added later if the client requests them.

Footer trust badges and listing UPI / PDP Pay Later tags remain part of the visual language. They are **not** display-only substitutes for real checkout payment.

Architecture placeholder: a `payment` adapter behind the API (online methods). COD is a checkout payment option with configurable surcharge once the charge rule is decided. Wire gateway integration only after a provider is chosen.

### 7.5 Meesho

The Meesho store is **catalog reference**, not a live integration. There is no Meesho API in this architecture.

---

## 8. Customer vs admin applications

### 8.1 Customer storefront

- Public.
- PDF screens: Home, PLP, PDP.
- Chrome: header (logo, search, wishlist, cart count), footer (quick links, return policy, payment badges), WhatsApp widget.
- Conversion paths: Add to Cart → checkout (**FINAL**); Buy via WhatsApp (**alternate**); Shop Now (**destination PENDING**); Follow on Instagram.

### 8.2 Admin CMS

- Authenticated.
- Non-technical: short labels, category dropdown fixed to the 5 PDF categories, image upload, two prices.
- Must not require the owner to write HTML or SQL.

**FINAL confirmed:** admin order screens belong in the commerce phase once checkout ships. They are not in the CURRENT V1 architecture surface. Product **Hide/Unpublish** is the normal visibility control (FUTURE vs CURRENT hard delete).

### 8.3 Why not Shopify / WooCommerce

The PDF’s CMS paragraph is about **ease of editing**, not the runtime. The client already chose React + Spring Boot + MySQL. Building a small custom admin meets both constraints.

---

## 9. What this architecture deliberately excludes

- Microservices
- API gateway product
- Redis / Kafka / Elasticsearch
- Headless Shopify
- Native mobile apps
- Multi-tenant / multi-vendor
- Server-side rendering framework (not required by the PDF; React SPA is enough)

These can be revisited only if scale or SEO requirements are later confirmed.

---

## 10. Environment sketch (deployment not specified)

**CLIENT CONFIRMATION REQUIRED:** hosting, domain, SSL, CI/CD.

A typical small deployment, for planning only:

- React static build (nginx or equivalent)
- Spring Boot JAR
- Managed or self-hosted MySQL

Not part of Step 1 implementation.

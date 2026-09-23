# MA CREATIONS — Frontend Catalog Implementation

**Step:** 7 — React storefront connected to catalog APIs  
**No Redux.** Local React state/hooks only.  
**Document type:** CURRENT V1 as-built.

**Not implemented in Step 7 (original):** wishlist, checkout, customer auth, payments, featured API, Instagram live feed.

**Later steps (as built):** guest shopping cart; product search; **guest wishlist IMPLEMENTED** (`/wishlist` + `GET /api/wishlist`).

**FINAL confirmed (remaining):** website checkout + payment (**UPI**, **Credit/Debit Card**, **Net Banking**, **COD**; gateway/provider PENDING; COD charge rule PENDING); **guest checkout** + optional **customer accounts** (auth/tracking mechanisms PENDING design); wishlist **customer merge**. Contacts: WhatsApp `6395700831`; Instagram `https://www.instagram.com/macreations.living/`. Shop Now destination and bestseller rules remain **PENDING**.

**IMPLEMENTED:** Search, guest cart, guest wishlist, **Hide/Unpublish** (`published`).

---

## React structure

```
frontend/ma-creations-web/src/
├── components/
│   ├── Button/
│   ├── ProductCard/
│   ├── CategoryCard/
│   ├── PriceDisplay/
│   ├── DiscountBadge/
│   ├── RatingDisplay/
│   ├── Header/
│   ├── Footer/
│   ├── Loading/
│   ├── ErrorState/
│   ├── HeroCarousel/          (temporary)
│   ├── InstagramSection/      (temporary)
│   └── WhatsAppWidget/
├── pages/
│   ├── Home/HomePage.jsx
│   ├── Category/CategoryPage.jsx
│   └── ProductDetails/ProductDetailsPage.jsx
├── layouts/StorefrontLayout.jsx
├── services/
│   ├── apiClient.js
│   ├── categoryService.js
│   └── productService.js
├── config/brand.js
├── routes/AppRoutes.jsx
└── test/setup.js
```

---

## Routes

| Path | Page |
|---|---|
| `/` | Homepage |
| `/categories/:categoryId` | Category / PLP |
| `/products/:id` | Product Detail / PDP |

No checkout, account, orders, or customer-login routes.

---

## API integration

| Service | Backend |
|---|---|
| `fetchCategories()` | `GET /api/categories` |
| `fetchProducts({ categoryId, sort })` | `GET /api/products?categoryId=&sort=` |
| `fetchProductById(id)` | `GET /api/products/{id}` |
| `resolveMediaUrl(url)` | Prefixes `/api/media/...` with `VITE_API_BASE_URL` |

UI components do **not** call `fetch` directly.

Sort values: `newest`, `price_asc`, `price_desc`.

---

## Environment variables

| Variable | Purpose |
|---|---|
| `VITE_API_BASE_URL` | Backend base URL (default `http://localhost:8080`) |
| `VITE_BRAND_FONT_FAMILY` | Temporary font CSS stack override |
| `VITE_WHATSAPP_NUMBER` | Digits with country code for WhatsApp CTAs |
| `VITE_INSTAGRAM_HANDLE` | Follow CTA |
| `VITE_INSTAGRAM_FOLLOW_URL` | Optional explicit follow URL |

See `frontend/ma-creations-web/.env.example`.

---

## Design system

| Token | Value |
|---|---|
| Background | `#FDFBF7` |
| Primary | `#D4A373` |
| Text | `#2C2C2C` |
| Font | Temporary: Inter + Montserrat loaded; default prefers Inter. **Final choice CLIENT CONFIRMATION REQUIRED.** |

**Responsive:** mobile-first (~390), `md`+ for tablet/desktop, content width `max-w-6xl`. Homepage categories scroll on mobile / wrap on desktop. Product grids 2 → 3 → 4 columns. PDP sticky bottom CTA on mobile only.

---

## Temporary / TBD sections

| Section | Notes |
|---|---|
| Hero carousel | Asset-driven later; Step 7 used placeholders |
| Shop Now | Temporary `#categories` until destination **PENDING** is answered |
| Bestsellers & Featured | Newest fallback; **selection rules PENDING** |
| Instagram | Follow URL **CONFIRMED**; live grid PENDING |
| WhatsApp | Number **CONFIRMED** `6395700831`; template PENDING |
| Header Search | **IMPLEMENTED** — expands input → `/search?q=` |
| Header Wishlist | **IMPLEMENTED** — links to `/wishlist` with live badge |
| Header Cart | **IMPLEMENTED** — links to `/cart` with live badge |
| Add to Cart | UI notice only — **FINAL** requires persistent cart |

---

## Known limitations

- Live catalog requires backend + MySQL with data
- Cart badge uses live guest cart count
- Search is live via header → `/search?q=`
- No admin React screens in this step (added in later steps)
- Instagram is not a live feed

---

## Intentionally not implemented in Step 7

Customer/admin authentication (admin added later), cart/wishlist persistence, checkout, orders, payments, coupons, shipping, variants, inventory, review submission, payment gateway APIs, featured/bestseller backend flags.

**Note:** Checkout (**guest + optional accounts**), payment (**UPI** + **Card** + **Net Banking** + **COD**) remain **CONFIRMED FINAL** for later phases. **Search**, **guest cart**, **guest wishlist**, and **Hide/Unpublish** are **IMPLEMENTED**. Customer wishlist merge and auth remain PENDING. Gateway/provider, COD charge rule, and auth/tracking mechanisms remain PENDING design.

---

## Tests

```bash
cd frontend/ma-creations-web
npm test
```

Coverage: API services, ProductCard/CategoryCard, PLP sort interaction, PDP load / not-found.

---

## Run

```bash
npm run dev      # http://localhost:5173
npm run build
```

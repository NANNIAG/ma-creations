# MA CREATIONS — Storefront Polish (Step 10)

**Goal:** Production-ready customer-facing polish for confirmed/configurable storefront pieces without inventing client-specific values.

**Document type:** CURRENT V1 implementation notes.

**FINAL confirmed (not built in this step):** persistent cart, wishlist, product search, website checkout + payment (**UPI**, **Credit/Debit Card**, **Net Banking**, **COD**; gateway/provider PENDING; COD charge rule PENDING), **guest checkout** + optional **customer accounts** (mechanisms PENDING design), Hide/Unpublish. See `CLIENT_CONFIRMATIONS.md`.

**Confirmed contacts:** WhatsApp `6395700831`; Instagram `https://www.instagram.com/macreations.living/`.

---

## Changes made

- Confirmed design tokens reinforced (`#FDFBF7`, `#D4A373`, `#2C2C2C`); font remains env-overridable (Inter/Montserrat both loaded temporarily).
- **Hero** is asset-driven: gradient placeholders by default; real images via `public/heroes/` + `VITE_HERO_SLIDES_JSON`.
- **Shop Now** destination configurable via `VITE_HERO_SHOP_NOW_TARGET` (temporary default `#categories`; **final destination PENDING**).
- **WhatsApp** number validation (≥10 digits), safer unconfigured UI, FAB cleared above mobile sticky CTA.
- **Instagram** follow URL/handle via env; placeholders explicitly labeled as non-live.
- Homepage / PLP empty states polished (no fake products / no fake product images).
- Header icons remain disabled UI-only with clearer a11y labels (**FINAL** requires working Search / Wishlist / Cart later).
- Footer shows Instagram/WhatsApp links only when configured.
- Focus-visible polish on buttons/header controls.
- Category names still loaded from `GET /api/categories` (not hardcoded).
- Bestsellers section still uses newest products fallback (**selection rules PENDING**).

**Not added in Step 10:** checkout/cart/wishlist backends, Instagram Graph API, payment gateway, fake catalog product photography. Required checkout methods (**UPI**, **Card**, **Net Banking**, **COD**) are CONFIRMED; gateway/provider and COD charge rule PENDING — not implemented as payment here.

---

## Environment variables (public / frontend)

| Variable | Required | Purpose |
|---|---|---|
| `VITE_API_BASE_URL` | Yes (for non-default API host/port) | Backend base URL |
| `VITE_WHATSAPP_NUMBER` | Optional | Digits + country code for WhatsApp CTAs |
| `VITE_INSTAGRAM_HANDLE` | Optional | Handle for follow CTA |
| `VITE_INSTAGRAM_FOLLOW_URL` | Optional | Explicit profile URL (overrides handle) |
| `VITE_BRAND_FONT_FAMILY` | Optional | Temporary font stack override |
| `VITE_HERO_SHOP_NOW_TARGET` | Optional | `#categories` or path like `/categories/1` |
| `VITE_HERO_SLIDES_JSON` | Optional | JSON array of hero slides with optional `image` |

All `VITE_*` values are **public** (bundled to the browser). Never put JWT/DB secrets here.

Template: `frontend/ma-creations-web/.env.example`

Local note: if the API runs on **8081**, set `VITE_API_BASE_URL=http://localhost:8081` in `.env.local`.

---

## WhatsApp configuration

1. Copy `.env.example` → `.env.local`
2. Set `VITE_WHATSAPP_NUMBER=91XXXXXXXXXX` (no `+`)
3. Restart `npm run dev`

Affects: floating widget, PDP “Buy via WhatsApp”, footer link.

If unset: disabled/config messaging — **no fake production number**.

---

## Instagram configuration

Set either:

- `VITE_INSTAGRAM_HANDLE=your_handle`, or
- `VITE_INSTAGRAM_FOLLOW_URL=https://instagram.com/your_handle`

Live grid/API is **not** implemented (CLIENT CONFIRMATION REQUIRED).

---

## Hero asset setup

1. Drop images into `frontend/ma-creations-web/public/heroes/` (see `README.md` there).
2. Reference them in `VITE_HERO_SLIDES_JSON` (paths like `/heroes/slide-1.jpg`).
3. Optionally set `VITE_HERO_SHOP_NOW_TARGET`.

Until then, clean gradient placeholders are shown (not presented as client photography).

---

## Font pending decision

Inter **or** Montserrat remains **CLIENT CONFIRMATION REQUIRED**.  
Both are loaded; default stack prefers Inter then Montserrat; override with `VITE_BRAND_FONT_FAMILY`.

---

## Client assets / contacts

| Item | Status |
|---|---|
| Hero / category creatives | Provided (see `public/heroes/`, `public/categories/`) |
| Logo | Provided (`public/branding/logo.png`) |
| WhatsApp | **CONFIRMED** `6395700831` |
| Instagram | **CONFIRMED** `https://www.instagram.com/macreations.living/` |
| Return policy / payment badge copy | PENDING |
| Shop Now final destination | **PENDING** |

---

## Tests / builds

| Check | Result |
|---|---|
| `npm test -- --run` | Pass — **33** tests (as of Step 10) |
| `npm run build` | Pass |
| `mvn test` | Pass (no MySQL integration claimed) |
| `mvn package` | Pass |

---

## Known / TBD (FINAL vs CURRENT)

| Topic | FINAL | CURRENT V1 |
|---|---|---|
| Search, wishlist, persistent cart | **CONFIRMED required** | Not built / UI-only |
| Checkout, orders, online payment | **CONFIRMED** (guest + accounts; UPI + Card + Net Banking + COD; gateway + COD charge PENDING) | Not built |
| Customer auth / guest order tracking | **CONFIRMED need**; mechanisms PENDING design | Not built |
| Bestseller / featured selection rules | **PENDING** | Newest fallback |
| Shop Now final destination | **PENDING** | Temporary `#categories` |
| Hide/Unpublish | **CONFIRMED** | Hard delete in admin |
| Reviews write / S3 | PENDING | N/A |

# MA CREATIONS — Requirements

**Source of truth:** `D:\Muskan\MA_Creations_Figma_Blueprint_and_Catalog.pdf` (8 pages), titled *MA CREATIONS — WEBSITE UI/UX FIGMA BLUEPRINT*.

**Analysis method:** Full text extraction plus visual review of Meesho catalog screenshots (PDF pages 2, 4, and 6) and figure captions (pages 3, 5, and 7).

**Stack (client-stated, not from the PDF):** React.js, Tailwind CSS, React Router, Redux Toolkit if required, Java, Spring Boot, Spring Data JPA / Hibernate, Spring Security, REST APIs, MySQL.

**Legend**

- **PDF-explicit:** stated in the blueprint text.
- **Screenshot-observed:** visible in the Meesho catalog images inside the PDF. These show current catalog aesthetic, pricing tags, ratings, and product varieties. They are reference material, not a second wireframe spec. Meesho marketplace chrome that is *not* repeated in Section 3 is not treated as a website requirement.
- **CLIENT CONFIRMATION REQUIRED / PENDING:** not yet decided (see `CLIENT_CONFIRMATIONS.md`).
- **CONFIRMED (FINAL scope):** answered in the client requirement-clarification phase. May not be implemented yet — keep separate from **CURRENT V1** build status.

**Scope status (do not confuse with CURRENT V1 implementation)**

| Area | FINAL client decision | Notes |
|---|---|---|
| Selling / checkout / online payment | **CONFIRMED** — customers order on the website; full shopping + checkout + online payment | WhatsApp = contact / alternate buy, not primary order flow |
| Search, wishlist, persistent cart | Search + cart + wishlist **IMPLEMENTED** (guest + customer + merge) | — |
| Customer accounts | **IMPLEMENTED** — mobile OTP login (optional) | Real SMS provider PENDING; guest checkout remains |
| UPI / Card / Net Banking | **IMPLEMENTED** via Razorpay Checkout (Step 23–24) | Live Razorpay test credentials may still be unset locally |
| Cash on Delivery (COD) | **IMPLEMENTED** order + **₹20** fee (Step 25) | Outside Razorpay |
| Pay Later | **REQUIRED** in checkout; gated by `RAZORPAY_PAY_LATER_ENABLED` | Provider rail / merchant enablement PENDING |
| Additional payment methods | May be added later if the client requests them | — |
| Product hide/unpublish | **IMPLEMENTED** (`published` flag; Hide/Publish) | Hard delete removed from Admin flow |
| Product images | **CONFIRMED** — Admin Dashboard upload only | No fake/static catalog product images |
| WhatsApp number | **CONFIRMED** client-provided `6395700831` | Intl/`wa.me` formatting = config verification |
| Instagram | **CONFIRMED** `https://www.instagram.com/macreations.living/` | Clean URL only |
| Shop Now destination | **PENDING** | Do not assume |
| Bestsellers / Featured rules | **PENDING** | Do not assume |

See `CLIENT_CONFIRMATIONS.md` for the authoritative confirmation log.

---

## 1. Project purpose (PDF-explicit)

Build a **custom e-commerce website** for **MA CREATIONS**.

The PDF is a master UI/UX blueprint and product taxonomy guide. It covers:

- Aesthetic vision and Figma style guide
- Page-by-page wireframe specifications
- Product categorization based on the Meesho store catalog
- CMS setup instructions for future product updates without coding

**Store identity**

| Item | Source | Value |
|---|---|---|
| Website brand name | PDF-explicit (header logo) | `MA CREATIONS` |
| Meesho store name | PDF-explicit + screenshot | `M_A Creations` / `M_A CREATIONS` |
| Business type | PDF-explicit | Home and kitchen products (current and future catalog must use the 5 categories) |
| Primary traffic sources | PDF-explicit | Instagram, Facebook, WhatsApp |
| Traffic device mix | PDF-explicit | 90%+ social media traffic is mobile |
| Design goal | PDF-explicit | Clean, premium, welcoming; high conversion from social traffic |

---

## 2. Functional requirements

### 2.1 Catalog and navigation (PDF-explicit)

- Organize **all current and future** home/kitchen products into **exactly 5 categories** (see Section 4).
- Homepage must expose those 5 categories as quick-link tiles.
- Category / product listing page (PLP) must list products with sort.
- Product detail page (PDP) must show title, images, rating, and pricing.
- Header search icon is specified (PDF). **CONFIRMED (FINAL):** product search is **required** on the storefront. Results page layout, autocomplete, and searchable fields detail remain **PENDING**.

### 2.2 Product discovery (PDF-explicit)

- Homepage **Bestsellers & Featured** product grid.
- PLP sort by:
  - Price: Low to High
  - Price: High to Low
  - Newest arrivals
- PLP “filter bar” is named, but the only specified controls are the sort options above. Additional filters (category on PLP, price range, rating, gender) are **CLIENT CONFIRMATION REQUIRED**.

**Screenshot-observed (Meesho only, not in Section 3 wireframes):** Sort, Category dropdown, Gender dropdown, Filters. Gender is a Meesho marketplace control and is **not** a confirmed website requirement.

### 2.3 Pricing display (PDF-explicit + screenshot-observed)

Product cards and PDP must show a clear pricing hierarchy:

- Selling / discounted price
- MRP
- Percentage-off badge
- **UPI badge** on listing cards (PDF example: `₹124 ₹152 18% off UPI badge`)
- **“with Pay Later”** tag on PDP (PDF example: `4.0 ★ [135 ratings]` plus dynamic pricing with Pay Later)

**Screenshot-observed examples (INR):**

| Product title (as shown) | Selling price | MRP | Off | Pay Later price | Rating |
|---|---|---|---|---|---|
| Classy Water Bottles | ₹147 | ₹188 | 22% off | ₹106 | 4★ (135) |
| Classic Water Bottles | ₹148 | ₹176 | 16% off | ₹107 | 3.6★ (21) |
| Designer Water Bottles | ₹214 | ₹260 | 18% off | ₹175 | 4★ (135) |
| Classy Lunch Boxes | ₹188 | ₹221 | 15% off | ₹148 | 4★ (135) |
| Graceful Cooking Spoons | ₹156 | ₹199 | 22% off | ₹115 | 4★ (135) |
| Cups, Mugs & Saucers | ₹186 | ₹228 | 18% off | ₹146 | 4★ (135) |
| Cycling Water Bottles & Shakers | ₹172 | ₹218 | 21% off | ₹132 | 4★ (135) |
| Cups, Mugs & Saucers (second card) | ₹165 | ₹194 | 15% off | ₹125 | 3.4★ (7) |

**CONFIRMED REQUIRED checkout payment methods:** **UPI**, **Credit/Debit Card**, **Net Banking**, **Pay Later**, and **Cash on Delivery (COD)**. COD orders have an **additional charge**; exact amount/calculation rule is **PENDING** — do not assume a fixed amount or percentage. **Payment gateway/provider** and **Pay Later provider** are **PENDING** — do not invent. PDF/screenshot UPI and Pay Later badges remain visual language until payment UX is designed.

Currency is **INR (₹)** from the PDF example and screenshots. Product prices are **GST-inclusive**; GST is **not charged** separately by the application (Step 25).

### 2.4 Cart (PDF-explicit + CONFIRMED FINAL)

Specified in PDF:

- Header shopping cart **counter badge**
- **Add to Cart** on homepage product cards
- **Add to Cart** as primary PDP action
- Sticky bottom CTA bar on product pages (contents of that bar are not itemized; Add to Cart / Buy via WhatsApp are the specified PDP actions)

**CONFIRMED (FINAL):** a **real / persistent shopping cart** is required. CURRENT V1 UI-only “Add to Cart” must eventually be replaced. Cart page and shopping flow are part of website order placement.

**Still PENDING:** cart page wireframe details, guest vs logged-in storage strategy, mini-cart, stock checks, cart expiry.

### 2.5 Wishlist (PDF-explicit + CONFIRMED FINAL)

Specified in PDF:

- Header **Wishlist icon**

**Screenshot-observed:** heart icon on each product card.

**CONFIRMED (FINAL):** wishlist is **required**; customers must be able to **add/remove** products.

**IMPLEMENTED (guest + customer):** DB wishlist + `/wishlist` + ProductCard/PDP hearts. Guest: `X-Wishlist-Token`. Customer: CUSTOMER JWT. Guest→customer merge: `POST /api/wishlist/merge`.

**Still PENDING:** checkout linkage only.

### 2.6 WhatsApp (PDF-explicit + CONFIRMED contacts)

Two distinct WhatsApp surfaces:

1. **Floating WhatsApp chat widget** in the footer area, for instant customer inquiries.
2. PDP secondary button **Buy via WhatsApp** for quick social conversion (**alternate** buy path — not the primary order flow).

**CONFIRMED:** client-provided WhatsApp Business number **`6395700831`**. Do not invent or change this number. International/`wa.me` formatting is a **configuration/verification** item if needed later.

**Still PENDING:** pre-filled message template; whether Buy via WhatsApp also creates a site order record; business hours / staffing.

### 2.7 Instagram / social proof (PDF-explicit + CONFIRMED profile)

- Homepage live grid of recent posts from Instagram.
- PDF originally offered `@she_lift__heavy` or brand page — **superseded**.
- CTA: **Follow on Instagram**.

**CONFIRMED:** official profile URL `https://www.instagram.com/macreations.living/` (clean URL; no tracking query params).

**Still PENDING:** embed vs Graph API for live grid; post-tap behavior; Facebook on-site UI/pixel.

### 2.8 Reviews / ratings (PDF-explicit for display)

PDP must show **star rating and rating count** (example: `4.0 ★ [135 ratings]`).

Screenshot-observed: per-product stars and counts; store-level `4★` and `135 ratings`.

Customer ability to **write** reviews, moderation, photos in reviews, and whether ratings are imported from Meesho or collected on this site are **CLIENT CONFIRMATION REQUIRED**.

### 2.9 Search (PDF-explicit icon) — IMPLEMENTED

Header **Search icon** is required (PDF).

**IMPLEMENTED:** customers can search products from the storefront header. Results page: `/search?q=`. API: `GET /api/products?search=`. Matches title, category name, and slug (case-insensitive substring).

**Still PENDING / out of scope for current search:** autocomplete, typo tolerance, advanced filters.

### 2.10 CMS / admin (PDF-explicit + CONFIRMED visibility rules)

- Simple backend the owner can **log in to without coding knowledge**.
- PDF cites Shopify or WordPress/WooCommerce as examples of that simplicity. That is a **usability requirement**, not a platform requirement. The agreed build is a custom Spring Boot admin (see `SYSTEM_ARCHITECTURE.md`).
- **Add New Product** form fields:
  - Product Title
  - Category Dropdown (linked to the 5 main categories)
  - Image Upload
  - Selling Price
  - MRP

**IMPLEMENTED:** product images are uploaded/managed via Admin (no fake/static catalog product images). Product **list**, **edit**, and **Hide/Publish** (`published` boolean). Hidden products must not appear on the storefront but remain in Admin to publish again. Permanent delete is **not** part of the normal admin workflow.

**Still PENDING:** inventory, banners CMS, homepage content CMS, return-policy editing, multiple admin users, order management UI details.

**CURRENT note:** Hard delete was removed from the Admin product API/UI when Hide/Unpublish shipped (V6 `published` column).

### 2.11 Checkout, orders, accounts, shipping, payments

The PDF does **not** wireframe cart/checkout/order confirmation pages. Client clarification has since **CONFIRMED FINAL** business scope:

| Topic | Status |
|---|---|
| Complete shopping + checkout on the website | **CONFIRMED** |
| Online payment (**UPI**, **Credit/Debit Card**, **Net Banking**) | **CONFIRMED REQUIRED** (gateway/provider **PENDING**) |
| Cash on Delivery (COD) | **CONFIRMED REQUIRED**; fee **₹20 IMPLEMENTED** |
| Shipping | V1 flat **₹20 IMPLEMENTED**; provider/PIN **PENDING** |
| GST | **Not charged**; prices GST-inclusive (**IMPLEMENTED**) |
| WhatsApp as primary order path | **No** — alternate / contact only |
| Customer registration / login | **CONFIRMED** — optional accounts (order history, tracking, wishlist / saved info) |
| Guest checkout | **CONFIRMED** — order without account; Order ID; guest tracking flow PENDING design |
| Payment gateway name | **PENDING** |
| Shipping provider, charges, PIN codes | **PENDING** |
| Coupons / offers engine | **PENDING** |
| Order history (customer) | **IMPLEMENTED** (Step 26A); email / SMS **PENDING** |
| Inventory / stock / variants | **PENDING** |
| Legal pages beyond footer return-policy link | **PENDING** |

Do not invent PENDING providers or rules. Track details in `CLIENT_CONFIRMATIONS.md`.

---

## 3. UI / UX requirements

### 3.1 Brand aesthetic (PDF-explicit)

| Token | Value | Usage |
|---|---|---|
| Background | Soft warm neutral cream `#FDFBF7` | Site background; organic boutique feel; must not distract from product photography |
| Primary accent | Warm terracotta / beige `#D4A373` | Primary action buttons, badges, active category highlights, Shop Now |
| Typography color | Dark charcoal `#2C2C2C` | Text on mobile and desktop |
| Fonts | Inter **or** Montserrat | Header Bold 600/700; Body Regular 400 |
| Layout | Fully responsive, **mobile-first** | Sticky bottom CTA bar on product pages |

Exact font choice (Inter vs Montserrat), logo file, and icon set are **CLIENT CONFIRMATION REQUIRED**.

### 3.2 Screen 1 — Homepage (mobile and desktop) (PDF-explicit)

1. **Header bar**
   - Left: logo text `MA CREATIONS`
   - Right: Search icon, Wishlist icon, Shopping Cart counter badge
2. **Hero banner**
   - Full-width image carousel
   - Themes to highlight: home decor, festive candles, sleek water bottles
   - Bold **Shop Now** button in `#D4A373`
3. **Category quick links**
   - Horizontal scroll **or** grid
   - **5** circular / rounded image tiles
   - One tile per main category
4. **Bestsellers & Featured grid**
   - Clean **2-column** product grid
   - Each card: product photo, title, discounted price vs MRP, percentage-off badge, **Add to Cart**
5. **Social proof**
   - Live Instagram post grid
   - **Follow on Instagram** CTA
6. **Footer**
   - Quick links
   - Return policy
   - Payment trust badges
   - Floating WhatsApp chat widget

Header/footer repetition on inner pages is implied by normal site structure but not written. **CLIENT CONFIRMATION REQUIRED** if inner pages differ.

**Screenshot-observed store header (Meesho, not a website wireframe):** store banner, share button, `4★ / 135 ratings / 4 Followers / 0 Products`, Unfollow. Do **not** copy Meesho follower/product counts or Unfollow into the custom site unless the client confirms a store-profile page.

### 3.3 Screen 2 — Category and product listing (PLP) (PDF-explicit)

- Sort and filter bar: dropdown sort for Price (Low to High, High to Low) and Newest arrivals
- Product cards in a responsive grid
- Pricing hierarchy including UPI badge (example `₹124 ₹152 18% off`)

Grid column count for desktop vs mobile (homepage specifies 2-column; PLP says “responsive grid” only) is **CLIENT CONFIRMATION REQUIRED**.

### 3.4 Screen 3 — Product detail (PDP) (PDF-explicit)

- High-resolution **zoomable** main image
- Thumbnail gallery underneath
- Product title
- Star rating and rating count
- Dynamic pricing with **with Pay Later** tag
- Primary: **Add to Cart**
- Secondary: **Buy via WhatsApp**
- Sticky bottom CTA bar (mobile-first layout architecture)

Product description, specifications, delivery info, similar products, and share button are **CLIENT CONFIRMATION REQUIRED**.

### 3.5 Screen 4 — CMS admin (PDF-explicit)

- Login without coding knowledge
- Add New Product form (fields listed in Section 2.10)

Admin visual style is not specified. **CLIENT CONFIRMATION REQUIRED** whether admin uses the same cream/terracotta theme or a simpler functional UI.

### 3.6 Screens not in the PDF

These were **not** wireframed in the PDF: Search results, Wishlist page, Cart page, Checkout, Order confirmation, Customer login/register, Order tracking, About, Contact, Return policy content page, Privacy, Terms.

**FINAL confirmed:** screens for Search, Wishlist, Cart, Checkout, Order Confirmation, **Guest Checkout**, optional **Customer Account** (login/register, order history, tracking). Customer login/register wireframes still to be designed; auth mechanism PENDING. Other legal/content pages remain **PENDING** unless separately confirmed.

---

## 4. Product and category requirements

### 4.1 Five required categories (PDF-explicit)

All current and future home/kitchen products **must** use these 5 categories.

| # | Category name | Sub-categories / focus (PDF) |
|---|---|---|
| 1 | Hydration & Drinkware | Water bottles, Gym shakers, Insulated vacuum flasks |
| 2 | Lunch & Meal Prep | BPA-free lunch boxes, partitioned meal containers |
| 3 | Kitchen Gadgets & Prep | Graters, slicers, pizza cutters, cooking spoons, coffee pots |
| 4 | Storage & Kitchenware | Containers, jars, cups, mugs, saucers, strainers |
| 5 | Home Decor & Festivity | Candles, decorative diyas, lanterns, tealight sets |

Whether **sub-categories** are navigable filters, CMS fields, or descriptive notes only is **CLIENT CONFIRMATION REQUIRED**. The PDF lists them as “Sub-Categories / Focus.”

### 4.2 Sample products from catalog (PDF-explicit names)

These names are the taxonomy’s “Sample Products from Catalog.” They are the named catalog set to organize. The PDF does not say every name must exist as a live SKU on day one.

**Hydration & Drinkware**

- Modern Water Bottles
- Classy Water Bottles
- Classic Water Bottles
- Designer Water Bottles
- Cycling Water Bottles & Shakers
- Amazing Water Bottles
- Stylo Water Bottles

**Lunch & Meal Prep**

- Essential Lunch Boxes
- Stylo Lunch Boxes
- Designer Lunch Boxes
- Fancy Lunch Boxes
- Modern Lunch Boxes
- Classy Lunch Boxes

**Kitchen Gadgets & Prep**

- Graceful Graters & Slicers
- Trendy Pizza Cutters
- Fancy Pizza Cutters
- Stylo Graters & Slicers
- Graceful Cooking Spoons
- Classy Coffee Pots

**Storage & Kitchenware**

- Modern Jars & Containers
- Essential Jars & Containers
- Cups, Mugs & Saucers
- Strainers & Sieves

**Home Decor & Festivity**

- Royal Candles
- Royal Diyas & Lanterns
- Graceful Diyas & Lanterns
- Fancy Candles
- Classy Candles
- Unique Candles

### 4.3 Screenshot-observed catalog (visual, pages 2 / 4 / 6)

Figure captions in the PDF:

- Figure 1.1: Store Header, Modern Water Bottles & Essential Lunch Boxes
- Figure 1.2: Kitchen Prep Tools, Shakers, Cups & Mugs
- Figure 1.3: Home Decor, Royal Candles & Festive Diyas

**Visible product cards**

- Modern Water Bottles (image; catalog id fragment `1042806413`; pricing cropped)
- Essential Lunch Boxes (teal partitioned “Hungry Lunch Box” set with cup, spoon, fork; pricing cropped)
- Classy Water Bottles (pink bottle; full pricing — see table in 2.3)
- Classic Water Bottles (clear/sports-style bottle; full pricing)
- Designer Water Bottles (gym/time-marked bottles; full pricing)
- Classy Lunch Boxes (purple partitioned lunch set; full pricing)
- Modern Lunch Boxes (teal partitioned lunch set; pricing cropped)
- Essential Thermos & Vacuum Flasks (pink flask; **this title appears in the screenshot but is not in the Section 4 sample-name list**; flasks are a Hydration sub-category)
- Graceful Cooking Spoons (wooden spoon in pan; full pricing)
- Cups, Mugs & Saucers (tumbler/mug set; two distinct cards/prices)
- Cycling Water Bottles & Shakers (black/blue shaker; full pricing)
- Bottom of Figure 1.3 screenshot: additional cards showing glass jars and a strainer (titles cropped). These align visually with Storage & Kitchenware. The caption also claims Home Decor / Royal Candles / Festive Diyas, but those product titles were **not fully readable** in the provided screenshots.

**Screenshot-observed card UI (reference aesthetic, not extra screens):** product photo, wishlist heart overlay, title, selling price, struck MRP, % off, UPI badge, Pay Later price, optional “shop” label, star rating and count.

**CLIENT CONFIRMATION REQUIRED:** complete SKU list, descriptions, remaining prices, whether “Essential Thermos & Vacuum Flasks” is a product or only a sub-category, and whether duplicate titles (two “Cups, Mugs & Saucers” cards) are variants or separate products.

### 4.4 Product data the CMS can currently capture (PDF-explicit)

- Title
- One of 5 categories
- Image(s) via upload (count of images not specified)
- Selling price
- MRP

**Not specified / still PENDING:** SKU, description, additional images max count, stock quantity, variants, weight, material (e.g. BPA-free as a product flag), HSN/GST, video, bestseller/featured flags, display order, slug.

**CONFIRMED (FINAL):** product **Hide/Unpublish** (active/hidden-style visibility) for normal admin workflow — see §2.10. **Bestsellers / Featured selection rules remain PENDING** — do not assume manual flags, sales, or newest.

Percentage off can be derived as `(MRP - Selling Price) / MRP`. Checkout must accept **UPI**, **Credit/Debit Card**, **Net Banking**, and **COD** (FINAL required). COD additional charge amount/rule remains **PENDING**. Any PDF “Pay Later” display price storage/calculation remains **PENDING**. Gateway/provider remains **PENDING**.

---

## 5. Customer-facing features

| Feature | Status |
|---|---|
| Responsive storefront (mobile-first) | PDF-explicit |
| Homepage (hero, 5 categories, bestsellers/featured, Instagram, footer) | PDF-explicit; **Shop Now destination PENDING**; **bestseller/featured rules PENDING** |
| Category / PLP with price and newest sort | PDF-explicit |
| PDP with gallery, rating, pricing, Add to Cart, Buy via WhatsApp | PDF-explicit; WhatsApp = alternate buy |
| Product search | **IMPLEMENTED** (`/search?q=` + `GET /api/products?search=`) |
| Wishlist (add/remove) | **IMPLEMENTED** (guest + customer + merge) |
| Persistent cart + cart shopping flow | **IMPLEMENTED** (guest + customer + merge) |
| Floating WhatsApp inquiries | PDF-explicit; number **CONFIRMED** `6395700831` |
| Instagram Follow CTA | PDF-explicit; profile **CONFIRMED** `https://www.instagram.com/macreations.living/`; live grid tech PENDING |
| Footer quick links, return policy, payment trust badges | PDF-explicit; link targets and badge set PENDING |
| Sticky PDP CTA bar | PDF-explicit |
| Checkout and order confirmation | **CONFIRMED required** (FINAL); wireframes PENDING |
| Online payment (UPI + Credit/Debit Card + Net Banking) | **CONFIRMED required** (FINAL); gateway/provider PENDING |
| Cash on Delivery (COD) | **CONFIRMED required** (FINAL); additional charge; charge rule PENDING |
| Customer accounts (optional) + Guest checkout | **IMPLEMENTED** mobile OTP + order history (26A) + guest track order#+mobile (26B) | SMS provider PENDING |
| Checkout / Orders | **IMPLEMENTED** place-order, payment, V1 shipping/COD/tax, order mgmt (26A), manual AWB (26B) | Courier API / cancel / invoices PENDING |
| Manage orders | **IMPLEMENTED** (26A list/detail/status + 26B AWB); courier/refund PENDING |
| Write reviews | PENDING (display is PDF-explicit) |

---

## 6. CMS / admin requirements

| Feature | Status |
|---|---|
| Admin login usable by a non-developer | PDF-explicit |
| Add product: title, category (5), image upload, selling price, MRP | PDF-explicit; images via Admin only (**CONFIRMED**) |
| Simple dashboard overview | Named (“Dashboard Overview”) but **no widgets specified**. PENDING |
| Product list / edit | **CONFIRMED** (also CURRENT V1) |
| Hide / Unpublish (normal visibility) | **CONFIRMED** (FINAL); not yet CURRENT V1 |
| Permanent delete in normal workflow | **Not** the normal flow (FINAL) |
| Manage categories (beyond the fixed 5) | PDF says 5 precise categories; changing the set is PENDING |
| Manage hero banners | PENDING |
| Manage Instagram handle / WhatsApp number | PENDING CMS; values CONFIRMED for env/config |
| Manage orders | **IMPLEMENTED** (Step 26A list/detail/status + 26B AWB); courier/refund PENDING |
| Manage inventory | PENDING |
| Manage reviews | PENDING |
| Manage return policy content | PENDING |
| Roles (owner vs staff) | PENDING |

---

## 7. Out of scope (platform / marketplace) — still not approved

Do not treat the following as approved, even if common in e-commerce:

- Shopify or WordPress/WooCommerce as the implementation platform
- Meesho Gender filter, Meesho store follow counts, or Meesho “shop” label
- Multi-vendor marketplace
- Subscription boxes
- Blog / content marketing CMS
- Multi-currency / multi-language
- Native iOS/Android apps

**Note:** Website checkout, persistent cart, wishlist, search, online payment (**UPI**, **Credit/Debit Card**, **Net Banking**), **COD**, and hide/unpublish are **CONFIRMED FINAL** business requirements. Payment gateway/provider, COD charge rule, and other PENDING items are tracked in `CLIENT_CONFIRMATIONS.md` and `DEVELOPMENT_PLAN.md`.

---

## 8. Traceability

| PDF section | Maps to |
|---|---|
| §1 Brand Aesthetic | UI tokens, typography, mobile-first, sticky PDP CTA |
| §2 Meesho screenshots | Catalog names, INR pricing pattern, ratings, UPI / Pay Later badges, wishlist hearts, cart badge |
| §3 Screen 1–3 | Customer storefront pages |
| §3 Screen 4 | Admin add-product + login |
| §4 Taxonomy matrix | 5 categories, sub-category focus, sample product names |

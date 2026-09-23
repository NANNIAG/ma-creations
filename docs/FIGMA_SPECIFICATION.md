# MA CREATIONS — Figma Specification

**Purpose:** Figma-ready UI/UX specification for **PDF Screens 1–4** (CURRENT designed pack).

**Status layers**

| Layer | Meaning |
|---|---|
| **CURRENT Figma pack** | Home, PLP, PDP, Admin login + Add Product |
| **FINAL confirmed (wireframes TBD)** | Search, Wishlist, Cart, Checkout (guest + account), Order Confirmation, optional Customer Account (login/register, order history, tracking), Hide/Unpublish admin UX, payment method selection for **UPI**, **Credit/Debit Card**, **Net Banking**, **COD** |
| **PENDING** | Shop Now destination, bestseller/featured rules, gateway/provider branding, **COD charge display**, **exact auth & guest-tracking UX** (do not invent) |

**Sources of truth**

- `MA_Creations_Figma_Blueprint_and_Catalog.pdf`
- `REQUIREMENTS.md`
- `SYSTEM_ARCHITECTURE.md`
- `USER_WORKFLOW.md`
- `CLIENT_CONFIRMATIONS.md`

**Legend**

| Tag | Meaning |
|---|---|
| **PDF-explicit** | Stated in the blueprint / requirements pack |
| **UI assumption** | Layout detail needed to draw Figma frames; not inventing a new feature |
| **CLIENT CONFIRMATION REQUIRED / PENDING** | Not decided; do not invent |
| **CONFIRMED FINAL** | Approved business scope; may need new frames not in the original PDF pack |

**Out of scope for the original PDF Figma pack (now FINAL / FUTURE frames)**

- Checkout, payment, Search results, Wishlist page, Cart page, Order Confirmation, Guest Checkout, optional Customer Account (login/register, order history, tracking) — **CONFIRMED FINAL** business need; draw when scheduled
- Exact OTP/password/social auth UX and guest order-verification flow — **PENDING design** (do not invent)
- Coupons, inventory UI — PENDING
- Admin product list/edit exist in CURRENT V1 code; prefer **Hide/Unpublish** over permanent delete in FINAL admin UX
- Meesho-only chrome (Gender filter, Follow/Unfollow store, follower counts)

**Confirmed contacts for mock content:** WhatsApp `6395700831`; Instagram `https://www.instagram.com/macreations.living/`. Do not use `@she_lift__heavy`.

---

## 1. Design System

### 1.1 Colors (PDF-explicit)

| Token | Hex | Usage |
|---|---|---|
| Background | `#FDFBF7` | Page background; soft warm cream; must not overpower product photos |
| Primary | `#D4A373` | Primary buttons, badges, active category highlight, Shop Now |
| Text | `#2C2C2C` | Headings and body text (mobile + desktop) |

**UI assumptions (supporting tokens for Figma; not in PDF)**

| Token | Proposed hex | Usage | Status |
|---|---|---|---|
| Surface | `#FFFFFF` | Cards, admin panels, inputs on cream | UI assumption |
| Border subtle | `#E8E2D9` | Dividers, input borders | UI assumption |
| Muted text | `#6B6560` | Secondary labels, MRP strikethrough companion | UI assumption |
| Success / discount | `#2E7D32` | `% off` text (screenshot reference green) | Screenshot-informed assumption |
| Error | `#B3261E` | Form validation | UI assumption |
| Overlay | `rgba(44,44,44,0.4)` | Image zoom / modal scrim | UI assumption |

Do **not** introduce purple marketplace accents from Meesho screenshots into the brand system. Brand accent remains `#D4A373`.

### 1.2 Typography

| Item | Spec | Status |
|---|---|---|
| Font family | Inter **or** Montserrat | **CLIENT CONFIRMATION REQUIRED** — do not pick one in Figma final; show both in a font decision frame or use a placeholder “Sans” until confirmed |
| Heading weight | Bold **600 / 700** | PDF-explicit |
| Body weight | Regular **400** | PDF-explicit |

**Suggested type scale (UI assumption for Figma consistency)**

| Style | Size mobile | Size desktop | Weight | Color |
|---|---|---|---|---|
| Display / Hero | 28–32 | 40–48 | 700 | `#2C2C2C` |
| H1 page title | 24 | 32 | 700 | `#2C2C2C` |
| H2 section | 20 | 24 | 600 | `#2C2C2C` |
| H3 card title | 14–16 | 16 | 600 | `#2C2C2C` |
| Body | 14–16 | 16 | 400 | `#2C2C2C` |
| Caption / meta | 12 | 12–13 | 400 | muted |
| Price selling | 16–18 | 18–20 | 700 | `#2C2C2C` |
| Price MRP | 12–14 | 14 | 400 | muted + strikethrough |
| Button label | 14–16 | 16 | 600 | on primary: white or `#2C2C2C` — **CLIENT CONFIRMATION REQUIRED** for contrast choice |

### 1.3 Spacing system (UI assumption)

Use an **8px base grid**:

| Token | Value |
|---|---|
| xs | 4 |
| sm | 8 |
| md | 16 |
| lg | 24 |
| xl | 32 |
| 2xl | 48 |
| 3xl | 64 |

**Page gutters**

| Breakpoint | Side padding |
|---|---|
| Mobile | 16 |
| Tablet | 24 |
| Desktop | 40–64 (content max-width; see §2) |

### 1.4 Border radius (UI assumption)

| Element | Radius | Notes |
|---|---|---|
| Buttons | 8 | Soft boutique, not pill-heavy |
| Product cards | 12 | Image + card container |
| Category tiles | Fully circular **or** highly rounded (PDF: “circular/rounded”) | Prefer circular image + label below |
| Badges | 6–999 (pill for UPI) | Match screenshot hierarchy lightly |
| Inputs | 8 | Admin forms |
| WhatsApp FAB | 999 | Floating circle |

Avoid default “AI purple pill” excess; keep radius calm.

### 1.5 Shadows (UI assumption)

Keep shadows **minimal** so photography stays primary.

| Token | Spec |
|---|---|
| none | Default for most surfaces |
| soft | `0 2px 8px rgba(44,44,44,0.06)` — optional card lift |
| sticky | `0 -2px 12px rgba(44,44,44,0.08)` — mobile PDP sticky CTA bar |

No multi-layer glow.

### 1.6 Buttons

| Variant | Style | Status |
|---|---|---|
| Primary | Fill `#D4A373`, label high-contrast, weight 600 | PDF-explicit for Shop Now / primary actions |
| Secondary | Outline `#D4A373` or charcoal outline | PDF: Buy via WhatsApp as prominent secondary |
| Tertiary / text | Charcoal text, no fill | UI assumption (Follow on Instagram can be primary or outline) |
| Icon button | 40–44px hit target, charcoal icons | Header Search / Wishlist / Cart |

**States (UI assumption):** default, hover (desktop), pressed, disabled, loading.

**Mobile:** sticky bar buttons full-width or split 50/50 (Add to Cart primary + WhatsApp secondary).

### 1.7 Badges

| Badge | Content | Placement | Status |
|---|---|---|---|
| Discount % | e.g. `18% off` | Product card / PDP near price | PDF-explicit |
| UPI | `UPI` | Product listing cards | PDF-explicit (example on PLP) |
| Pay Later | `with Pay Later` (+ optional lower amount) | PDP pricing | PDF-explicit |
| Cart counter | numeric | On cart icon | PDF-explicit |
| Active category | terracotta highlight | Category tile selected | PDF-explicit |

**CONFIRMED REQUIRED checkout methods:** **UPI**, **Credit/Debit Card**, **Net Banking**, and **COD**. **Gateway/provider PENDING** — do not invent branding. **COD additional charge** amount/rule **PENDING** — do not invent a fixed fee. Additional methods may be added later if requested. PDF badges above remain visual language; checkout payment UX frames are FUTURE.

### 1.8 Cards

**Product card (PDF-explicit content)**

- Product photo
- Title
- Discounted price vs MRP
- Percentage off badge
- Add to Cart (homepage Bestsellers & Featured; PLP card CTA is implied by e-commerce listing — PDF PLP emphasizes pricing hierarchy; Add to Cart is explicit on homepage and PDP)

**PLP card pricing hierarchy (PDF example):** `₹124` `₹152` `18% off` + UPI badge.

**Rating on cards:** screenshot-observed; PDF requires rating on PDP. Show rating on PLP cards only if confirmed; otherwise PDP-only. Mark PLP rating as **proposed / screenshot-informed**.

**Category card / tile**

- Circular/rounded image
- Category name under tile
- Five total

### 1.9 Icons (PDF-explicit presence; style UI assumption)

| Icon | Location |
|---|---|
| Search | Header |
| Wishlist (heart) | Header |
| Cart | Header + counter |
| WhatsApp | Floating widget; PDP secondary CTA may include WhatsApp mark |
| Instagram | Social proof / Follow CTA |

Icon set (outline vs filled): **CLIENT CONFIRMATION REQUIRED**. Use simple outline charcoal for Figma drafts.

### 1.10 Inputs (Admin + future forms)

| Property | Spec | Status |
|---|---|---|
| Height | 44–48 | UI assumption (touch-friendly) |
| Border | 1px subtle | UI assumption |
| Focus | Primary ring `#D4A373` | UI assumption |
| Label | Above field, 14/600 | UI assumption |
| Error | Red helper text | UI assumption |

### 1.11 Navigation

**Customer header (PDF-explicit)**

- Left: Logo `MA CREATIONS` (text until logo asset arrives)
- Right: Search, Wishlist, Cart + counter

**Admin navigation (UI assumption)**

- Simple left sidebar or top bar: Dashboard, Add Product
- Non-technical labels; no dense enterprise nav

Search / Wishlist destinations: **CLIENT CONFIRMATION REQUIRED** (icons present; pages not wireframed).

### 1.12 Responsive breakpoints (UI assumption; mobile-first)

| Name | Min width | Figma role |
|---|---|---|
| Mobile | 0–767 | Primary design |
| Tablet | 768–1023 | Optional bridge |
| Desktop | 1024+ | Second required set |

PDF requires Mobile **and** Desktop for homepage; PLP/PDP must also be responsive.

---

## 2. Responsive Frames

Recommended Figma frame sizes (mobile-first):

| Device | Frame name | Size (W × H) | Notes |
|---|---|---|---|
| Mobile | `Mobile / 390` | **390 × 844** | Primary; design all customer flows here first |
| Tablet | `Tablet / 768` | **768 × 1024** | Optional; validate 2-col → multi-col |
| Desktop | `Desktop / 1440` | **1440 × 1024** | Content max-width **1200** centered (UI assumption) |

**Admin frames**

| Screen | Recommended |
|---|---|
| Admin Login | Desktop 1440 (also Mobile 390 if owner uses phone) |
| Dashboard / Add Product | Desktop **1440** primary (CMS usability); optional tablet |

**Design order in Figma**

1. Mobile customer screens  
2. Desktop customer screens  
3. Admin desktop  

---

## 3. Screen Inventory

### 3.1 Confirmed v1 screens

**CUSTOMER**

| # | Screen | Frames |
|---|---|---|
| 1 | Homepage — Mobile | 390 |
| 2 | Homepage — Desktop | 1440 |
| 3 | Category / Product Listing (PLP) — Mobile | 390 |
| 4 | Category / Product Listing (PLP) — Desktop | 1440 |
| 5 | Product Detail (PDP) — Mobile | 390 + sticky CTA |
| 6 | Product Detail (PDP) — Desktop | 1440 |

**ADMIN**

| # | Screen | Frames |
|---|---|---|
| 7 | Admin Login | 1440 (+ optional 390) |
| 8 | Admin Dashboard | 1440 |
| 9 | Add Product | 1440 |

### 3.2 Explicitly excluded from v1 Figma (FUTURE / CLIENT CONFIRMATION REQUIRED)

Do **not** design as approved screens:

- Cart page
- Checkout
- Payment
- Order confirmation
- Customer login / register / account
- My Orders
- Shipping / tracking
- Search results
- Wishlist page
- Product edit / delete admin screens

If needed for stakeholder discussion only, place on a Figma page titled **`99 — FUTURE / Unconfirmed`** with a red “NOT IN V1” stamp.

---

## 4. Homepage Specification

### 4.1 Homepage — Mobile (390)

**Overall**

- Background `#FDFBF7`
- Vertical scroll
- Floating WhatsApp widget fixed bottom-right (above safe area; clear of sticky elements)
- Section order (PDF-explicit): Header → Hero → Category tiles → Bestsellers & Featured → Instagram → Footer

#### Header

| Aspect | Spec |
|---|---|
| Layout | Single row, height ~56–64 |
| Left | Logo text **MA CREATIONS** (or logo asset when provided) |
| Right | Search icon, Wishlist icon, Cart icon + counter badge |
| Alignment | Logo left; icons right, 8–12 gap |
| Spacing | Horizontal padding 16 |
| Behavior | Sticky header **UI assumption** (not PDF-explicit; recommended for mobile social traffic) |
| CTA | Icons only; destinations for Search/Wishlist **CLIENT CONFIRMATION REQUIRED**; Cart icon updates counter on Add to Cart |

#### Hero carousel

| Aspect | Spec |
|---|---|
| Layout | Full-bleed width within frame; aspect ~16:9 or 4:5 **UI assumption** |
| Content | Images highlighting **home decor, festive candles, sleek water bottles** |
| Components | Image slides, pagination dots, **Shop Now** button `#D4A373` |
| Spacing | Section below header; CTA overlaid or below image — prefer CTA on image lower third without clutter **UI assumption** |
| Alignment | CTA centered or left-aligned in content area |
| Responsive | Full width mobile |
| CTA behavior | Shop Now → enter shop (**destination PENDING** — do not assume category/all/campaign) |

#### Five category tiles

| Aspect | Spec |
|---|---|
| Layout | Horizontal **scroll** (preferred on mobile) **or** wrap grid — PDF allows either |
| Components | 5 circular/rounded image tiles + labels |
| Content | 1. Hydration & Drinkware 2. Lunch & Meal Prep 3. Kitchen Gadgets & Prep 4. Storage & Kitchenware 5. Home Decor & Festivity |
| Spacing | Tile size ~72–88px image; gap 12–16; section title optional **UI assumption** |
| Alignment | Left-edge padded scroll |
| Behavior | Tap tile → PLP for that category |
| Active state | Terracotta highlight when on that category (more relevant on PLP return) |

#### Bestsellers & Featured grid

| Aspect | Spec |
|---|---|
| Layout | **Clean 2-column** product grid |
| Components | Product cards |
| Content | Product photo, title, discounted price vs MRP, % off badge, **Add to Cart** |
| Spacing | Gap 12–16; section padding 16–24 |
| Alignment | Equal column widths |
| Behavior | Card tap → PDP; Add to Cart → increments header cart counter (cart page not in v1) |
| How products are chosen | **CLIENT CONFIRMATION REQUIRED** (manual flags vs all vs sales) |

#### Product card (homepage)

See §8 Product Card. Homepage requires Add to Cart on card (PDF-explicit).

#### Instagram / social proof

| Aspect | Spec |
|---|---|
| Layout | Grid of recent posts (2×N or 3×N **UI assumption**) |
| Content | Live posts from Instagram — profile **CONFIRMED** `https://www.instagram.com/macreations.living/` (grid tech PENDING) |
| Components | Instagram post cards + **Follow on Instagram** CTA |
| Spacing | Section gap 24+ |
| Behavior | Follow CTA → Instagram profile URL (**CLIENT CONFIRMATION REQUIRED**). Post tap behavior **CLIENT CONFIRMATION REQUIRED** |

#### Footer

| Aspect | Spec |
|---|---|
| Layout | Stacked: quick links, return policy, payment trust badges |
| Content | Quick links (**targets CLIENT CONFIRMATION REQUIRED**), Return policy link (**content CLIENT CONFIRMATION REQUIRED**), payment trust badges (**which badges CLIENT CONFIRMATION REQUIRED**) |
| Spacing | Generous padding 24–32 |
| WhatsApp | Floating widget for inquiries (not in footer column only — PDF: footer area + floating widget) |

#### WhatsApp floating widget

| Aspect | Spec |
|---|---|
| Layout | Fixed circle FAB, bottom-right |
| Content | WhatsApp icon |
| Behavior | Opens WhatsApp chat for inquiries (**number CLIENT CONFIRMATION REQUIRED**) |
| Z-index | Above content; not covering Shop Now permanently |

---

### 4.2 Homepage — Desktop (1440)

Same sections and content as mobile. Differences:

| Section | Desktop behavior |
|---|---|
| Header | Wider; logo left; icons right; optional max-width content bar |
| Hero | Full-bleed or max-width hero; larger Shop Now; carousel arrows **UI assumption** |
| Categories | Prefer **row/grid of 5** visible without scroll |
| Bestsellers | Still readable; may expand to **3–4 columns** on desktop while keeping mobile 2-col as source of truth — **CLIENT CONFIRMATION REQUIRED** if departing from 2-col. Safe default: keep **2-col** to match PDF, or 4-col only if client approves |
| Instagram | Wider grid (3–6 tiles) |
| Footer | Multi-column quick links |
| WhatsApp FAB | Same floating behavior |

**UI assumption:** Main content max-width ~1200 centered on cream background.

---

## 5. Category / PLP Specification

### 5.1 Shared (Mobile + Desktop)

#### Header

Same customer header as Homepage (PDF-explicit icons).

#### Breadcrumb

| Status | Spec |
|---|---|
| **UI assumption** | `Home / {Category Name}` — helpful, not PDF-explicit |
| Optional | Omit if client wants minimal chrome |

#### Category title

- H1 = category name (one of the 5)
- Alignment: left, padding 16 (mobile) / within max-width (desktop)

#### Product count

| Status | Spec |
|---|---|
| **Proposed** | “{n} products” under title | **CLIENT CONFIRMATION REQUIRED** / optional |

#### Sort (PDF-explicit)

Dropdown / select with:

1. Price: Low to High  
2. Price: High to Low  
3. Newest arrivals  

Place in a **Sort & Filter bar** (PDF names this bar).

#### Filter

| Status | Spec |
|---|---|
| PDF | Names “Sort & Filter Bar” but **only specifies sort options** |
| Additional filters (price range, rating, gender, sub-category) | **Do not add** unless **CLIENT CONFIRMATION REQUIRED** / FUTURE |
| Figma | Label the control group “Sort”; if a Filter chip is shown, stamp **NOT CONFIRMED** |

#### Product grid

- Responsive grid of product cards
- Mobile: **2 columns** (align with homepage / screenshot aesthetic)
- Desktop: 3–4 columns **UI assumption** (confirm if needed)

#### Product card (PLP)

| Element | Status |
|---|---|
| Photo | PDF-explicit (grid items) |
| Title | PDF-explicit |
| Selling price | PDF-explicit |
| MRP (strikethrough) | PDF-explicit |
| Discount % | PDF-explicit |
| UPI badge | PDF-explicit (example) |
| Rating | Screenshot-informed; **proposed for PLP** — PDF requires on PDP |
| Add to Cart on PLP card | Homepage yes; PLP not explicit — **proposed**; card click → PDP is safe |
| Wishlist heart on card | Screenshot-observed; **CLIENT CONFIRMATION REQUIRED** |

#### Add to Cart behavior (PLP)

- Prefer: tap card → PDP; Add to Cart on homepage + PDP confirmed  
- If PLP includes Add to Cart button: same as homepage (increment badge only; no cart page)

---

### 5.2 PLP — Mobile

- Sort bar full width under title  
- 2-column grid, gap 12  
- Sticky header optional  

### 5.3 PLP — Desktop

- Title + sort on one row  
- Wider grid  
- Same pricing hierarchy  

---

## 6. Product Detail Page Specification

### 6.1 Confirmed requirements (PDF-explicit)

| Element | Spec |
|---|---|
| Header | Same storefront header |
| Image gallery | High-resolution **zoomable** main image |
| Thumbnails | Underneath main image |
| Title | Clear product title |
| Rating | Star rating e.g. `4.0 ★` |
| Rating count | e.g. `[135 ratings]` |
| Dynamic pricing | Selling price + hierarchy |
| Pay Later tag | “with Pay Later” |
| Primary CTA | **Add to Cart** |
| Secondary CTA | **Buy via WhatsApp** |
| Mobile layout | **Sticky bottom CTA bar** |

### 6.2 UI assumptions

| Element | Assumption |
|---|---|
| Desktop layout | Two-column: gallery left (~50%), details right |
| Mobile layout | Gallery on top, details below, sticky CTA bottom |
| Zoom | Tap/pinch or hover lens; lightbox on tap |
| Discount + MRP on PDP | Show same hierarchy as cards (consistent commerce UI) |
| Sticky bar contents | Primary Add to Cart + Secondary Buy via WhatsApp (PDF actions; bar contents not itemized) |

### 6.3 CLIENT CONFIRMATION REQUIRED

| Item | Why |
|---|---|
| Product description / specs | Not in PDF |
| Variants | Not in PDF |
| Stock status | Not in PDF |
| Pay Later amount vs label-only | Display vs real BNPL |
| UPI on PDP | PDF example is on PLP; PDP stresses Pay Later |
| WhatsApp number + message template | Needed for Buy via WhatsApp |
| Whether Add to Cart opens a cart | Cart page unconfirmed |
| Related products | Not in PDF |
| Share button | Not in PDF |

### 6.4 PDP — Mobile

1. Header  
2. Main image (full width)  
3. Thumbnail row horizontal scroll  
4. Title  
5. Rating + count  
6. Price block: selling, MRP, % off, Pay Later tag  
7. Spacer for sticky bar  
8. Sticky bottom: Add to Cart (primary) | Buy via WhatsApp (secondary)  
9. Footer below fold; WhatsApp FAB may hide or sit above sticky bar — **UI assumption:** hide FAB on PDP when sticky bar present to avoid collision, or offset FAB — confirm in prototype review  

### 6.5 PDP — Desktop

- No sticky bottom bar required by PDF (sticky is mobile architecture)  
- Primary + Secondary buttons inline under price  
- Zoom on main image hover/click  

---

## 7. Admin UI

Goal: **Shopify/Woo-like simplicity** for a non-developer (PDF usability requirement; custom CMS).

**Admin visual theme:** same cream/terracotta **or** plain functional — **CLIENT CONFIRMATION REQUIRED**. Default Figma: light functional white/cream with primary `#D4A373` buttons so brand stays consistent without decorating the CMS.

### 7.1 Admin Login

| Element | Spec | Status |
|---|---|---|
| Screen purpose | Owner logs in without coding | PDF-explicit |
| Identifier field | Email **or** username | **CLIENT CONFIRMATION REQUIRED** which |
| Password | Password input | Necessary for login concept; mechanism not detailed in PDF |
| Login button | Primary `#D4A373` | UI assumption |
| Validation | Empty fields, invalid credentials | UI assumption |
| Forgot password | Not in PDF | **CLIENT CONFIRMATION REQUIRED** / FUTURE |
| Branding | Small MA CREATIONS wordmark | UI assumption |

**States:** empty, filled, error, loading, success → Dashboard.

### 7.2 Admin Dashboard

PDF names **“Dashboard Overview”** but specifies **no widgets**.

| Element | Spec | Status |
|---|---|---|
| Simple overview | Welcome + primary action **Add New Product** | UI assumption to make overview usable |
| Nav | Dashboard, Add Product | UI assumption |
| Widgets (sales, orders, stock) | Not specified | **CLIENT CONFIRMATION REQUIRED** — do not invent charts |
| Product list table | Not specified | **CLIENT CONFIRMATION REQUIRED** |

**Figma v1 dashboard:** left sidebar + main panel with:

- Heading “Dashboard”
- Short helper text: “Add and manage your catalog”
- Large button **Add New Product**
- Optional empty-state illustration  

Do not show Orders/Revenue unless confirmed.

### 7.3 Add Product

**Confirmed fields only (PDF-explicit)**

| Field | Control |
|---|---|
| Product Title | Text input |
| Category | Dropdown linked to **exactly 5** categories |
| Image Upload | Upload control |
| Selling Price | Number input (INR) |
| MRP | Number input (INR) |

| Element | Spec |
|---|---|
| Submit | “Save Product” / “Add Product” primary button |
| Cancel | Secondary — UI assumption |
| Success | Toast or return to dashboard with confirmation — UI assumption |

**Do not** mark as mandatory in Figma: description, SKU, stock, variants, Pay Later price, multiple images (multi-image is **CLIENT CONFIRMATION REQUIRED**; PDF says Image Upload singular).

**Validation UI assumptions:** title required; category required; selling ≤ MRP warning; image required.

---

## 8. Component Library

For each component: variants, states, desktop/mobile behavior.

### Header

| | |
|---|---|
| Variants | Customer default |
| States | Default; scrolled/sticky (assumption) |
| Mobile | Compact icons |
| Desktop | Same structure, more padding |

### Footer

| | |
|---|---|
| Variants | Default |
| States | Default |
| Mobile | Stacked |
| Desktop | Multi-column |

### Button

| Variants | Primary, Secondary, Tertiary, Icon |
| States | Default, Hover, Pressed, Disabled, Loading |
| Mobile | Min height 44; sticky full-width groups |
| Desktop | Inline; hover enabled |

### Product Card

| Variants | Homepage (with Add to Cart), PLP (price hierarchy + UPI) |
| States | Default, Hover (desktop lift), Pressed |
| Mobile | 2-col width |
| Desktop | Grid cell in 2–4 col |

### Category Card / Tile

| Variants | Default, Active (terracotta) |
| States | Default, Pressed |
| Mobile | Horizontal scroll |
| Desktop | 5 in a row |

### Price Display

| Variants | Card compact, PDP large |
| Content | Selling + MRP strikethrough |
| Mobile/Desktop | Scale typography per §1.2 |

### Discount Badge

| Variants | Text `% off` or chip |
| States | Visible when MRP > selling |

### Rating

| Variants | Stars + numeric; with count |
| PDP | Required |
| PLP | Optional / proposed |

### UPI Badge

| Variants | Small pill `UPI` |
| PLP | PDF example |
| Semantics | Display vs payment — confirmation |

### Pay Later Badge

| Variants | Text tag; optional price amount |
| PDP | Required display |
| Semantics | Confirmation |

### Search / Wishlist / Cart Icons

| States | Default; Cart with badge `0–9+` |
| Mobile/Desktop | 44px hit area |
| Navigation targets | Confirmation for Search/Wishlist; Cart page confirmation |

### Filter

| v1 | **Not confirmed** — do not ship component as required |
| FUTURE | Stamp if drawn |

### Sort Dropdown

| Variants | Closed, Open |
| Options | Price L→H, Price H→L, Newest |
| Mobile | Full-width select |
| Desktop | Inline select |

### Image Gallery

| Variants | PDP mobile stack; PDP desktop side-by-side |
| States | Thumb selected; zoom active |
| Mobile | Thumb row under main |
| Desktop | Main + thumbs under or vertical |

### WhatsApp CTA

| Variants | FAB (inquiry); Button “Buy via WhatsApp” |
| States | Default, Pressed |
| Mobile | Sticky + FAB rules |
| Desktop | Inline secondary button |

### Instagram Card

| Variants | Post thumbnail |
| States | Default |
| Grid | 2-col mobile / wider desktop |

### Form Input

| Variants | Text, Password, Number |
| States | Default, Focus, Error, Disabled |
| Admin | Primary use |

### Category Dropdown

| Options | Exactly 5 PDF categories |
| States | Closed, Open, Error |

### Image Upload

| States | Empty, Uploading, Preview, Error |
| Mobile admin | Full width |

### Admin Sidebar

| Items | Dashboard, Add Product |
| States | Active route terracotta |
| Desktop | Persistent left nav |
| Mobile admin | Hamburger **UI assumption** if admin on phone |

---

## 9. Prototype / User Flow

### 9.1 Catalog browse → Add to Cart (confirmed path)

```
SOCIAL MEDIA (Instagram / Facebook / WhatsApp link)
    ↓
HOMEPAGE
    ↓
CATEGORY TILE
    ↓
PLP (sort optional)
    ↓
PDP
    ↓
ADD TO CART  →  header cart counter increments
```

**Note:** Stop here for v1 prototype. **No checkout frame.**

Optional hotspot: Homepage product card **Add to Cart** → counter updates without leaving page.

### 9.2 WhatsApp buy path (confirmed)

```
SOCIAL MEDIA
    ↓
HOMEPAGE  (or deep link to PDP — if used)
    ↓
PDP
    ↓
BUY VIA WHATSAPP
    ↓
WHATSAPP (external; Figma may use a simple “External: WhatsApp” overlay frame)
```

Also: any page → **WhatsApp FAB** → WhatsApp inquiry (no product).

### 9.3 Instagram path (confirmed)

```
HOMEPAGE → Instagram grid / Follow on Instagram → Instagram (external)
```

### 9.4 Admin path (confirmed)

```
ADMIN LOGIN
    ↓
DASHBOARD
    ↓
ADD PRODUCT
    ↓
PRODUCT CREATED (success state / return to dashboard)
```

### 9.5 Do not prototype

Checkout → Payment → Order Confirmation → My Orders.

---

## 10. Figma Page Structure

Recommended Figma file pages:

| Page | Contents |
|---|---|
| **01 — Cover** | Project name, “UI/UX Spec v1”, date, mobile-first note |
| **02 — Requirements** | Links/summary of PDF screens 1–4; legend PDF vs confirmation |
| **03 — User Flow** | Flows from §9 as FigJam-style or arrow frames |
| **04 — Design System** | Colors, type (both font options), spacing, radii, shadows |
| **05 — Components** | Component library from §8 |
| **06 — Customer Mobile** | Home, PLP, PDP (390) |
| **07 — Customer Desktop** | Home, PLP, PDP (1440) |
| **08 — Admin** | Login, Dashboard, Add Product |
| **09 — Prototype** | Connected hotspots for §9 only |
| **99 — FUTURE / Unconfirmed** | Optional cart/checkout/search — stamped not in v1 |

---

## 11. Developer Handoff

### 11.1 Homepage

| Item | Detail |
|---|---|
| Components | Header, HeroCarousel, CategoryTile×5, ProductCard, InstagramGrid, Footer, WhatsAppFab |
| Responsive | Mobile 2-col featured; desktop wider layout; categories scroll→row |
| Interactions | Carousel, Shop Now, tile→PLP, Add to Cart→badge++, Follow, WhatsApp |
| API (later) | `GET /categories`, `GET /products?featured|bestseller`, Instagram feed/proxy, site settings |
| Assets | Logo, 3+ hero images, 5 category images, product images, Instagram (live) |
| Backend dependency | Catalog + optional CMS flags for featured; Instagram integration |

### 11.2 PLP

| Item | Detail |
|---|---|
| Components | Header, CategoryTitle, SortDropdown, ProductCard, Footer, WhatsAppFab |
| Responsive | 2-col mobile; multi-col desktop |
| Interactions | Sort change reorders grid; card→PDP |
| API | `GET /products?category=&sort=price_asc\|price_desc\|newest` |
| Assets | Product images |
| Backend | Category + product list + sort |

### 11.3 PDP

| Item | Detail |
|---|---|
| Components | Header, ImageGallery, PriceDisplay, Rating, PayLaterBadge, Button primary/secondary, StickyCtaBar (mobile), Footer, WhatsAppFab |
| Responsive | Stack + sticky mobile; split desktop |
| Interactions | Thumb select, zoom, Add to Cart, Buy via WhatsApp |
| API | `GET /products/{slug}`; WhatsApp link helper |
| Assets | Multi images per product (**count CLIENT CONFIRMATION REQUIRED**) |
| Backend | Product detail; WhatsApp number config |

### 11.4 Admin Login / Dashboard / Add Product

| Item | Detail |
|---|---|
| Components | FormInput, Button, CategoryDropdown, ImageUpload, AdminSidebar |
| Responsive | Desktop-first CMS |
| Interactions | Login; navigate Add Product; upload; save |
| API | Admin auth; `POST /admin/products`; image upload; `GET /admin/categories` |
| Assets | None beyond brand mark |
| Backend | Spring Security + product create |

---

## 12. Missing Assets

Checklist to collect from the client. **Do not invent.**

| Asset | Status |
|---|---|
| Logo (final file) | CLIENT CONFIRMATION REQUIRED |
| Favicon / share image | CLIENT CONFIRMATION REQUIRED |
| Brand font choice: Inter **or** Montserrat | CLIENT CONFIRMATION REQUIRED |
| Hero / banner images (home decor, festive candles, water bottles) | Partially provided |
| Shop Now destination | **PENDING** |
| Instagram profile / Follow URL | **CONFIRMED** `https://www.instagram.com/macreations.living/` |
| WhatsApp Business number | **CONFIRMED** `6395700831` (intl format = config verify) |
| Five category tile / banner images | Provided for all 5 |
| Complete product catalog (titles) | PENDING |
| Product images | Admin upload only — no fake catalog images |
| Selling prices | PENDING (partial screenshot prices for reference only) |
| MRP values | PENDING |
| Ratings + rating counts (source: Meesho import vs new) | PENDING |
| Buy via WhatsApp message template | PENDING |
| Footer quick link list | PENDING |
| Return policy content | PENDING |
| Payment trust badge set | PENDING (required methods: UPI, Card, Net Banking, COD — FINAL; gateway + COD charge PENDING) |
| Admin login identifier (email vs username) | PENDING |
| Bestsellers / Featured selection rules | **PENDING** |

---

## 13. Traceability review

| Spec section | Traceability |
|---|---|
| Colors / type weights / mobile-first / sticky PDP CTA | PDF §1 |
| Home / PLP / PDP / CMS screens | PDF §3 |
| Five categories | PDF §4 |
| UPI on PLP, Pay Later on PDP, WhatsApp, Instagram | PDF §3 |
| Spacing, radii, shadows, breakpoints, sticky header, breadcrumbs, dashboard widgets | UI assumption or confirmation — marked |
| Checkout / guest + account / order tracking / extra filters / Gender | FINAL or FUTURE / PENDING mechanism design — see CLIENT_CONFIRMATIONS |
| Inter vs Montserrat | Left open — CLIENT CONFIRMATION REQUIRED |

This document is ready for designers to build the Figma file structure in §10 without implementing application code.

# MA CREATIONS — Database Review

**Purpose:** Final review of the proposed **CURRENT V1** database against PDF requirements, before SQL/migrations for the catalog freeze.

**Status note (post client clarification):** This review covers the **four-entity CURRENT V1** model only. **FINAL confirmed** commerce (cart, wishlist, orders, **guest checkout** + optional **customer accounts**, payment methods **UPI** / **Credit/Debit Card** / **Net Banking** / **COD**, hide/unpublish) is approved business scope for a later phase — **not** designed as DDL in this review. Gateway/provider, **COD charge rule**, and auth/tracking mechanisms **PENDING**. See `CLIENT_CONFIRMATIONS.md`.

**Sources reviewed**

- `REQUIREMENTS.md`
- `DATABASE_ER_DESIGN.md`
- `CLIENT_CONFIRMATIONS.md`
- `FIGMA_SPECIFICATION.md`
- `SYSTEM_ARCHITECTURE.md`

**Scope:** Only the four V1 entities — `Category`, `Product`, `ProductImage`, `AdminUser`.

**No SQL. No application code.**

**Verdict summary:** The four-entity model remains aligned with CURRENT V1 (catalog + admin create + rating display). Required checkout methods **UPI**, **Credit/Debit Card**, **Net Banking**, and **COD** are **CONFIRMED**; gateway/provider and **COD charge rule** are **PENDING** — do **not** invent payment tables, a gateway, or a COD fee here. Featured/bestseller selection rules and Shop Now destination remain **PENDING**.

---

## Review method

| Classification | Meaning |
|---|---|
| **Confirmed** | Required by PDF / confirmed storefront or CMS behavior |
| **Structural** | Needed to implement a confirmed feature (PK, FK, sort key, URL key, timestamps); not a new business feature |
| **Proposed** | Optional / nice-to-have; not required to meet PDF |
| **Client confirmation** | Cannot finalize meaning, nullability, or inclusion until client answers |

---

# 1. Category

**Entity purpose:** Hold the five fixed taxonomy categories for homepage tiles, PLP, and the admin Category dropdown.

### Primary key

| Key | Type concept | Required |
|---|---|---|
| `id` | Surrogate PK | Required (structural) |

### Foreign keys

None.

### Relationships

| Relationship | Cardinality | Status |
|---|---|---|
| Category → Product | One-to-many | Confirmed |

---

### Field-by-field

| Field | Confirmed? | Proposed? | Client confirmation? | Required / Optional | Why it exists |
|---|---|---|---|---|---|
| `id` | Structural | — | No | **Required** | Primary key |
| `name` | **Yes** (PDF five category names) | — | Exact spelling fixed by PDF; changing set needs confirmation (§12.6) | **Required** | Display name for tiles, PLP title, CMS dropdown |
| `slug` | Structural | — | Exact slug strings not in PDF | **Required** | Stable PLP/API path (`/category/{slug}`) |
| `display_order` | Structural | — | No (PDF order 1–5 is clear) | **Required** | Preserve PDF category order on homepage |
| `tile_image_url` | — | **Yes** | Yes — CMS tile upload not in PDF; may be static assets | **Optional** | Homepage circular/rounded tiles need images |
| `focus_notes` | — | **Yes** | Yes — subcategories navigable? (§5.11) | **Optional** | Hold PDF “Sub-Categories / Focus” text without a SubCategory table |
| `created_at` | Structural | — | No | **Required** | Audit / seed tracking |
| `updated_at` | Structural | — | No | **Required** | Audit |

### Confirmed fields (Category)

- `name` (business)
- `id`, `slug`, `display_order`, `created_at`, `updated_at` (structural)

### Proposed fields (Category)

- `tile_image_url`
- `focus_notes`

### Fields requiring client confirmation (Category)

- Whether category tile images are DB-backed vs static frontend assets  
- Whether a 6th category may ever be added (`CLIENT_CONFIRMATIONS` §12.6)  
- Whether focus text is stored, ignored, or later becomes navigable subcategories  

---

# 2. Product

**Entity purpose:** Catalog item for homepage / PLP / PDP and admin Add Product (title, category, selling price, MRP).

### Primary key

| Key | Required |
|---|---|
| `id` | **Required** (structural) |

### Foreign keys

| FK | References | Required / Optional | Status |
|---|---|---|---|
| `category_id` | `Category.id` | **Required** | Confirmed (CMS category dropdown) |
| `created_by_admin_id` | `AdminUser.id` | **Optional** | Proposed audit only |

### Relationships

| Relationship | Cardinality | Status |
|---|---|---|
| Product → Category | Many-to-one | Confirmed |
| Product → ProductImage | One-to-many | Confirmed |
| Product → AdminUser (creator) | Many-to-one | Proposed (nullable audit) |

---

### Field-by-field

| Field | Confirmed? | Proposed? | Client confirmation? | Required / Optional | Why it exists |
|---|---|---|---|---|---|
| `id` | Structural | — | No | **Required** | Primary key |
| `title` | **Yes** (CMS Product Title) | — | No | **Required** | PLP/PDP/cards + CMS |
| `category_id` | **Yes** | — | Must be one of five | **Required** | Taxonomy assignment |
| `selling_price` | **Yes** (CMS Selling Price) | — | Currency/GST inclusion open | **Required** | Discounted price on cards/PDP |
| `mrp` | **Yes** (CMS MRP) | — | Same | **Required** | Strikethrough MRP; drives % off |
| `slug` | Structural | — | Generation rules not in PDF | **Required** for clean URLs | PDP/API identity (`/product/{slug}`) — see special notes |
| `created_at` | Structural + **sort need** | — | No | **Required** | PLP **Newest arrivals** sort (PDF-explicit) |
| `updated_at` | Structural | — | No | **Required** | Audit |
| `average_rating` | **Display need yes** | — | **Yes** — data source (§10.1) | **Optional at row level** (nullable until data exists) | PDP star rating (PDF example 4.0★) |
| `rating_count` | **Display need yes** | — | **Yes** — source + null vs 0 | **Optional at row level** or default 0 | PDP rating count (PDF example 135) |
| `created_by_admin_id` | — | **Yes** | Whether to keep audit FK | **Optional** | Who created the product |
| `percent_off` | **Derived — do not store** | — | No | N/A | Compute `(mrp - selling_price) / mrp` |
| `pay_later_price` | — | If stored | **Yes** (§6.2–6.3) | Do not add until confirmed | PDP “with Pay Later” — see special notes |
| `upi_badge_enabled` | — | If stored | **Yes** (§6.1) | Do not add until confirmed | PLP UPI badge — see special notes |
| `is_bestseller` / `is_featured` | — | **Yes** | **Yes** (§5.10) | Do not add until confirmed | Homepage Bestsellers & Featured — see special notes |
| `description`, `sku`, `stock`, `status` | — | — | **Yes** | Excluded from freeze | Not in CMS form / PDF |

### Confirmed fields (Product)

**Business (PDF CMS / storefront):**

- `title`
- `category_id`
- `selling_price`
- `mrp`
- Rating **display capability** via `average_rating` + `rating_count` (UI confirmed; population model not)

**Structural / sort:**

- `id`
- `slug`
- `created_at`
- `updated_at`

### Proposed fields (Product)

- `created_by_admin_id`
- Any future `is_bestseller` / `is_featured`
- Any future `pay_later_price` / `upi_badge_enabled`

### Fields requiring client confirmation (Product)

- How ratings are populated (Meesho import vs manual vs later on-site reviews)  
- Whether rating fields are nullable or default to 0  
- Pay Later: store price, calculate, label-only, or omit  
- UPI: always-on UI vs per-product flag vs omit on independent site  
- Bestsellers/Featured selection rules and whether flags are needed  
- Product active/hidden without edit UI (§12.1)  
- Description, SKU, stock, variants (excluded until confirmed)  

---

# 3. ProductImage

**Entity purpose:** Persist CMS image upload and support PDP gallery (main + thumbnails) and card primary photo.

### Primary key

| Key | Required |
|---|---|
| `id` | **Required** |

### Foreign keys

| FK | References | Required | Status |
|---|---|---|---|
| `product_id` | `Product.id` | **Required** | Confirmed |

### Relationships

| Relationship | Cardinality | Status |
|---|---|---|
| ProductImage → Product | Many-to-one | Confirmed |

---

### Field-by-field

| Field | Confirmed? | Proposed? | Client confirmation? | Required / Optional | Why it exists |
|---|---|---|---|---|---|
| `id` | Structural | — | No | **Required** | Primary key |
| `product_id` | **Yes** | — | No | **Required** | Belongs to product |
| `storage_path` / `url` | **Upload need yes** | — | **Yes** — disk vs S3 (§14.3); exact column naming | **Required** | Location of uploaded file |
| `sort_order` | Structural | — | No | **Required** | Primary image = 0 for card + PDP main; others thumbnails |
| `alt_text` | — | **Yes** | No (optional a11y) | **Optional** | Accessibility |
| `created_at` | Structural | — | No | **Required** | Audit |

### Confirmed fields (ProductImage)

- `product_id`
- Storage reference field (path or URL — **backend choice pending confirmation**)
- `id`, `sort_order`, `created_at` (structural)

### Proposed fields (ProductImage)

- `alt_text`

### Fields requiring client confirmation (ProductImage)

- Image storage strategy: server disk vs S3-compatible (`CLIENT_CONFIRMATIONS` §14.3)  
- Max images per product (`§12.2`) — entity supports 1..N; CMS form says singular upload  
- Whether multi-image is in scope for v1 admin UI or only one image at create  

**Storage strategy review (special attention):**

| Approach | Fits V1? | Notes |
|---|---|---|
| Store relative `storage_path` on disk | Safe default for small site | Matches `SYSTEM_ARCHITECTURE` proposal |
| Store full public `url` | Also fine | Better if CDN/object storage confirmed |
| Hybrid (path + CDN base in config) | Fine | Path in DB; base URL in config |

**Decision for review:** Keep **one NOT NULL string field** for the file reference. Do **not** invent a separate CDN entity. Finalize disk vs cloud before migrations.

---

# 4. AdminUser

**Entity purpose:** Allow the non-technical owner to log into the CMS and create products.

### Primary key

| Key | Required |
|---|---|
| `id` | **Required** |

### Foreign keys

None (AdminUser is parent of optional product audit FK).

### Relationships

| Relationship | Cardinality | Status |
|---|---|---|
| AdminUser → Product (created_by) | One-to-many | Proposed optional audit |

---

### Field-by-field

| Field | Confirmed? | Proposed? | Client confirmation? | Required / Optional | Why it exists |
|---|---|---|---|---|---|
| `id` | Structural | — | No | **Required** | Primary key |
| `login_identifier` | **Login need yes** | — | **Yes** — email vs username (§7.3) | **Required** | CMS login |
| `password_hash` | Structural (auth) | — | Mechanism cookie/JWT is app detail | **Required** | Secure credential storage (Spring Security) |
| `display_name` | — | **Yes** | No | **Optional** | Dashboard greeting |
| `enabled` | Structural | — | No | **Required** | Disable account without delete |
| `created_at` | Structural | — | No | **Required** | Audit |
| `updated_at` | Structural | — | No | **Required** | Audit |
| `role` / multi-admin | — | — | **Yes** (§7.4) | Do not add until confirmed | Only single-owner assumed |

### Confirmed fields (AdminUser)

- Need for a login identity + password hash (CMS login is PDF-explicit)
- `id`, `enabled`, timestamps (structural)

### Proposed fields (AdminUser)

- `display_name`
- Product audit link from Product side

### Fields requiring client confirmation (AdminUser)

- Email vs username for `login_identifier`  
- Password reset  
- Multiple admins / roles  
- Whether `created_by_admin_id` on Product is kept  

**Authentication fields review:** Plain password must never be stored. `password_hash` is structural for Spring Security. Identifier **shape** is the only open product question before DDL naming (`email` vs `username` vs generic `login_identifier`).

---

# 5. Special attention topics

## 5.1 Product rating fields (`average_rating`)

| Question | Finding |
|---|---|
| Required by PDF UI? | **Yes** — PDP must show star rating |
| Requires Review table? | **No** — excluded from V1 unless §10 confirms on-site reviews |
| Safe column? | **Yes**, as nullable aggregate on Product |
| Blocked? | **Data source** and CMS ability to set ratings (Add Product form does **not** include rating fields) |

**Review decision:** Keep nullable `average_rating` on Product for display. Do not invent Review. Population method = client confirmation. Until then, UI may hide rating when null.

## 5.2 Rating count (`rating_count`)

| Question | Finding |
|---|---|
| Required by PDF UI? | **Yes** — e.g. `[135 ratings]` |
| Store? | **Yes**, beside average on Product |
| Null vs 0 | Client confirmation |
| Write reviews API | Not confirmed — no child rows |

## 5.3 Product `slug`

| Question | Finding |
|---|---|
| Named in PDF? | No |
| Needed? | **Yes** (structural) for PDP routes and API as designed in architecture/Figma |
| Confirmed business field? | No — structural |
| Risk | Generation from title may collide (e.g. two “Cups, Mugs & Saucers”) — client may need unique slug rule |

**Review decision:** **Safe to implement** as required unique structural field. Not a new storefront feature.

## 5.4 `created_at` / `updated_at`

| Field | Product | Others |
|---|---|---|
| `created_at` | **Required** — PDF sort “Newest arrivals” | Structural audit on Category, ProductImage, AdminUser |
| `updated_at` | Structural audit | Structural audit |

**Review decision:** Safe. Product.`created_at` is both structural and functionally required for confirmed PLP sort.

## 5.5 Product image URL / storage strategy

| Finding | Detail |
|---|---|
| Confirmed | Image upload exists; images must be retrievable for cards/PDP |
| Entity | `ProductImage` with path/URL string is correct |
| Not confirmed | Disk vs S3 (`§14.3`), max count (`§12.2`) |
| Architecture | Small-site default: local/object folder + path in DB |

**Review decision:** Entity is safe. Storage **backend** requires confirmation before production ops; DDL can use a generic string column either way.

## 5.6 Admin authentication fields

| Finding | Detail |
|---|---|
| Confirmed | Admin can log in |
| Safe | `password_hash`, `enabled`, PK |
| Blocked for naming | email vs username |

## 5.7 Admin audit relationship

| Finding | Detail |
|---|---|
| PDF require? | No |
| Useful? | Yes for support |
| Review | Keep **optional / proposed**; may omit from first DDL without blocking catalog |

## 5.8 UPI / Pay Later representation

| UI element | PDF | Database |
|---|---|---|
| UPI badge on PLP | Explicit example | No product column required for CURRENT freeze |
| Pay Later on PDP | Explicit tag | Display-price column PENDING |
| Payment tables | — | **FINAL confirmed need** (UPI + Card + Net Banking + COD); not in CURRENT freeze; gateway/provider + COD charge rule PENDING |

**Review decision:** Required checkout methods **UPI**, **Credit/Debit Card**, **Net Banking**, and **COD** are **CONFIRMED** (not UI-only). Gateway/provider and COD charge amount/rule are **PENDING**. Do **not** invent Payment DDL, gateway integrations, or a COD fee in this review. Do **not** add `upi_*` / `pay_later_*` product columns until display-price rules are decided.

## 5.9 Featured / Bestseller representation

| Finding | Detail |
|---|---|
| PDF | Homepage section “Bestsellers & Featured” |
| CMS | No flags on Add Product form |
| How chosen | **PENDING** (§5.10) — do not assume |

**Options without premature columns:** show newest products; show all; or hard-code seed IDs in config temporarily.

**Review decision:** Do **not** add `is_bestseller` / `is_featured` until client confirms selection rules. Homepage section remains a UI requirement; membership rule is open.

---

# 6. Compliance check — excluded entities

Per instructions and `CLIENT_CONFIRMATIONS.md` (none of these are explicitly confirmed):

| Entity | In V1 schema? |
|---|---|
| Customer | **No** |
| Cart / CartItem | **No** |
| Wishlist | **No** |
| Order | **No** |
| Payment | **No** |
| Coupon | **No** |
| Variant | **No** |
| Inventory | **No** |
| Review | **No** |
| SubCategory | **No** |
| HeroBanner | **No** |

This matches `DATABASE_ER_DESIGN.md` and Figma v1 screen scope.

---

# FINAL DATABASE DECISIONS

## A. Safe to implement now

These may proceed to SQL/migrations when Step 4 is authorized:

| Item | Notes |
|---|---|
| Entity **Category** | Seed exactly 5 PDF names; `id`, `name`, `slug`, `display_order`, timestamps |
| Entity **Product** | `id`, `title`, `category_id`, `selling_price`, `mrp`, `slug`, `created_at`, `updated_at` |
| Entity **ProductImage** | `id`, `product_id`, storage string, `sort_order`, `created_at` |
| Entity **AdminUser** | `id`, login identifier column, `password_hash`, `enabled`, timestamps |
| FK Category ← Product | Required |
| FK Product ← ProductImage | Required |
| `% off` | Derived only — not a column |
| No Cart/Order/Customer/etc. | Remains out of **CURRENT V1 freeze** (FINAL commerce later) |
| Rating **columns** on Product | Safe as **nullable** display fields so PDP can render when data exists |

## B. Requires client confirmation

Do not treat as locked business rules; confirm before relying on them in CMS/UI logic:

| Item | Why blocked |
|---|---|
| Admin email vs username | §7.3 |
| Rating data source / who sets averages | §10.1; not on Add Product form |
| Rating null vs zero display | UX |
| Image storage disk vs S3 | §14.3 |
| Max images per product | §12.2 |
| Pay Later display price / calculation | Formula PENDING if offered (§6.3); not in current required-method list |
| UPI product flag vs always-on | Method **CONFIRMED REQUIRED**; flag PENDING (§6.1) |
| `is_bestseller` / `is_featured` | Selection rules **PENDING** (§5.10) |
| `created_by_admin_id` audit FK | Optional |
| `tile_image_url` / `focus_notes` on Category | Proposed |
| `alt_text` on images | Proposed |
| Product hide/status | **CONFIRMED FINAL** Hide/Unpublish; column shape PENDING (§12.1) |
| Expanding beyond 5 categories | PENDING (§12.6) |
| Payment gateway / provider | **PENDING** (required online methods: UPI, Credit/Debit Card, Net Banking) |
| COD charge amount/rule | **PENDING** (COD method CONFIRMED REQUIRED) |
| Shop Now destination | **PENDING** |

## C. Out of CURRENT V1 freeze (not “forever excluded”)

Do not create these tables in the catalog freeze DDL. **FINAL confirmed** items must still be designed later without inventing providers:

- Customer / customer auth — **CONFIRMED need** (optional accounts); mechanism PENDING design; guest checkout also CONFIRMED  
- Cart / Wishlist persistence — **FINAL confirmed need**  
- Order / OrderItem — **FINAL confirmed need** (guest + registered; Order ID)  
- Payment / gateway records — **FINAL confirmed need** (UPI + Credit/Debit Card + Net Banking + COD; gateway/provider + COD charge rule PENDING)  
- Coupon / Variant / Inventory ledger — PENDING  
- Review (per-review rows) — PENDING  
- SubCategory / HeroBanner / SiteSetting — PENDING (contacts may stay in env)  
- Stored `percent_off` — do not store (derived)  
- Featured flag columns — PENDING selection rules  

---

## Review conclusion

The proposed CURRENT V1 database in `DATABASE_ER_DESIGN.md` is **consistent with the catalog + simple CMS freeze**.

**Ready for DDL** of the four entities (already implemented — see `DATABASE_IMPLEMENTATION.md`).

**Do not invent** Payment/Cart/Order DDL or gateway integrations here. FINAL commerce is confirmed at the business level; providers and storage strategies remain PENDING (`CLIENT_CONFIRMATIONS.md`).

No SQL was generated in this review.

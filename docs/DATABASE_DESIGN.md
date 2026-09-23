# MA CREATIONS — Database Design

**No SQL is included in this document.** This is an entity and relationship design only.

**Legend**

- **PDF-explicit:** named or required by a specified CMS field or UI element.
- **Screenshot-observed:** visible in Meesho catalog images in the PDF; may inform display fields, not automatically a column.
- **Proposed / inferred:** needed to make a working custom site; confirm before locking.
- **FINAL confirmed:** client clarification — required for next commerce phase; **schema shapes may still be PENDING**.
- **CURRENT V1:** four-entity catalog freeze already implemented (see `DATABASE_IMPLEMENTATION.md`).

---

## 1. Design rules

- Model the **5 fixed categories** as data, not hardcoded-only, so the CMS dropdown can be data-driven while remaining limited to those five.
- Product fields in CURRENT V1 match the Add Product form: title, category, image, selling price, MRP.
- **FINAL confirmed:** Order / Payment / Cart / Wishlist persistence / **Customer** (for optional accounts) entities will be required — **do not invent DDL here**; design when storage, auth mechanism, and provider decisions land. Guest checkout is also CONFIRMED (orders without a Customer account).
- Prefer calculated `% off` over a stored field the owner could desync.
**IMPLEMENTED:** product visibility via Hide/Unpublish (`published` boolean; published products only on storefront). Soft-hide preferred over permanent delete in normal admin workflow.
- Product images come from Admin upload only — no fake/static catalog product image seeds.

---

## 2. Entity map

```
AdminUser 1───* Product (created/updated by; proposed audit)

Category 1───* Product
Category 1───* SubCategoryFocus     (optional; see 3.2)
Product  1───* ProductImage
Product  1───* Review               (only if reviews are collected on this site)

Customer 1───* Cart / Wishlist / Order     (FINAL: optional Customer accounts CONFIRMED; guest Orders also CONFIRMED without Customer)

Product  1───* CartItem / WishlistItem     (FINAL required; storage strategy PENDING)
Order    1───* Payment                     (FINAL required; gateway PENDING — no provider invented)
Order    may be guest (no Customer) or linked to Customer
```

---

## 3. Entities

### 3.1 Category

**Why:** PDF requires 5 precise categories and a CMS category dropdown linked to them.

| Field | Source | Notes |
|---|---|---|
| id | Proposed | Primary key |
| name | PDF-explicit | Exact names: Hydration & Drinkware; Lunch & Meal Prep; Kitchen Gadgets & Prep; Storage & Kitchenware; Home Decor & Festivity |
| slug | Proposed | URL for PLP |
| tile_image_url | Inferred | Homepage “5 circular/rounded image tiles” need images; CMS upload for tiles is not specified |
| display_order | Proposed | Keep the PDF’s 1–5 order |
| created_at / updated_at | Proposed | Audit |

Seed exactly 5 rows. Adding a sixth category contradicts the PDF unless the client changes the taxonomy.

### 3.2 SubCategoryFocus (optional)

**Why:** PDF column “Sub-Categories / Focus” (e.g. Water bottles, Gym shakers, Insulated vacuum flasks).

| Field | Source | Notes |
|---|---|---|
| id | Proposed | |
| category_id | PDF-explicit relationship | Belongs to one of the 5 |
| name | PDF-explicit | From the taxonomy focus lists |
| is_navigable | Proposed | **CLIENT CONFIRMATION REQUIRED:** filter vs description only |

If the client says sub-categories are notes only, this table can be omitted and stored as text on Category.

### 3.3 Product

**Why:** Core catalog; CMS Add Product form; PLP/PDP/homepage cards.

| Field | Source | Notes |
|---|---|---|
| id | Proposed | Primary key |
| title | PDF-explicit | CMS “Product Title”; PLP/PDP title |
| category_id | PDF-explicit | CMS category dropdown; one of 5 |
| selling_price | PDF-explicit | CMS “Selling Price”; discounted price on cards |
| mrp | PDF-explicit | CMS “MRP”; struck-through on cards |
| percent_off | Derived | `(mrp - selling_price) / mrp`; display badge |
| pay_later_price | Screenshot-observed + PDP copy | PDF PDP visual. Not in current required checkout methods (UPI / Card / Net Banking / COD). Whether a lower display price is stored/calculated remains **PENDING** |
| average_rating | PDF-explicit display | Example 4.0★. Could be stored denormalized or computed |
| rating_count | PDF-explicit display | Example 135 ratings |
| is_bestseller | Inferred | Homepage “Bestsellers & Featured”. **Selection rules PENDING** — do not add until confirmed |
| is_featured | Inferred | Same section. May be one flag or two — **PENDING** |
| created_at | Inferred | Needed for PLP sort “Newest arrivals” |
| slug | Proposed | PDP URL |
| description | Not in PDF | PENDING |
| sku / catalog_id | Screenshot-observed fragments (e.g. `1042806413`, `1051471515`) | Meesho ids; PENDING whether to keep |
| stock_quantity | Not in PDF | PENDING |
| `published` (boolean) | **IMPLEMENTED** | V6 `product.published` TINYINT(1) NOT NULL DEFAULT 1; Hide/Unpublish |
| upi_available | Screenshot + PLP UPI badge | **UPI is a REQUIRED checkout method** (FINAL). Per-product flag vs always-on UI still PENDING |
| material / bpa_free | Taxonomy focus text for lunch boxes | Not a CMS field. PENDING |
| meesho_sample_name | PDF sample list | Optional mapping to taxonomy names |

**Screenshot-observed titles that must be representable as `title`:** Modern Water Bottles, Essential Lunch Boxes, Classy Water Bottles, Classic Water Bottles, Designer Water Bottles, Classy Lunch Boxes, Modern Lunch Boxes, Essential Thermos & Vacuum Flasks, Graceful Cooking Spoons, Cups, Mugs & Saucers, Cycling Water Bottles & Shakers, plus taxonomy names not visible in screenshots.

Two screenshot cards share the title **Cups, Mugs & Saucers** at different prices. **CLIENT CONFIRMATION REQUIRED:** variants vs two products. No variant entity is proposed until that answer exists.

### 3.4 ProductImage

**Why:** CMS Image Upload; PDP zoomable main image + thumbnail gallery; cards need a photo.

| Field | Source | Notes |
|---|---|---|
| id | Proposed | |
| product_id | PDF-explicit | |
| file_path_or_url | PDF-explicit upload | Storage mechanism CLIENT CONFIRMATION REQUIRED |
| sort_order | Inferred | First image = card + main PDP image |
| alt_text | Proposed | Accessibility; not in PDF |

**CLIENT CONFIRMATION REQUIRED:** max images per product.

### 3.5 AdminUser

**Why:** CMS “log in without coding knowledge.”

| Field | Source | Notes |
|---|---|---|
| id | Proposed | |
| email_or_username | Inferred | Login identifier not specified |
| password_hash | Inferred | Spring Security |
| display_name | Proposed | |
| enabled | Proposed | |
| created_at | Proposed | |

Roles beyond a single owner are **CLIENT CONFIRMATION REQUIRED**.

### 3.6 Review (conditional)

**Why:** PDP must **display** stars and count. The PDF does not say reviews are written on this site.

| Field | Source | Notes |
|---|---|---|
| product_id, rating, body, customer | Inferred | Only if on-site reviews are confirmed |
| imported_average / imported_count | Alternative | If ratings are copied from Meesho and frozen on Product |

Until confirmation, store `average_rating` and `rating_count` on Product as display fields.

### 3.7 SiteSetting (inferred)

**Why:** Instagram handle, WhatsApp number, Follow URL, and payment-badge assets must live somewhere if the owner is not a developer. The PDF does not define this table.

| Field (examples) | Source |
|---|---|
| instagram_handle / follow_url | **CONFIRMED** `https://www.instagram.com/macreations.living/` (may stay in frontend env) |
| whatsapp_number | **CONFIRMED** client-provided `6395700831` (intl format = config verification; may stay in frontend env) |
| follow_url | Inferred from Follow CTA |
| return_policy_url_or_html | Footer return policy; content unspecified |

**CLIENT CONFIRMATION REQUIRED** before adding.

### 3.8 HeroBanner (inferred)

**Why:** Homepage carousel of home decor, festive candles, water bottles + Shop Now.

Not in the CMS form. Could be static in the frontend for v1.

**CLIENT CONFIRMATION REQUIRED** if banners are CMS-managed.

| Field if created | Notes |
|---|---|
| image, caption, cta_label, cta_link, sort_order, active | All inferred |

---

## 4. Entities beyond CURRENT V1 catalog freeze

**CURRENT V1** implements Category, Product, ProductImage, AdminUser only.

**FINAL confirmed** business scope will require commerce entities; **do not invent DDL** until PENDING decisions (storage, gateway, auth/tracking mechanism design) are answered:

| Entity | FINAL status | Notes |
|---|---|---|
| Customer / User | **IMPLEMENTED** (V7 `customer`, mobile unique) | OTP auth; no password |
| Cart | **IMPLEMENTED** (V4 + V8 customer_id) | Guest XOR customer; merge → MERGED |
| Wishlist | **IMPLEMENTED** (V5 + V8 customer_id) | Guest XOR customer; merge → MERGED |
| Order | **IMPLEMENTED** schema (V9) | Preview only; place-order PENDING |
| OrderItem / OrderAddress | **IMPLEMENTED** (V9 snapshots) | History independent of live Product |
| Address | Likely with checkout | Guest + account; field set PENDING |
| Cart / CartItem | **CONFIRMED need** | Persistence strategy PENDING |
| Wishlist / WishlistItem | **CONFIRMED need** | Account-linked where applicable; guest rules PENDING |
| Order / OrderItem | **CONFIRMED need** | Guest and registered; Order ID required; status machine PENDING |
| Payment | **IMPLEMENTED** schema (V10 `payment_transaction`) + Razorpay adapter | UPI/Card/NetBanking/Pay Later online via Razorpay; COD offline; Pay Later enablement PENDING; COD charge PENDING |
| Shipment / Courier | PENDING | |
| Coupon | PENDING | |
| Variant / SKU options | PENDING | |
| Inventory movement | PENDING | |

---

## 5. Relationships

| Relationship | Type | Source |
|---|---|---|
| Category → Product | One to many | PDF: every product assigned via dropdown to one of 5 |
| Category → SubCategoryFocus | One to many | PDF taxonomy column; navigability unconfirmed |
| Product → ProductImage | One to many | PDP gallery + Admin upload (no fake catalog images) |
| Product → Review | One to many | Only if on-site reviews confirmed |
| AdminUser → Product | One to many (audit) | Proposed |
| Product belongs to one Category | Many to one | PDF does not mention multi-category products |

Homepage Bestsellers & Featured is **not** a separate entity until the client says how membership works (flags vs sales vs manual list) — **PENDING**.

---

## 6. Display fields vs stored fields

| UI element | Store in DB? |
|---|---|
| % off badge | No; calculate from MRP and selling price |
| UPI badge | **UPI is a REQUIRED checkout method** (FINAL). Per-product DB flag PENDING |
| Pay Later price | PDF visual; provider PENDING if offered; not in current required-method list |
| Star rating + count | Yes, as product aggregates and/or Review rows |
| Cart counter | Persistent cart (FINAL); table vs client storage PENDING |
| Instagram posts | Not copied into MySQL by default; fetched live |
| Payment trust badges | Static assets or SiteSetting; transactional Payment entity still required for real payments |

---

## 7. Seed data implied by the PDF

**Categories:** the 5 names in Section 4 of the PDF.

**Optional product seed:** the sample product names in the taxonomy, plus screenshot-observed titles and prices where readable. Completeness of the live catalog is **PENDING**.

Do not invent descriptions, stock, missing prices, or **fake product images**. Images come from Admin upload.

---

## 8. Open schema questions

See `CLIENT_CONFIRMATIONS.md`. Still blocking concrete commerce DDL:

1. Cart/wishlist technical storage (guest vs account linkage details)
2. Whether Pay Later shows a stored lower price (and how calculated)
3. Whether ratings are manual, imported, or user-generated
4. Image storage location (disk vs object storage)
5. Whether sub-categories are entities
6. Payment gateway — **CONFIRMED Razorpay**; Pay Later merchant enablement / BNPL rail PENDING; COD charge rule PENDING
7. Place-order API + checkout UI (Step 24+)
8. Shipping / GST rules
6a. COD charge amount/calculation rule (COD method CONFIRMED; rule PENDING)
7. Product publish/hide — **IMPLEMENTED** (`published` boolean, V6)
8. Customer mobile OTP auth — **IMPLEMENTED** (V7); real SMS provider + guest order-tracking verification flow PENDING
9. Shop Now destination and bestseller/featured selection rules (PENDING — not schema-blocking for catalog)

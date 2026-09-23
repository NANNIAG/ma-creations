# MA CREATIONS — Development Plan

**Planning + delivery roadmap.** Keep **CURRENT V1** (catalog + admin CMS already built) separate from **FINAL confirmed business scope** (checkout, search, wishlist, persistent cart, required payment methods UPI / Card / Net Banking / **COD** — see `CLIENT_CONFIRMATIONS.md`).

Do not invent shipping rules, BNPL brand names, or selection algorithms while those items remain PENDING. **Razorpay** is the selected payment gateway (Step 23).

Dependency order is top-to-bottom. A later phase must not start if its listed dependency is missing.

---

## Phase 0 — Discovery freeze

**Goal:** Architecture pack from the PDF.

**Tasks**

1. Analyze the PDF text, wireframes, taxonomy, and Meesho screenshots.
2. Write `REQUIREMENTS.md`, `SYSTEM_ARCHITECTURE.md`, `DATABASE_DESIGN.md`, `API_DESIGN.md`, `USER_WORKFLOW.md`, `DEVELOPMENT_PLAN.md`, `CLIENT_CONFIRMATIONS.md`.
3. Separate PDF-explicit items from CLIENT CONFIRMATION REQUIRED items.

**Exit:** These seven files exist and are internally consistent.

**Status:** Complete.

---

## Phase 1 — Client confirmations and visual design

**Depends on:** Phase 0.

**Goal:** Close gaps the PDF leaves, and produce Figma screens that match Section 3 (plus newly confirmed commerce screens when designed).

**Tasks (order)**

1. Walk through `CLIENT_CONFIRMATIONS.md` with the client. **FINAL clarifications now include:**
   - Selling model: **website checkout + online payment** (WhatsApp = contact / alternate)
   - Search, wishlist, persistent cart: **required**
   - Required checkout payment methods: **UPI**, **Credit/Debit Card**, **Net Banking**, **COD** (gateway/provider still PENDING — do not choose; COD charge amount/rule PENDING — do not invent)
   - Additional payment methods may be added later if the client requests them
   - Admin visibility: **Hide/Unpublish** (not permanent delete in normal flow)
   - WhatsApp number **`6395700831`**; Instagram **`https://www.instagram.com/macreations.living/`**
   - Still PENDING: Shop Now destination, bestseller/featured rules, Inter vs Montserrat, gateway/provider, shipping; **auth & guest-tracking mechanism design** (guest + accounts CONFIRMED)
2. Produce Figma (mobile-first, then desktop) for **PDF screens** plus **FINAL required** commerce screens (Search, Wishlist, Cart, Checkout, Order Confirmation) when scheduled — wireframes still TBD.
3. Collect brand assets: logo, product photos (via Admin upload — no fake catalog images), hero images, category tile photos.
4. Confirm the live product list vs taxonomy sample names vs screenshot titles/prices.

**Exit:** Signed answers to blocking confirmations; Figma for Screen 1–4 (+ commerce frames as ready); asset list.

**Status:** Clarification phase complete for items above; remaining PENDING items still open.

---

## Phase 2 — Database schema (design to SQL)

**Depends on:** Phase 1 answers that affect schema (cart persistence strategy, customer auth, orders, payments, hide/unpublish).

**Goal:** MySQL schema that matches `DATABASE_DESIGN.md` after confirmations.

**Tasks (order)**

1. Freeze entities for the phase being built (CURRENT V1: Category, Product, ProductImage, AdminUser). For the **next commerce phase**, expand only with **CONFIRMED** extras — Order/Payment/Cart/Wishlist shapes remain **PENDING design** (do not invent DDL here).
2. Write SQL DDL / Flyway migrations for the approved slice.
3. Seed the **5 categories** with PDF names.
4. Optionally seed sample products **only** with names/prices actually provided. Do not invent missing prices or fake product images.
5. Decide image storage path convention (Admin upload remains intended approach).

**Exit:** Reviewed schema + seed plan for the active phase.

**CURRENT V1 status:** Catalog schema implemented (see `DATABASE_IMPLEMENTATION.md`).

---

## Phase 3 — Backend foundation

**Depends on:** Phase 2 schema; stack already chosen.

**Goal:** Spring Boot API skeleton for catalog (+ later commerce APIs).

**Tasks (order)**

1. Create the Spring Boot project.
2. Connect MySQL, JPA entities, Flyway migrations.
3. Spring Security: admin login.
4. Implement **public** catalog `GET` APIs with PDF sorts.
5. Implement admin product create + image upload; list/edit as confirmed.
6. Calculate `% off` on the server.
7. **FINAL scope (next phase — not CURRENT V1):** search APIs, cart/wishlist persistence APIs, checkout/orders/payment initiation — only after PENDING provider/storage decisions allow design. Do not invent gateway integrations early.

**Exit (CURRENT V1):** Catalog read APIs + admin create/list/edit/delete product, secured.

**FUTURE (CONFIRMED):** Add commerce modules (checkout, orders, payments). Hide/Unpublish is **IMPLEMENTED**.

---

## Phase 4 — Admin CMS UI

**Depends on:** Phase 3 admin APIs.

**Goal:** Non-technical catalog maintenance from Screen 4.

**Tasks (order)**

1. React admin: login, Add Product (Title, Category, Image upload, Selling Price, MRP).
2. Product list / edit (CURRENT V1).
3. **IMPLEMENTED:** Hide/Unpublish instead of permanent delete in the normal workflow.
4. Simple dashboard shell. Widgets only if confirmed.

**Exit:** Owner can log in, add, list, edit, and **Hide/Publish** products (hard delete removed from normal Admin flow).

**IMPLEMENTED:** Hide/Unpublish (`product.published`, `PATCH /api/admin/products/{id}/status`).

---

## Phase 5 — Customer storefront (PDF screens)

**Depends on:** Phase 3 catalog APIs; Phase 1 Figma; design tokens.

**Goal:** Screens 1–3, mobile-first.

**Tasks (order)**

1. Storefront layout: header (logo, search, wishlist, cart badge), footer, floating WhatsApp widget.
2. Homepage: hero + Shop Now (**destination PENDING**), 5 category tiles, bestsellers/featured (**rules PENDING** — newest fallback today), Instagram + Follow CTA.
3. PLP / PDP with pricing hierarchy; Buy via WhatsApp as **alternate** path.
4. Wire Add to Cart — **CURRENT V1:** UI-only notice; **FINAL:** persistent cart.
5. **FINAL (CONFIRMED):** implement Search and Wishlist (pages/UX still PENDING detail).

**Exit (CURRENT V1):** Browse Home → Category → PDP; WhatsApp and UI-only Add to Cart.

---

## Phase 6 — Integrations

**Depends on:** Phase 5 UI surfaces; confirmed credentials.

**Tasks (order)**

1. WhatsApp widget + PDP Buy link with client number **`6395700831`** (verify intl formatting for `wa.me` as needed).
2. Instagram Follow URL `https://www.instagram.com/macreations.living/`; live grid embed/API still PENDING.
3. **FINAL:** Payment gateway integration once a provider is named — must support **UPI**, **Credit/Debit Card**, and **Net Banking**. **COD** is a required checkout option with an additional charge (charge rule PENDING). Do not choose a provider in this plan.
4. Facebook pixel / ads only if confirmed.

**Exit:** Social CTAs work; payment integration when provider is chosen.

---

## Phase 7 — Checkout and commerce (CONFIRMED FINAL; not optional)

**Depends on:** Client FINAL decision for website checkout (done); guest + optional accounts CONFIRMED; remaining PENDING: gateway/provider, shipping, auth/tracking mechanism design, cart storage.

**Tasks (order) — FUTURE implementation**

1. Persistent cart page + replace UI-only Add to Cart — **IMPLEMENTED** (guest + customer + merge)
2. Wishlist add/remove — **IMPLEMENTED** (guest + customer + merge)
3. Product search — **IMPLEMENTED** (`GET /api/products?search=` + `/search?q=`)
4. Checkout supporting **guest** and **registered** customers — **IMPLEMENTED** (Step 24 `/checkout` + `POST /api/orders`)
5. Online payment supporting **UPI**, **Credit/Debit Card**, and **Net Banking** via **Razorpay** — **IMPLEMENTED** foundation + Checkout wiring (Step 23–24)
5a. **COD** as a checkout option — **IMPLEMENTED** placement + **₹20** fee (Step 25)
5b. **Pay Later** — **REQUIRED** in UI/API; merchant rail gated by `RAZORPAY_PAY_LATER_ENABLED`
5c. Shipping / GST V1 rules — flat **₹20** shipping, GST not charged (**IMPLEMENTED** Step 25); courier provider **PENDING**
6. Order confirmation with **Order Number** — **IMPLEMENTED** (`/order-success/:orderNumber`)
7. Guest order tracking (order number + mobile) — **IMPLEMENTED** (Step 26B); account order history — **IMPLEMENTED** (Step 26A)
8. Optional customer mobile OTP login — **IMPLEMENTED** (SMS provider PENDING)
9. Checkout order core + preview — **IMPLEMENTED** (V9)
9a. Place-order + Razorpay Checkout + payment failure/retry — **IMPLEMENTED** (Step 24)
9b. Admin order list / detail / status update — **IMPLEMENTED** (Step 26A)
9c. Manual tracking/AWB + admin/customer/guest display — **IMPLEMENTED** (Step 26B); courier API **PENDING**
10. Email/SMS **if** confirmed — **PENDING**
11. Product Hide/Unpublish — **IMPLEMENTED** (storefront filter + admin Hide/Publish)

Do **not** skip this phase on the assumption of WhatsApp-first selling — that model was **not** chosen.

---

## Phase 8 — Testing

**Depends on:** Phases 3–6 (and 7 when built).

**Tasks:** API/UI tests for the active phase; WhatsApp link tests; visual check against brand tokens.

---

## Phase 9 — Deployment

**Depends on:** Phase 8; hosting answers still largely PENDING in `CLIENT_CONFIRMATIONS.md`.

---

## Mapping to requested workstreams

| Workstream | Phases |
|---|---|
| Figma / UI | 1, 4, 5, 7 |
| Database | 2 (+ commerce expansions) |
| Backend | 3, 7 |
| Frontend | 4, 5, 7 |
| Integrations | 6 |
| Testing | 8 |
| Deployment | 9 |

---

## Next implementation guidance

1. Keep CURRENT V1 catalog/CMS stable.
2. Before finishing remaining Phase 7 items: close PENDING (shipping **provider**, Pay Later Razorpay enablement, guest order-tracking flow, invoices/notifications).
3. **Step 23 done:** Razorpay payment foundation (V10, initiate/verify/webhook).
4. **Step 24 done:** `POST /api/orders`, checkout UI, Razorpay Checkout wiring, COD placement, success/failure flows, idempotency.
5. **Step 25 done:** flat ₹20 shipping, ₹20 COD fee, GST not charged (prices GST-inclusive).
6. **Step 26A done:** customer order history/detail + admin order list/detail/status update (no tracking/cancel/refund).
7. **Step 26B done:** manual AWB tracking + guest track (order number + mobile). Courier/provider/URL deferred.
8. **Next:** courier integration when confirmed; cancel-refund; invoices/notifications.
9. Shop Now destination and bestseller/featured rules remain PENDING — do not hard-code assumptions.
10. Do not invent missing product prices or fake catalog images.

---

## Out of order work to refuse

- Adding Redis, Kafka, Elasticsearch, Shopify, or WooCommerce without a decision
- Inventing product descriptions or missing prices
- Building Gender filters because Meesho showed them
- Selecting a payment gateway/provider without client confirmation
- Treating WhatsApp as the primary order path (conflicts with FINAL decision)

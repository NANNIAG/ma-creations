# MA CREATIONS — Shipping, GST/Tax & COD Business Rules

**Step:** 25 — Analysis (earlier) + **V1 charge rules IMPLEMENTATION**  
**Date context:** After Steps 1–24; Step 25 implements finalized client charge rules.

### Step 25 V1 — IMPLEMENTED

| Rule | Status |
|---|---|
| Flat shipping charge **₹20.00** | **IMPLEMENTED** (`app.checkout.shipping.mode=FIXED`, `fixed-amount=20.00`) |
| COD additional charge **₹20.00** (COD only) | **IMPLEMENTED** (`app.checkout.cod.mode=FIXED`, `fixed-amount=20.00`) |
| GST **not charged**; taxAmount = **0**; prices **GST-inclusive** | **IMPLEMENTED** (`app.checkout.tax.mode=ZERO`; no GST UI line) |
| No PIN/courier shipping calculation | **CONFIRMED out of scope for V1** |
| No HSN/CGST/SGST/IGST columns | **CONFIRMED out of scope for V1** |

### Still PENDING (not this step)

| Item | Status |
|---|---|
| Shipping provider / courier integration | **PENDING** |
| Razorpay Pay Later merchant/rail enablement | **PENDING** |
| Invoice requirements | **PENDING** |
| Order tracking / history | **PENDING** (later steps) |
| Notifications | **PENDING** |
| Cancellation / refund / return rules | **PENDING** |

Pay Later **method** remains **CONFIRMED REQUIRED** and available in checkout.

---

## Historical analysis notes

The sections below retain the pre-decision analysis context. Where they conflict with the Step 25 V1 table above, **the V1 IMPLEMENTED table wins**.

**Sources inspected:**

| Source | Role |
|---|---|
| `docs/CLIENT_CONFIRMATIONS.md` | Authoritative client decision log |
| `docs/REQUIREMENTS.md` | PDF + FINAL scope |
| `docs/CHECKOUT_ORDER_ANALYSIS.md` | Order/checkout architecture |
| `docs/PAYMENT_PROVIDER_ANALYSIS.md` | Payment vs COD separation |
| `docs/API_DESIGN.md` | API contracts |
| `CheckoutService` + charge calculators | Runtime behaviour |
| V9 `orders` / `order_items` / `order_addresses` | Schema readiness |
| Frontend `/checkout` | Display patterns |

**Legend**

| Tag | Meaning |
|---|---|
| **CONFIRMED** | Explicit client / project decision |
| **PENDING** | Still needs a client or commercial answer |
| **IMPLEMENTED** | Present in code for V1 charge rules |
| **NOT SPECIFIED** | No statement historically |

---

## 0. Scope of the original analysis

This document originally answered:

1. What shipping / GST / COD rules were known vs unknown  
2. How checkout calculates totals  
3. Whether V9 is enough for charge snapshots  
4. What would change after decisions land  

**Pay Later:** method is **CONFIRMED REQUIRED**. Only Razorpay Pay Later / BNPL **merchant enablement / rail** remains PENDING.

---

## 1. Current behaviour (as of Step 24)

### 1.1 Total formula (already encoded)

```
items_subtotal
+ shipping_charge
+ cod_charge
+ tax_amount
− discount_amount
= grand_total
```

Computed **only on the server** in `CheckoutService.preview`, then snapshotted onto `Order` at `POST /api/orders` when `previewHash` matches and all charge calculators report `configured()`.

### 1.2 Pluggable calculators (**IMPLEMENTED placeholders**)

| Calculator | Config keys | Production default | Local (`application-local.yml`) | Behaviour |
|---|---|---|---|---|
| `ConfigurableShippingChargeCalculator` | `app.checkout.shipping.mode`, `fixed-amount` | **PENDING** | **ZERO** | PENDING → block place (`SHIPPING_RULE_PENDING`); ZERO/FIXED → amount |
| `ConfigurableCodChargeCalculator` | `app.checkout.cod.mode`, `fixed-amount` | **PENDING** | **ZERO** | Non-COD → always `0`; COD + PENDING → block; ZERO/FIXED → amount |
| `ConfigurableTaxCalculator` | `app.checkout.tax.mode`, `fixed-amount` | **ZERO** | (inherits / not overridden) | ZERO → `0` configured; PENDING → block; FIXED → amount |
| `ZeroDiscountCalculator` | — | always 0 | — | Coupons **PENDING**; amount already snapshotted as 0 |

Local ZERO for shipping/COD exists **only** so Steps 21–24 could be tested without inventing production fees. It is **not** a client-confirmed production rule.

### 1.3 Place-order gate

If any of shipping / COD (when COD) / tax / discount is not `configured()`, preview sets `readyToPlace=false`, `previewHash` is null (when charges incomplete), and place-order returns `CHECKOUT_REVIEW_REQUIRED`.

### 1.4 What is already snapshotted on Order (V9)

| Field | Snapshotted? |
|---|---|
| `items_subtotal` | Yes |
| `shipping_charge` | Yes |
| `cod_charge` | Yes |
| `tax_amount` | Yes (order-level aggregate) |
| `discount_amount` | Yes |
| `grand_total` | Yes |
| Per-line tax / HSN / GST split | **No** (not in V9 `order_items`) |

Shipping address (including `state`, `postal_code`) is snapshotted on `order_addresses` — enough input for **future** location-based shipping or IGST if those rules are confirmed later.

### 1.5 Payment methods vs charges

| Method | Online gateway | COD charge calculator | Same total pipeline? |
|---|---|---|---|
| UPI / CARD / NET_BANKING / PAY_LATER | Razorpay | Returns 0 | Yes — one `CheckoutService` |
| COD | None | Applied when method = COD | Yes — same pipeline |

No duplicated total logic per method.

---

## 2. Shipping — findings

### 2.1 Confirmed

| Item | Status | Evidence |
|---|---|---|
| Physical delivery of ordered products is part of website checkout | **CONFIRMED** (implied by complete checkout + delivery address) | `CLIENT_CONFIRMATIONS` §7.1a / §9.0–9.1 |
| Delivery address collected at checkout | **CONFIRMED** | Guest/customer checkout requirements |
| Shipping charge must be server-authoritative and order-snapshotted | **CONFIRMED** (architecture) | Checkout analysis §6–7; V9 columns |
| Shipping must be known before prepaid payment / COD accept | **CONFIRMED** (architecture) | Preview hash binds shipping into `grand_total` |

### 2.2 Pending / not specified

| Question | Status |
|---|---|
| Is shipping charged on every order? | **PENDING** / **NOT SPECIFIED** |
| Flat-rate shipping? | **PENDING** (§9.4) |
| Location / PIN-based shipping? | **PENDING** (§9.4) |
| Weight-based shipping? | **NOT SPECIFIED** (product weight also PENDING in catalog) |
| Order-value based shipping? | **PENDING** |
| Free-shipping threshold? | **PENDING** (§9.4) |
| Different shipping for COD vs prepaid? | **NOT SPECIFIED** |
| Shipping provider / courier for checkout pricing? | **PENDING** (§9.4) — see §10 |
| Who packs and ships? | **PENDING** (§9.5) |
| Exact production amount(s) | **PENDING** — do not invent |

### 2.3 Architecture implications (proposed, not implemented)

- **Price calculation** can be a pure server calculator (config or rules table) without a live courier API.  
- **Provider integration** (create shipment, AWB, tracking) is a **separate** later concern.  
- Until a rule is confirmed, production should keep `shipping.mode=PENDING` (fail closed) rather than silently shipping free — local ZERO is test-only.

### 2.4 Shipping summary

**Shipping charge model: PENDING.**  
**Shipping provider for immediate checkout math: not required** if the client confirms a simple tariff (e.g. flat / free-over-X) that can be computed from cart + address alone.  
**Courier integration: PENDING** and separable.

---

## 3. GST / tax — findings

### 3.1 Confirmed

| Item | Status | Evidence |
|---|---|---|
| Currency INR | **CONFIRMED** | Requirements / PDF |
| Tax component exists in total formula | **CONFIRMED** (architecture) | Checkout analysis; V9 `tax_amount` |
| Invoice / GST format details | **PENDING** | `CLIENT_CONFIRMATIONS` §9.6 |
| Prices inclusive of GST? | **PENDING** | §6.4 |

### 3.2 Not specified / pending (do not invent)

| Question | Status |
|---|---|
| Is MA CREATIONS required to charge GST on this website? | **PENDING** / **NOT SPECIFIED** |
| Selling prices GST-inclusive vs exclusive? | **PENDING** (§6.4) |
| GST rate(s)? | **PENDING** / **NOT SPECIFIED** |
| Same rate for all products? | **NOT SPECIFIED** |
| HSN / SAC codes on products? | **PENDING** (catalog fields not specified) |
| CGST+SGST vs IGST (intra vs inter-state)? | **NOT SPECIFIED** |
| Is customer shipping state required for tax? | **NOT SPECIFIED** (state is already collected for address) |
| Tax per line vs order rollup? | **NOT SPECIFIED** |
| Show tax separately at checkout? | **NOT SPECIFIED** (UI already has a Tax row when preview returns an amount) |
| Tax on shipping? | **NOT SPECIFIED** |
| Tax on COD fee? | **NOT SPECIFIED** |
| OrderItem tax snapshots? | **NOT SPECIFIED** — see §7 |
| GST breakdown on invoice? | **PENDING** (invoices overall PENDING) |
| Rounding rules (paise / HALF_UP)? | **NOT SPECIFIED** — code today uses scale 2 + `HALF_UP` for money; treat as **provisional engineering default**, not a client-confirmed tax rule |

### 3.3 Current tax calculator behaviour

- Default mode **ZERO** → `tax_amount = 0` and checkout can proceed.  
- This is an Engineering “do not invent GST %” stance, **not** a confirmation that GST is ₹0 in production.  
- If the client later requires GST before go-live, switch production to a configured rule or `PENDING` until rates are confirmed.

### 3.4 GST summary

**All material GST business parameters are PENDING.**  
No rate, inclusivity, HSN, or split may be assumed.

---

## 4. COD — findings

### 4.1 Confirmed

| Item | Status | Evidence |
|---|---|---|
| COD is a required checkout payment method | **CONFIRMED** | §6.1c, §9.3 |
| COD orders carry an **additional charge** | **CONFIRMED** | Same |
| COD does **not** use Razorpay online processing | **CONFIRMED** | Payment analysis; Step 23–24 |
| Exact charge amount / calculation | **PENDING** | §6.6, §9.3 — do not invent flat or % |
| COD order create → `PLACED` + `COD_PENDING` | **CONFIRMED** (implemented behaviour aligned with design) | Step 24 |

### 4.2 Unknown (pending / not specified)

| Question | Status |
|---|---|
| Exact COD fee | **PENDING** |
| Flat vs percentage vs tiered | **PENDING** |
| Min / max order amount for COD | **NOT SPECIFIED** |
| Location / PIN restrictions | **PENDING** (§9.4 adjacent) |
| Product restrictions | **NOT SPECIFIED** |
| COD availability by shipping area | **NOT SPECIFIED** |
| Whether COD charge is taxable | **NOT SPECIFIED** |
| Different shipping when COD selected | **NOT SPECIFIED** |
| When `COD_PENDING` becomes `PAID` (cash collected) | **PENDING** (admin process) |

### 4.3 Current COD calculator behaviour

- Non-COD methods → charge `0` (configured).  
- COD + production default `PENDING` → checkout blocked until rule configured.  
- Local `ZERO` → COD fee ₹0 for testing only.

### 4.4 COD summary

**Method CONFIRMED; fee rule PENDING.**  
Production COD path should remain blocked (`COD_CHARGE_RULE_PENDING`) until the client confirms the charge model — unless the client explicitly confirms “₹0 COD fee,” which they have **not**.

---

## 5. Total calculation — proposed production architecture

### 5.1 Single pipeline (keep)

All methods (UPI, Card, Net Banking, Pay Later, COD) must continue to use **one** server path:

1. Resolve cart + published products + quantities  
2. Build line snapshots (title, unit price, line subtotal)  
3. `items_subtotal`  
4. `shipping_charge` = `ShippingChargeCalculator`  
5. `cod_charge` = `CodChargeCalculator` (0 unless COD)  
6. `tax_amount` = `TaxCalculator` (inputs may later include address/state, shipping, COD)  
7. `discount_amount` = `DiscountCalculator` (coupons PENDING)  
8. `grand_total`  
9. Bind into `previewHash`  
10. On place-order: re-preview, match hash, persist Order snapshots  

Frontend totals remain **display-only**.

### 5.2 Order header snapshots (required)

Always persist on `orders`:

- `items_subtotal`  
- `shipping_charge`  
- `cod_charge`  
- `tax_amount`  
- `discount_amount`  
- `grand_total`  
- `currency`  
- `payment_method`  

These must never be client-supplied.

### 5.3 Optional future line-level tax (only if client requires)

If invoices need per-HSN / per-line GST, add OrderItem tax columns in a **future migration** after rules are known. Not required for the current aggregate `tax_amount` design.

---

## 6. Checkout flow (desired)

```
Cart
  ↓
Checkout (contact + shipping address)
  ↓
POST /api/checkout/preview
  · reload cart + products
  · shipping calculation (server)
  · tax calculation (server)
  · payment method selection
  · COD charge if paymentMethod = COD
  · grand total + previewHash
  ↓
UI shows Order Summary (server values)
  ↓
Place Order (POST /api/orders + idempotency key)
  · re-validate previewHash
  · snapshot Order / OrderItem / OrderAddress
  ↓
  ├─ COD → PLACED + COD_PENDING (no Razorpay)
  └─ Online → PENDING_PAYMENT + Razorpay initiate → verify/webhook → PAID / PLACED
  ↓
Order success / payment failure + retry
```

**Where each calculation happens:** exclusively in `CheckoutService` (and future calculator implementations). Address/state/PIN may become inputs once location-based rules are confirmed. Payment method is already an input (COD fee).

---

## 7. Database impact (V9)

### 7.1 Order header — **sufficient for current formula**

| Column | Sufficient? |
|---|---|
| `shipping_charge` | Yes |
| `cod_charge` | Yes |
| `tax_amount` | Yes (order-level) |
| `discount_amount` | Yes |
| `grand_total` | Yes |

**No migration is required** solely to store the current five money components.

### 7.2 Possible future columns (document only — do not migrate now)

Only if client confirms GST invoice / split tax needs:

| Possible addition | Why |
|---|---|
| `orders.taxable_value`, `cgst_amount`, `sgst_amount`, `igst_amount` | GST invoice breakdown |
| `orders.shipping_tax_amount`, `cod_tax_amount` | If tax on shipping/COD is confirmed separately |
| `order_items.hsn_snapshot`, `line_tax_amount`, `tax_rate_snapshot` | Per-line historical GST |
| `product.hsn_code`, `product.gst_rate` | Catalog tax masters |
| `product.weight_grams` | If weight-based shipping confirmed |

### 7.3 OrderItem today

Snapshots: product ref, title, slug, unit selling, MRP, qty, line subtotal.  
**No tax fields.** Adequate until per-line GST is confirmed.

### 7.4 Address

`state` + `postal_code` already present — enough for future PIN/state rules without schema change for address itself.

---

## 8. Frontend impact (analysis only)

### 8.1 Current UI (Step 24)

Order summary already shows:

- Line items + subtotal  
- Shipping  
- COD charge (when method = COD)  
- Tax  
- Discount (if > 0)  
- Grand total  

Unavailable / pending rules surface as “Will be calculated” / “Unavailable” and Place Order stays disabled when `readyToPlace` is false.

### 8.2 Recommended display policy (after rules confirmed)

| Row | Display |
|---|---|
| Subtotal | Always |
| Shipping | Always once rule configured; show ₹0 if free shipping confirmed; hide or “Will be calculated” while PENDING |
| COD fee | Only when payment method = COD |
| GST / Tax | Show when client wants separate tax line; if prices are GST-inclusive, confirm whether to show “Includes GST” vs separate amount |
| Discount | Only when > 0 or coupon applied |
| Grand total | Always |

Do not show fake ₹0 for **PENDING** rules (current UI already avoids inventing rates).

### 8.3 Likely UI changes after decisions

- Labels: “Shipping”, “COD fee”, “GST” vs “Tax”  
- Messaging for free-shipping threshold  
- COD fee disclosure before Place Order  
- Optional GST-inclusive footnote on PDP/cart  
- Address change → re-preview (already intended)

**No frontend code changes in Step 25.**

---

## 9. Configuration vs database

| Rule type | Recommended home | Rationale |
|---|---|---|
| Simple flat shipping / COD fee / single GST % | **Application config** first (`app.checkout.*`) | Matches current architecture; low ops cost; Flyway not needed for first go-live |
| Free-shipping threshold, FIXED amounts | Config | Same |
| PIN zones, multi-slab rates, per-product HSN | **Database** (+ optional admin UI later) | Too rich for YAML; needs auditability |
| Merchant toggles (enable COD in region) | DB / admin settings later | Operational |

**Recommendation for next implementation step after client answers:**

1. Start with **config-driven** FIXED / ZERO / threshold calculators (extend current components).  
2. Promote to **DB-backed** settings only when rules become multi-row or admin-editable.  
3. Do **not** hard-code rupee amounts in Java source.

---

## 10. Shipping provider (separate concerns)

| Concern | Needed for checkout grand total? | Status |
|---|---|---|
| **A. Shipping price calculation** | Yes | Rule **PENDING** |
| **B. Courier / provider integration** | No (unless client insists rates come from API) | **PENDING** |
| **C. Shipment creation (AWB)** | No — post-order fulfillment | **PENDING** |
| **D. Tracking** | No — post-order / guest tracking | **PENDING** |

**Conclusion:** Immediate checkout can ship with a **confirmed tariff calculator** without selecting Delhivery/Shiprocket/etc. Do not assume a courier.

---

## 11. Edge cases — expected behaviour

| Case | Expected behaviour |
|---|---|
| Free shipping order | If client confirms threshold/rule: calculator returns 0; still snapshot `shipping_charge=0` |
| COD order | Preview includes COD fee when configured; place without Razorpay; `COD_PENDING` |
| Prepaid (UPI/Card/NB/Pay Later) | `cod_charge=0`; Razorpay amount = `grand_total` |
| Pay Later | Same total pipeline; initiation gated by `RAZORPAY_PAY_LATER_ENABLED` |
| Zero shipping / zero COD fee | Allowed **only** if client confirms ₹0 or free shipping — not by inventing |
| Tax rounding | Apply client-confirmed rounding; until then keep scale-2 HALF_UP as engineering default for configured amounts only |
| Address / state change | Re-run preview; invalidate old `previewHash` |
| Cart price change | `PRICE_CHANGED` / `CHECKOUT_REVIEW_REQUIRED`; no order |
| Product hidden mid-checkout | Preview invalid; no order |
| Payment failure retry | Order recoverable; re-initiate payment; cart kept until capture |
| Duplicate Place Order | Same `idempotencyKey` → same order; no duplicate |

---

## 12. Business decision table

| Decision | Current status | Required before |
|---|---|---|
| Shipping charge model (flat / PIN / weight / value / free) | **PENDING** | Production shipping amounts on checkout |
| Free shipping threshold | **PENDING** | Free-shipping messaging & calculator |
| Shipping provider / courier | **PENDING** | Shipment creation & live rate APIs (not strictly for flat tariff) |
| GST applicability (charge GST on site?) | **PENDING** | Production tax line / compliance |
| GST rate(s) | **PENDING** | Tax calculator |
| GST-inclusive vs exclusive pricing | **PENDING** | PDP truth + tax math |
| HSN / SAC | **PENDING** | Per-line invoice / product tax masters |
| CGST+SGST vs IGST treatment | **PENDING** | State-based tax; invoice breakdown |
| COD fee amount | **PENDING** | Production COD path |
| COD fee calculation (flat / % / tier) | **PENDING** | COD calculator |
| COD restrictions (PIN, min order, products) | **PENDING** / **NOT SPECIFIED** | COD eligibility checks |
| Tax on shipping | **NOT SPECIFIED** | Tax calculator inputs |
| Tax on COD fee | **NOT SPECIFIED** | Tax calculator inputs |
| Rounding rules | **NOT SPECIFIED** (code uses scale 2 HALF_UP) | Compliance-grade invoices |
| Pay Later **method** | **CONFIRMED REQUIRED** | — (already in checkout) |
| Pay Later merchant / rail enablement | **PENDING** | Enabling Pay Later in Razorpay Dashboard |

---

## 13. Implementation plan (future — do not execute in Step 25)

1. **Finalize business rules** with the client using §12 (shipping model, COD fee, GST applicability/rate/inclusivity at minimum).  
2. **Update calculation services** — replace PENDING placeholders with confirmed algorithms; keep interfaces.  
3. **Add migration only if needed** — e.g. HSN / CGST-SGST-IGST / line tax (skip if order-level `tax_amount` remains enough).  
4. **Update checkout preview** — inputs (address/PIN) if location-based; pendingRules messaging.  
5. **Update order creation** — continue snapshotting header money fields; extend item snapshots if migrated.  
6. **Update checkout UI** — labels, free-shipping copy, GST footnote, COD fee disclosure.  
7. **Add tests** — each method’s totals; free shipping; COD fee; tax rounding; stale preview; idempotency.  
8. **Regression** — cart, Razorpay initiate/verify, COD place, guest + customer, Pay Later gate.  
9. **Separate later epic** — courier integration, shipment, tracking, GST invoice PDF, admin “COD collected”.

**Suggested minimum client answers to unblock a “charges go-live” step:**

1. Shipping: flat amount **or** free over ₹X **or** “₹0 shipping for now” (explicit).  
2. COD fee: flat ₹Y **or** Z% **or** “₹0 COD fee” (explicit).  
3. GST: not charged on site **or** inclusive at R% **or** exclusive at R% (and whether to show a separate line).

---

## 14. Compatibility with Steps 21–24

| Area | Compatible? |
|---|---|
| Preview hash binding all charge components | Yes |
| Fail-closed PENDING modes | Yes |
| Local ZERO for testability | Yes (must not be mistaken for production policy) |
| V9 money columns | Yes for aggregate charges |
| Razorpay amount = `grand_total` | Yes — once production charges are configured, prepaid amount follows automatically |
| COD outside Razorpay | Yes |

---

## 15. Explicit non-goals of Step 25

- No application code changes  
- No Flyway migrations  
- No config value changes in this step’s deliverable (analysis doc only)  
- No invented ₹ amounts or GST %  
- No courier vendor selection  
- No marking shipping/GST/COD rules as IMPLEMENTED  

---

*End of Step 25 analysis.*

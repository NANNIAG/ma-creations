# MA CREATIONS — Payment Provider Research & Architecture

**Step:** 22 — RESEARCH + ARCHITECTURE; **Step 23 — Razorpay payment foundation IMPLEMENTED**  
**Date:** 2026-09-22  
**Status:** Razorpay selected and integrated (V10, adapter, initiate/verify/webhook). Place-order + checkout UI **IMPLEMENTED** (Step 24).

### Step 23 implementation note

| Item | Status |
|------|--------|
| Payment gateway selection | **CONFIRMED — Razorpay** |
| V10 `payment_transaction` | **IMPLEMENTED** |
| `PaymentGateway` + `RazorpayPaymentGateway` | **IMPLEMENTED** |
| `POST /api/payments/initiate` | **IMPLEMENTED** |
| `POST /api/payments/verify` | **IMPLEMENTED** |
| `POST /api/payments/webhook/razorpay` | **IMPLEMENTED** (signature + idempotency) |
| Pay Later method | **REQUIRED**; merchant enablement via `RAZORPAY_PAY_LATER_ENABLED` (default false) — **do not invent BNPL brand** |
| Checkout UI / place-order | **IMPLEMENTED** (Step 24) |
| Refunds UI | **PENDING** (adapter `refund()` exists for later) |

**Sources consulted:** `CLIENT_CONFIRMATIONS.md`, `REQUIREMENTS.md`, `CHECKOUT_ORDER_ANALYSIS.md`, `DATABASE_DESIGN.md`, `DEVELOPMENT_PLAN.md`, `API_DESIGN.md`; existing `PaymentMethod` / `PaymentStatus` / `OrderStatus` / V9 schema; official provider documentation (links in §15).

**Legend**

| Tag | Meaning |
|-----|---------|
| **CONFIRMED** | Client business requirement — do not reopen |
| **PENDING** | Client / commercial decision — do not invent |
| **PROPOSED** | Recommended technical design for later implementation |
| **SUPPORTED** | Documented by the provider as available (may require account enablement) |
| **NOT SUPPORTED** | Provider docs do not offer this as a standard PG method |
| **REQUIRES SEPARATE PROVIDER** | Needs an extra product/onboarding beyond base PG |
| **NOT VERIFIED** | Insufficient official detail to claim support |

---

## 1. Current payment requirements

### 1.1 Confirmed checkout methods

| Method | Status | Notes |
|--------|--------|-------|
| UPI | **CONFIRMED REQUIRED** | Real checkout method via selected gateway |
| Credit / Debit Card | **CONFIRMED REQUIRED** | Real checkout method via selected gateway |
| Net Banking | **CONFIRMED REQUIRED** | Real checkout method via selected gateway |
| Pay Later | **CONFIRMED REQUIRED** | Method is required; **provider/integration PENDING** |
| COD | **CONFIRMED REQUIRED** | Additional COD charge applies; **charge rule PENDING** |

### 1.2 Confirmed non-goals / policies

| Item | Status |
|------|--------|
| Website checkout + online payment | **CONFIRMED** |
| WhatsApp as primary payment/order path | **NOT** primary (contact / alternate only) |
| Additional methods later if client asks | **CONFIRMED** policy |

### 1.3 Still PENDING (do not invent)

| Decision | Status |
|----------|--------|
| Payment gateway / provider | **CONFIRMED — Razorpay** |
| Pay Later provider name / rail | **PENDING** (method REQUIRED; enablement pending) |
| COD charge amount / calculation rule | **PENDING** |
| Shipping provider / charges | **PENDING** |
| GST / tax rules | **PENDING** |

### 1.4 Already implemented (foundation)

| Asset | Status |
|-------|--------|
| V9 `orders` / `order_items` / `order_addresses` | **IMPLEMENTED** |
| `PaymentMethod` enum: `UPI`, `CARD`, `NET_BANKING`, `PAY_LATER`, `COD` | **IMPLEMENTED** |
| `PaymentStatus`: `PENDING`, `PAID`, `FAILED`, `COD_PENDING` | **IMPLEMENTED** |
| `OrderStatus` (incl. `PENDING_PAYMENT`, `PLACED`, `PAYMENT_FAILED`) | **IMPLEMENTED** |
| `POST /api/checkout/preview` + HMAC preview hash | **IMPLEMENTED** |
| Pluggable shipping / COD / tax calculators (defaults PENDING / ZERO) | **IMPLEMENTED** placeholders |
| Payment transaction table / gateway adapter / webhooks | **NOT** implemented |
| Place-order API / checkout UI | **NOT** implemented |

Domain rule already encoded: **COD does not use the online payment gateway** (`PaymentMethod` javadoc). Online methods go through a future gateway adapter.

---

## 2. Provider research (India)

Four widely used India payment gateways were researched against **current official documentation**. No provider is selected as the client decision — commercial choice remains **PENDING**.

### 2.1 Razorpay

| Capability | Finding | Source |
|------------|---------|--------|
| UPI | **SUPPORTED** (Intent / QR; Collect deprecated for most merchants from 28 Feb 2026) | [UPI docs](https://razorpay.com/docs/payments/payment-methods/upi/) |
| Cards | **SUPPORTED** (credit/debit networks listed under Supported Methods) | [Supported Methods](https://razorpay.com/docs/payments/payment-gateway/web-integration/standard/configure-payment-methods/supported-methods/) |
| Net Banking | **SUPPORTED** (`method: netbanking`) | [Configuration](https://razorpay.com/docs/payments/payment-gateway/web-integration/standard/configure-payment-methods/understand-configuration/) |
| Pay Later | **SUPPORTED** as Checkout method `paylater`; listed providers include LazyPay (`lazypay`), PayPal (`paypal`); some require Dashboard approval. Simpl appears under Optimizer / EMI² eligibility as a separate routing product | [Pay Later](https://razorpay.com/docs/payments/payment-methods/pay-later), [Supported Methods](https://razorpay.com/docs/payments/payment-gateway/web-integration/standard/configure-payment-methods/supported-methods/), [Simpl Optimizer](https://razorpay.com/docs/payments/optimizer/simpl/) |
| Orders / payment intent | **SUPPORTED** — create Order server-side (`POST /v1/orders`), pass `order_id` to Checkout | [Create Order](https://razorpay.com/docs/api/orders/create/), [Java integration](https://razorpay.com/docs/payments/server-integration/java/integration-steps/) |
| Webhooks | **SUPPORTED** — separate Test/Live URLs; signature verification; events include `order.paid`, `payment.captured`, `payment.failed`, refund events | [Webhooks](https://razorpay.com/docs/webhooks), [All events](https://razorpay.com/docs/webhooks/all) |
| Refunds | **SUPPORTED** (API + webhooks); Pay Later / EMI instant refunds **not** supported per Pay Later docs | [Pay Later](https://razorpay.com/docs/payments/payment-methods/pay-later), [Refund webhooks](https://razorpay.com/docs/webhooks/refunds) |
| Sandbox / Test mode | **SUPPORTED** — Test Mode keys; mock bank Success/Failure | [Java integration](https://razorpay.com/docs/payments/server-integration/java/integration-steps/) |
| Java / Spring | **SUPPORTED** — official Java SDK + Orders/Payments/Refunds patterns | [Java SDK](https://razorpay.com/docs/payments/server-integration/java/integration-steps/) |
| React | **SUPPORTED** — Standard / Custom Checkout JS; key id only on frontend | Razorpay Checkout docs (linked from payment gateway web integration) |

**Fit notes:** Strong match for UPI + Cards + Net Banking + documented Pay Later rail. Official Java path and webhook verification align with Spring Boot. Provider list for Pay Later can change and often needs **account enablement** — treat concrete BNPL brand as **PENDING** client choice even if gateway is chosen later.

### 2.2 Cashfree Payments

| Capability | Finding | Source |
|------------|---------|--------|
| UPI | **SUPPORTED** (intent, QR, collect) | [Payment methods overview](https://www.cashfree.com/docs/payments/manage/payment-methods/overview) |
| Cards | **SUPPORTED** (domestic / international + Apple Pay noted) | Same |
| Net Banking | **SUPPORTED** (50+ banks; default-available language in docs) | [Payment methods](https://www.cashfree.com/docs/payments/manage/payment-methods) |
| Pay Later | **SUPPORTED** — `paylater` method; documented provider example **LazyPay** (`lazypay`). Cardless EMI listed separately (FlexMoney, ZestMoney, etc.) — **not** the same as Pay Later | [Payment methods](https://www.cashfree.com/docs/payments/manage/payment-methods) |
| Orders API | **SUPPORTED** — `POST /pg/orders`, payment sessions, eligibility APIs | [Create order](https://www.cashfree.com/docs/api-reference/payments/latest/orders/create-order), [Eligibility](https://www.cashfree.com/docs/api-reference/payments/latest/eligibility/get-eligible-payment-methods) |
| Webhooks | **SUPPORTED** — signature via `x-webhook-signature` + `x-webhook-timestamp` | [Webhook signature](https://www.cashfree.com/docs/payments/online/webhooks/signature-verification) |
| Refunds | **SUPPORTED** — create refund within six months; sandbox status simulation | [Create refund](https://www.cashfree.com/docs/api-reference/payments/latest/refunds/create) |
| Sandbox | **SUPPORTED** | [API best practices](https://www.cashfree.com/docs/api-reference/api-best-practices) |
| Java / Spring | **SUPPORTED** — official Java SDK (`cashfree_pg`) with `PGVerifyWebhookSignature` | [Maven Central](https://central.sonatype.com/artifact/com.cashfree.pg.java/cashfree_pg), [Webhook signature](https://www.cashfree.com/docs/payments/online/webhooks/signature-verification) |
| React | **SUPPORTED** — JS SDKs / hosted & embedded checkout patterns in Cashfree docs | Cashfree PG integration docs |

**Fit notes:** Comparable method coverage to Razorpay for MA CREATIONS’ confirmed methods. Pay Later currently documents LazyPay explicitly; other BNPL brands may need enablement — **NOT VERIFIED** beyond documented list without merchant account.

### 2.3 PayU India

| Capability | Finding | Source |
|------------|---------|--------|
| UPI | **SUPPORTED** (`pg=UPI`) | [Payment mode codes](https://docs.payu.in/docs/payment-mode-codes) |
| Cards | **SUPPORTED** (`CC` / `DC`) | Same |
| Net Banking | **SUPPORTED** (`NB`) | [Net Banking](https://docs.payu.in/docs/collect-payments-with-net-banking-seamless) |
| Pay Later / BNPL | **SUPPORTED** as mode `BNPL` (Buy Now Pay Later); provider via `bankcode` (e.g. LazyPay examples in API refs) | [BNPL intro](https://docs.payu.in/docs/payu-bnpl-integration-introduction), [Payment APIs addl info](https://docs.payu.in/reference/addl_info-payment-apis) |
| Webhooks / verification | **SUPPORTED** (PayU docs cover webhooks + verification APIs) | PayU developer docs |
| Refunds | **SUPPORTED** (documented in PayU PG product surface) | PayU payment gateway product pages / docs |
| Sandbox | **SUPPORTED** (`test.payu.in` flows in docs) | [Net Banking test notes](https://docs.payu.in/docs/collect-payments-with-net-banking-seamless) |
| Java / Spring | **SUPPORTED** via REST; official Java SDK availability **NOT VERIFIED** in this pass (HTTP integration is sufficient) | PayU API docs |
| React | **SUPPORTED** — hosted / merchant-hosted checkout patterns | PayU Web Checkout docs |

**Fit notes:** Mature India PG with explicit BNPL mode. Hash-based request signing is a common PayU pattern — adapter must encapsulate that; domain must not leak PayU `pg` / `bankcode` names into `PaymentMethod`.

### 2.4 PhonePe Payment Gateway

| Capability | Finding | Source |
|------------|---------|--------|
| UPI | **SUPPORTED** | [PhonePe PG](https://developer.phonepe.com/payment-gateway), [Standard Checkout](https://developer.phonepe.com/payment-gateway/website-integration/standard-checkout/api-integration/api-integration-website) |
| Cards | **SUPPORTED** | Same + business Standard Checkout FAQ |
| Net Banking | **SUPPORTED** (50+ banks per business docs) | [business.phonepe.com/standard-checkout](https://business.phonepe.com/standard-checkout) |
| Classic multi-brand Pay Later (LazyPay / Simpl style) | **NOT VERIFIED** as a dedicated `PAY_LATER` method list in PG docs reviewed | — |
| UPI Credit Line (BNPL-like instrument) | **SUPPORTED** as UPI instrument `CREDIT_LINE` (docs describe BNPL / pay-later products on UPI) | [Supported values](https://developer.phonepe.com/payment-gateway/website-integration/standard-checkout/api-integration/api-reference/create-payment/supported-values), [Press: Credit Line on UPI](https://www.phonepe.com/press/phonepe-enables-credit-line-on-upi-on-its-platform/) |
| Webhooks + order status | **SUPPORTED** — create pay, order status, refund APIs; sandbox base URL documented | [Website Checkout](https://developer.phonepe.com/payment-gateway/website-integration/standard-checkout/api-integration/api-integration-website) |
| Refunds | **SUPPORTED** (`POST /payments/v2/refund`) | Same |
| Sandbox | **SUPPORTED** (`api-preprod.phonepe.com`) | Same |
| Java / Spring | REST **SUPPORTED**; dedicated Spring SDK **NOT VERIFIED** in this pass | Same |
| React | **SUPPORTED** — Standard Checkout redirect / website integration | Same |

**Fit notes:** Strong for UPI + Cards + Net Banking. **Pay Later as required by MA CREATIONS** must not silently be equated to UPI Credit Line without client confirmation — product semantics differ (credit line on UPI vs named BNPL providers). Treat classic Pay Later coverage as **NOT VERIFIED** / possibly **REQUIRES SEPARATE PROVIDER** until PhonePe account confirms merchant enablement of equivalent options.

---

## 3. Pay Later research

### 3.1 Definitions (do not conflate)

| Term | Meaning for this project |
|------|--------------------------|
| **Pay Later** | Confirmed checkout **method** `PaymentMethod.PAY_LATER` — customer buys now, settles later with a BNPL / pay-later provider |
| **BNPL** | Industry label overlapping Pay Later; provider docs often use BNPL interchangeably |
| **Card EMI** | Installments on a credit/debit card — **not** automatically Pay Later |
| **Cardless EMI** | EMI without card (e.g. ZestMoney / FlexMoney style) — **not** automatically Pay Later |
| **UPI Credit Line** | Credit-line instrument on UPI — BNPL-*like*; **not** proven equivalent to client “Pay Later” without confirmation |

### 3.2 Gateway-native vs separate

| Gateway | Pay Later / BNPL via same PG? | Notes |
|---------|-------------------------------|-------|
| Razorpay | **SUPPORTED** native Checkout method `paylater` | Providers documented: LazyPay, PayPal; Simpl via Optimizer may be **REQUIRES SEPARATE** Optimizer/onboarding path |
| Cashfree | **SUPPORTED** native `paylater` | Documented provider: LazyPay; cardless EMI is a **separate** category |
| PayU | **SUPPORTED** as `BNPL` mode | Provider selected via bank codes — enablement **PENDING** per merchant |
| PhonePe | Classic Pay Later brands **NOT VERIFIED**; UPI `CREDIT_LINE` **SUPPORTED** | Client must confirm if Credit Line satisfies “Pay Later” |

### 3.3 Recommendation for architecture (not a provider selection)

- Keep domain enum **`PAY_LATER`** provider-agnostic.
- Adapter maps `PAY_LATER` → gateway-specific method codes (`paylater` / `BNPL` / etc.).
- Store optional `provider_method_detail` (e.g. `lazypay`) on **payment transaction**, not as Order domain enum values.
- **Do not** implement Pay Later until both **gateway** and **Pay Later brand** are client-confirmed.

---

## 4. Provider comparison

| Provider | UPI | Cards | Net Banking | Pay Later | Webhooks | Refunds | Sandbox | Java/Spring | React |
|----------|-----|-------|-------------|-----------|----------|---------|---------|-------------|-------|
| **Razorpay** | SUPPORTED | SUPPORTED | SUPPORTED | SUPPORTED (account enablement; provider list limited in docs) | SUPPORTED | SUPPORTED | SUPPORTED | SUPPORTED | SUPPORTED |
| **Cashfree** | SUPPORTED | SUPPORTED | SUPPORTED | SUPPORTED (LazyPay documented; others NOT VERIFIED) | SUPPORTED | SUPPORTED | SUPPORTED | SUPPORTED | SUPPORTED |
| **PayU** | SUPPORTED | SUPPORTED | SUPPORTED | SUPPORTED (BNPL mode) | SUPPORTED | SUPPORTED | SUPPORTED | SUPPORTED via REST (SDK NOT VERIFIED) | SUPPORTED |
| **PhonePe PG** | SUPPORTED | SUPPORTED | SUPPORTED | NOT VERIFIED as classic Pay Later; UPI Credit Line SUPPORTED | SUPPORTED | SUPPORTED | SUPPORTED | SUPPORTED via REST (SDK NOT VERIFIED) | SUPPORTED |

**COD column:** For all providers → **NOT APPLICABLE** to online PG (COD stays in-app).

**Important:** This table does **not** pick a winner. Fees, settlement SLA, MDR, KYC friction, and Pay Later brand availability are **commercial** and **PENDING**.

---

## 5. Recommended architecture (provider-independent)

### 5.1 Principles

1. Business / order layer depends only on **MA CREATIONS** types (`PaymentMethod`, `PaymentStatus`, amounts in `DECIMAL`).
2. Provider SDKs live **only** inside adapter packages.
3. Frontend never asserts payment success; it may show “processing” until backend confirms.
4. Order amounts always recomputed server-side (reuse checkout preview rules + snapshots).
5. COD bypasses `PaymentGateway` entirely.

### 5.2 Component sketch

```
Frontend (React)
   ↓  place order / initiate payment (auth + cart tokens only; no money trust)
Backend OrderService / PaymentService
   ↓
PaymentGateway (interface)
   ↓
RazorpayAdapter | CashfreeAdapter | PayUAdapter | PhonePeAdapter | (future)
   ↓
Payment Provider APIs

Webhook:
Provider → POST /api/payments/webhooks/{provider}
        → Signature verification
        → PaymentWebhookHandler
        → PaymentTransaction update (idempotent)
        → Order.payment_status / Order.status update
```

### 5.3 Proposed interfaces (conceptual — not implemented)

| Type | Responsibility |
|------|----------------|
| `PaymentGateway` | `createPayment(...)`, `fetchPaymentStatus(...)`, `refund(...)` (later), `verifyWebhook(...)` |
| `PaymentService` | Orchestrates order + transaction rows; chooses gateway from config; never called for COD capture |
| `PaymentRequest` | Internal DTO: orderId, amount, currency, method, customer contact, idempotencyKey, return URLs |
| `PaymentResult` | Internal DTO: provider refs, redirect/checkout payload, status |
| `PaymentWebhookHandler` | Verify → normalize event → apply status transition |

### 5.4 Mapping confirmed methods → architecture

| Domain `PaymentMethod` | Online gateway? | Mapping rule |
|------------------------|-----------------|--------------|
| `UPI` | Yes | Adapter → provider UPI instrument |
| `CARD` | Yes | Adapter → cards |
| `NET_BANKING` | Yes | Adapter → netbanking |
| `PAY_LATER` | Yes | Adapter → paylater/BNPL; store sub-provider code on transaction |
| `COD` | **No** | `CodPaymentHandler`: create order `PLACED` + `COD_PENDING`; apply COD charge from calculator when rule CONFIRMED |

Do **not** hard-code Razorpay `upi` / PayU `NB` / Cashfree `lazypay` into `orders.payment_method`.

---

## 6. Payment transaction model (future V10 — **do not create yet**)

### 6.1 Purpose

Separate **payment attempts** from **orders**. One order may have multiple payment attempts (retry after failure); webhooks attach to transactions.

### 6.2 Proposed table `payment_transaction` (PROPOSED)

| Field | Type | Null | Purpose |
|-------|------|------|---------|
| `id` | BIGINT PK | NO | Surrogate |
| `order_id` | BIGINT FK → orders | NO | Owning order |
| `provider` | VARCHAR(32) | NO | Configured gateway id: `RAZORPAY` / `CASHFREE` / `PAYU` / `PHONEPE` / `NONE` (COD) |
| `provider_order_id` | VARCHAR(128) | YES | Provider order/session id |
| `provider_payment_id` | VARCHAR(128) | YES | Provider payment/transaction id |
| `payment_method` | VARCHAR(32) | NO | Domain method (`UPI`…`COD`) |
| `provider_method_detail` | VARCHAR(64) | YES | e.g. `lazypay`, bank code — opaque |
| `amount` | DECIMAL(12,2) | NO | Charged amount (INR) |
| `currency` | CHAR(3) | NO | Default `INR` |
| `status` | VARCHAR(32) | NO | `CREATED` / `PENDING` / `AUTHORIZED` / `CAPTURED` / `FAILED` / `REFUNDED` / `CANCELLED` (map → order `PaymentStatus`) |
| `idempotency_key` | VARCHAR(64) | NO | Client/server retry key (unique) |
| `provider_raw_ref` | VARCHAR(512) | YES | Non-sensitive reference / receipt only — **not** full card payload |
| `failure_code` | VARCHAR(64) | YES | Normalized / provider code |
| `webhook_event_id` | VARCHAR(128) | YES | Last processed event id (dedupe aid) |
| `created_at` | DATETIME(6) | NO | |
| `updated_at` | DATETIME(6) | NO | |

### 6.3 Constraints / indexes (PROPOSED)

- `UNIQUE (idempotency_key)`
- `UNIQUE (provider, provider_payment_id)` where `provider_payment_id` IS NOT NULL (partial unique if MySQL version allows; else enforce in service)
- `KEY (order_id, created_at)`
- `KEY (status)`
- `FK order_id → orders(id)` RESTRICT/CASCADE per delete policy (prefer RESTRICT)

### 6.4 Money

All money columns **DECIMAL(12,2)** — consistent with V9. Never float/double.

### 6.5 What not to store

- Card number, CVV, full PAN, UPI PIN, raw 3DS payloads, unredacted webhook bodies with sensitive fields in logs.

---

## 7. Prepaid flow (online methods)

```
Cart (guest token XOR customer JWT)
  → POST /api/checkout/preview  (existing; server prices)
  → POST /api/orders            (future: create order PENDING_PAYMENT + snapshots)
  → POST /api/payments/initiate (future: create PaymentTransaction + PaymentGateway.createPayment)
  → Provider Checkout (redirect / JS checkout)
  → Customer pays
  → Provider webhook (authoritative)
       → verify signature
       → update PaymentTransaction
       → if captured: Order.payment_status=PAID, Order.status=PLACED (or PROCESSING per policy)
  → Frontend return URL
       → GET payment/order status from **backend** (never trust query ?success=true alone)
```

**Rules**

- Frontend must **never** be trusted to declare payment successful.
- Prefer webhook as source of truth; on return URL, **fetch** provider/payment status server-side if webhook lag.
- Amount on provider order must equal server `grand_total` (or payable online portion) at creation time.
- `preview_hash` (existing) should be re-validated at place-order before creating payment.

---

## 8. COD flow (separate)

```
Cart
  → Checkout preview (paymentMethod=COD)
  → COD charge from CodChargeCalculator when rule CONFIRMED (today: PENDING placeholder)
  → Create Order
       status = PLACED
       payment_status = COD_PENDING
       payment_method = COD
  → Optional PaymentTransaction with provider=NONE, status=COD_PENDING (or skip row)
  → No online gateway call
```

Do **not** route COD through Razorpay/Cashfree/PayU/PhonePe unless a future **client-confirmed** business rule requires it.

---

## 9. Webhook flow

```
Provider HTTP POST
  → /api/payments/webhooks/{provider}   (public; no user JWT)
  → read raw body
  → verify signature (provider secret)
  → parse event → internal PaymentEvent
  → idempotent apply:
        if event already processed → 200 OK no-op
        else update PaymentTransaction + Order
  → 200 quickly; heavy work sync/short
```

**Edge cases**

| Case | Handling (PROPOSED) |
|------|---------------------|
| Duplicate webhook | Dedupe on `webhook_event_id` / provider payment id + status |
| Webhook before frontend redirect | Order already PAID; UI polls status → success |
| Frontend redirect without webhook | Backend status fetch / reconcile job |
| Failed payment webhook | `PaymentTransaction=FAILED`; order `PAYMENT_FAILED` or remain `PENDING_PAYMENT` for retry policy (**PENDING** product rule) |

---

## 10. Security

| Rule | Requirement |
|------|-------------|
| Amounts | Backend calculates; ignore client money fields |
| Payment status | Backend + provider verification only |
| Payment creation | Backend creates provider order/session |
| Webhooks | Signature verification mandatory |
| Webhook idempotency | Mandatory |
| Provider IDs | Unique per provider |
| PCI | Never store card number / CVV; prefer hosted checkout |
| Logging | No secrets, no PAN, no full webhook secrets |
| Config | Env vars only; sandbox ≠ production credentials |
| CORS / keys | Only publishable key on React; secrets server-side |

---

## 11. Idempotency

| Operation | Key | Behavior |
|-----------|-----|----------|
| Place order | `orders.idempotency_key` (V9 already has column) | Same key → same order |
| Create payment | `payment_transaction.idempotency_key` | Same key → same transaction / provider session |
| Browser / network retry | Client resends same Idempotency-Key header | No double charge |
| Webhook | Provider event id | No double status transition |
| Redirect without webhook | Status API | Read-only confirm |

---

## 12. Configuration (PROPOSED names)

Generic env (no real values in repo):

```text
PAYMENT_PROVIDER=                 # e.g. RAZORPAY | CASHFREE | PAYU | PHONEPE | unset
PAYMENT_KEY_ID=
PAYMENT_KEY_SECRET=
PAYMENT_WEBHOOK_SECRET=
PAYMENT_ENV=sandbox|production
# Optional Pay Later brand once chosen:
PAYMENT_PAY_LATER_PROVIDER=       # e.g. lazypay — PENDING
```

Spring maps to `app.payment.*`. **Do not** commit credentials. **Do not** put secrets in frontend `.env`.

---

## 13. Implementation sequence (recommended)

Gateway remains **PENDING**. Implementation can still proceed in adapter-ready slices once the client names a provider — or scaffold interfaces first.

### Step 23 — Payment foundation (after gateway named **or** with stub adapter)

1. Flyway **V10** `payment_transaction` (+ indexes)
2. `PaymentGateway` interface + DTOs
3. One concrete adapter (selected provider) **or** `NoOp`/`Stub` for CI only
4. `PaymentService` initiate + status
5. Webhook endpoint + signature verification + idempotency
6. Map transaction status → `Order.payment_status` / `OrderStatus`
7. Automated tests (signature, idempotency, COD path does not call gateway)

### Step 24 — Order placement + checkout UI

1. `POST /api/orders` (guest + customer) using preview validation / hash
2. Branch: COD vs online initiate payment
3. React checkout UI (methods, address, COD disclosure when rule known)
4. Return / success / failure pages driven by **backend status**
5. Cart convert/clear policy

### Step 25+ (as PENDING items close)

- Enable Pay Later brand in adapter after client confirmation  
- Wire COD charge rule when confirmed  
- Shipping / tax when confirmed  
- Refunds / admin capture tools  

**Dependency note:** Do **not** start Step 23 gateway SDK work until `PAYMENT_PROVIDER` is client-confirmed (per `CLIENT_CONFIRMATIONS.md` §6.1d). Scaffolding interfaces + V10 without a live SDK is acceptable if kept provider-neutral.

---

## 14. Remaining decisions

| Decision | Status |
|----------|--------|
| Payment Gateway | **CONFIRMED — Razorpay** |
| Pay Later Provider | **PENDING** (method CONFIRMED; Razorpay enablement/rail PENDING) |
| UPI | **CONFIRMED** |
| Cards | **CONFIRMED** |
| Net Banking | **CONFIRMED** |
| Pay Later (method) | **CONFIRMED** |
| COD | **CONFIRMED** |
| COD Extra Charge | **IMPLEMENTED** — flat ₹20 when COD |
| Shipping Provider | **PENDING** |
| Shipping Charges | **IMPLEMENTED** — flat ₹20 (V1; not PIN-based) |
| GST/Tax | **IMPLEMENTED** — not charged; prices GST-inclusive; taxAmount=0 |

No confirmed requirement was downgraded to pending.

---

## 15. Source links / references

### Project docs

- `docs/CLIENT_CONFIRMATIONS.md` (§6, §9)
- `docs/REQUIREMENTS.md`
- `docs/CHECKOUT_ORDER_ANALYSIS.md`
- `docs/DATABASE_DESIGN.md`
- `docs/DEVELOPMENT_PLAN.md`
- V9: `V9__create_order_core.sql`
- Enums: `PaymentMethod`, `PaymentStatus`, `OrderStatus`

### Razorpay

- https://razorpay.com/docs/payments/payment-methods/
- https://razorpay.com/docs/payments/payment-methods/upi/
- https://razorpay.com/docs/payments/payment-methods/pay-later
- https://razorpay.com/docs/payments/payment-gateway/web-integration/standard/configure-payment-methods/supported-methods/
- https://razorpay.com/docs/api/orders/create/
- https://razorpay.com/docs/payments/server-integration/java/integration-steps/
- https://razorpay.com/docs/webhooks
- https://razorpay.com/docs/webhooks/all

### Cashfree

- https://www.cashfree.com/docs/payments/manage/payment-methods
- https://www.cashfree.com/docs/payments/manage/payment-methods/overview
- https://www.cashfree.com/docs/api-reference/payments/latest/orders/create-order
- https://www.cashfree.com/docs/payments/online/webhooks/signature-verification
- https://www.cashfree.com/docs/api-reference/payments/latest/refunds/create
- https://central.sonatype.com/artifact/com.cashfree.pg.java/cashfree_pg

### PayU

- https://docs.payu.in/docs/payment-mode-codes
- https://docs.payu.in/docs/payu-bnpl-integration-introduction
- https://docs.payu.in/docs/collect-payments-with-net-banking-seamless
- https://docs.payu.in/reference/addl_info-payment-apis
- https://payu.in/payment-gateway/

### PhonePe

- https://developer.phonepe.com/payment-gateway
- https://developer.phonepe.com/payment-gateway/website-integration/standard-checkout/api-integration/api-integration-website
- https://developer.phonepe.com/payment-gateway/website-integration/standard-checkout/api-integration/api-reference/create-payment/supported-values
- https://business.phonepe.com/standard-checkout
- https://www.phonepe.com/press/phonepe-enables-credit-line-on-upi-on-its-platform/

---

## Document control

| Item | Value |
|------|-------|
| Step | 22 |
| Type | Analysis only |
| Provider selected? | **No** — remains PENDING |
| Code / migrations changed? | **No** |

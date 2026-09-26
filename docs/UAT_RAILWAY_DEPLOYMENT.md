# MA CREATIONS — UAT Backend Deployment on Railway

**Scope:** Backend API (`backend/ma-creations-api`) → Railway, database on **Aiven MySQL UAT**.
**Out of scope for this document:** Vercel frontend deploy, courier integration, invoices, notifications, production cutover.

**Status:** Configuration / runbook only. Does not deploy the service.

---

## 1. Prerequisites

| Prerequisite | Notes |
|---|---|
| GitHub repo | `https://github.com/NANNIAG/ma-creations` |
| Railway account + project | Linked to the GitHub repo |
| Java | **17** (matches `pom.xml` `<java.version>17</java.version>`) |
| Aiven MySQL UAT | Migrations **V1–V11 already applied** |
| Razorpay | **Test/sandbox** keys for UAT |
| Secrets | Set only in Railway Variables — never commit |

### Can this Spring Boot app run on Railway?

**Yes**, with these conditions:

1. Root directory set to `backend/ma-creations-api` (or equivalent Nixpacks/Maven build from that module).
2. `SPRING_PROFILES_ACTIVE=uat` (**not** `local`).
3. All required env vars set (see §3–§7).
4. `server.port` binds to Railway’s `PORT` (configured as `${PORT:${SERVER_PORT:8080}}`).
5. Outbound network access from Railway to Aiven MySQL (SSL).

There is **no** Dockerfile or Maven Wrapper (`mvnw`) in-repo; Railway Nixpacks should detect `pom.xml` and install Maven + JDK 17.

---

## 2. Railway service settings

| Setting | Recommended value |
|---|---|
| **Root Directory** | `backend/ma-creations-api` |
| **Builder** | Nixpacks (default) or Railpack |
| **Build command** | `mvn -DskipTests package` |
| **Start / run command** | `java -Dserver.port=$PORT -jar target/ma-creations-api-0.0.1-SNAPSHOT.jar` |
| **Health check path** | `/api/health` |
| **Health check expected** | HTTP 200, body contains `"status":"UP"` |

### Optional: monorepo root deploy

If Root Directory is the repo root instead:

```text
Build:  cd backend/ma-creations-api && mvn -DskipTests package
Start:  java -jar backend/ma-creations-api/target/ma-creations-api-0.0.1-SNAPSHOT.jar
```

Prefer Root Directory = `backend/ma-creations-api` for simpler paths.

### Note on Flyway packaging

`pom.xml` packages SQL from both:

- `src/main/resources/db/migration`
- `../../database/migrations` → classpath `db/migration`

V1–V11 must be present for Flyway on startup (`ddl-auto: validate`, Flyway enabled). UAT DB already has V1–V11 applied; Flyway should report schema up to date (no destructive re-run of applied versions).

---

## 3. Required environment variables (summary)

Set these in **Railway → Variables** (service scope).

| Variable | Required | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | **Yes** | Must be `uat` |
| `PORT` | Auto (Railway) | Bound by Spring `server.port` |
| `DB_URL` | **Yes** | Aiven JDBC URL |
| `DB_USERNAME` | **Yes** | Aiven user |
| `DB_PASSWORD` | **Yes** | Aiven password |
| `CORS_ALLOWED_ORIGINS` | **Yes** (before frontend UAT) | Comma-separated origins |
| `JWT_SECRET` | **Yes** | Admin JWT (≥ 32 chars) |
| `CUSTOMER_JWT_SECRET` | **Yes** | Customer JWT (≥ 32 chars, **different** from admin) |
| `CHECKOUT_PREVIEW_SECRET` | **Yes** | Preview HMAC binding (≥ strong secret) |
| `RAZORPAY_KEY_ID` | **Yes** for online pay | Test key id |
| `RAZORPAY_KEY_SECRET` | **Yes** for online pay | Test key secret (server only) |
| `RAZORPAY_WEBHOOK_SECRET` | Recommended | Webhook signature verification |
| `ADMIN_BOOTSTRAP_EMAIL` | Optional once | First admin bootstrap |
| `ADMIN_BOOTSTRAP_PASSWORD` | Optional once | First admin bootstrap |
| `UPLOAD_DIR` | Optional | Defaults to `uploads/products` (ephemeral on Railway) |

Do **not** set `CUSTOMER_OTP_DEV_FIXED` or enable OTP logging on UAT.

---

## 4. Aiven MySQL environment variables

Example shape (replace with real Aiven UAT values — **do not commit**):

```text
DB_URL=jdbc:mysql://YOUR_AIVEN_HOST:YOUR_PORT/YOUR_DB?sslMode=REQUIRED&serverTimezone=UTC
DB_USERNAME=avnadmin
DB_PASSWORD=********
```

Notes:

- Prefer `sslMode=REQUIRED` (or Aiven’s documented SSL params) for public UAT hosts.
- Do **not** use `createDatabaseIfNotExist=true` against managed Aiven (DB already exists).
- `spring.jpa.hibernate.ddl-auto=validate` — schema changes only via Flyway; V1–V11 already applied.
- Confirm Railway can reach Aiven (IP allowlist / public access as configured on Aiven).

---

## 5. CORS (future Vercel frontend)

Configured via:

```text
CORS_ALLOWED_ORIGINS=https://your-uat-app.vercel.app,http://localhost:5173
```

- Comma-separated list; no trailing slash.
- Update this variable when the Vercel UAT URL is known.
- Allowed headers include `Authorization`, `Content-Type`, `Accept`, `X-Cart-Token`, `X-Wishlist-Token` (required for guest cart/wishlist from the browser).
- Credentials allowed; do not use `*` origins.

Until Vercel is live, you may temporarily leave localhost for smoke tests only.

---

## 6. Razorpay test-mode variables

```text
PAYMENT_PROVIDER=RAZORPAY
RAZORPAY_KEY_ID=rzp_test_********
RAZORPAY_KEY_SECRET=********
RAZORPAY_WEBHOOK_SECRET=********
RAZORPAY_PAY_LATER_ENABLED=false
```

| Rule | Detail |
|---|---|
| UAT keys | **Test** mode only (`rzp_test_…`) |
| Secrets | Server-side only — never in frontend `VITE_*` |
| Webhook | Point Razorpay test webhook to `https://YOUR_RAILWAY_HOST/api/payments/webhook/razorpay` when testing capture/fail events |
| Pay Later | Keep `false` until merchant BNPL is confirmed |

Checkout charge modes inherit V1 defaults from `application.yml` (shipping FIXED ₹20, COD FIXED ₹20, tax ZERO) — no override required for UAT unless intentionally testing.

---

## 7. Admin / customer JWT variables

```text
JWT_SECRET=<random-string-at-least-32-chars>
JWT_EXPIRATION_MS=86400000
CUSTOMER_JWT_SECRET=<different-random-string-at-least-32-chars>
CUSTOMER_JWT_EXPIRATION_MS=86400000
CUSTOMER_OTP_EXPIRY_MINUTES=5
CUSTOMER_OTP_MAX_ATTEMPTS=5
```

| Rule | Detail |
|---|---|
| Length | Both secrets must be ≥ 32 characters (enforced at startup) |
| Separation | Admin and customer secrets **must** differ |
| Profile | With `uat`, fixed OTP `123456` is **off** |
| SMS | Real SMS provider still PENDING — OTP delivery for UAT may still rely on temporary logging or a future SMS adapter; do not enable `CUSTOMER_OTP_DEV_FIXED` in UAT |

Optional first admin (only when `admin_user` is empty):

```text
ADMIN_BOOTSTRAP_EMAIL=uat-admin@example.com
ADMIN_BOOTSTRAP_PASSWORD=<strong-password>
```

Remove or rotate bootstrap password after first successful login.

Also set:

```text
CHECKOUT_PREVIEW_SECRET=<strong-random-secret>
```

---

## 8. Upload / product image storage limitation

| Item | Behavior on Railway |
|---|---|
| Implementation | `LocalFileProductImageStorage` — local disk under `UPLOAD_DIR` |
| Default path | `uploads/products` |
| Public URLs | `/api/media/{filename}` |
| **UAT limitation** | Railway filesystem is **ephemeral**. Redeploys / restarts **lose uploaded product images** unless an external volume or object storage is added later. |
| UAT mitigation | Re-upload images after redeploy, or accept catalog image loss until object storage is confirmed |
| Out of scope | S3 / R2 / persistent volume — not required for this Railway prep step |

Database rows for products remain on Aiven; only binary files on disk are at risk.

---

## 9. Health-check endpoint

| Property | Value |
|---|---|
| Method / path | `GET /api/health` |
| Auth | None (`permitAll`) |
| Success | `200` + `{"status":"UP"}` |

Use this as Railway’s health check path so traffic is not routed before the JVM is ready.

---

## 10. Profile safety (`local` vs `uat`)

| Profile | Use |
|---|---|
| `local` | Developer machines only — fixed OTP `123456`, OTP log, SQL logging |
| `uat` | Railway UAT — no fixed OTP, quieter logging |
| (unset / default) | Falls back to `local` via `application.yml` — **unsafe for Railway** |

**Railway must set:** `SPRING_PROFILES_ACTIVE=uat`

---

## 11. Secrets hygiene

| Check | Status |
|---|---|
| `.env` gitignored | Yes (root + `backend/ma-creations-api/.env`) |
| Uploads gitignored | Yes |
| Templates only | `backend/ma-creations-api/.env.example` (no real secrets) |
| Do not commit | Aiven passwords, Razorpay secrets, JWT secrets, bootstrap passwords |

---

## 12. Post-deployment verification checklist

After the first successful Railway deploy:

1. [ ] `GET https://YOUR_RAILWAY_HOST/api/health` → `200` / `UP`
2. [ ] App logs show Flyway current version **11** (or “up to date”), no migration errors
3. [ ] `GET /api/categories` returns published catalog data from Aiven
4. [ ] Admin login with bootstrap or existing UAT admin works
5. [ ] Admin can open products list; upload image works **until** next redeploy (document ephemeral disk)
6. [ ] Customer OTP flow works under UAT rules (no fixed `123456` unless you knowingly broke profile)
7. [ ] Guest cart with `X-Cart-Token` works once CORS includes the Vercel origin
8. [ ] Checkout preview + place order (COD) succeeds against Aiven
9. [ ] Razorpay test initiate/verify (if keys set) does not expose secrets in responses
10. [ ] Guest track: `/api/orders/track?orderNumber=&mobileNumber=` works for a guest order
11. [ ] Confirm `SPRING_PROFILES_ACTIVE=uat` in Railway variables
12. [ ] Confirm no `CUSTOMER_OTP_DEV_FIXED=123456` on Railway

---

## 13. Frontend (later — Vercel)

Not deployed in this step. When ready:

| Frontend var | Value |
|---|---|
| `VITE_API_BASE_URL` | `https://YOUR_RAILWAY_HOST` (no trailing slash) |

Then update Railway `CORS_ALLOWED_ORIGINS` to include the Vercel URL.

---

## 14. Explicit non-goals (still PENDING)

- Courier / shipping provider integration
- Tracking URL / provider fields beyond manual AWB
- Invoices / notifications
- Persistent object storage for images
- Production Razorpay live keys
- Real SMS OTP provider

---

*End of UAT Railway deployment runbook.*

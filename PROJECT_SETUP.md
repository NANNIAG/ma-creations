# MA CREATIONS — Project Setup

Foundation initialization only. Business features (catalog APIs, UI screens, checkout, etc.) are **not** implemented yet.

Planning documents live in [`docs/`](docs/).

---

## Prerequisites

| Tool | Required version | Notes |
|---|---|---|
| **JDK** | **17+** | Spring Boot 3.4.x requires Java 17. This machine has Temurin 17 at `C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot` (PATH may still default to Java 8 — set `JAVA_HOME`). |
| **Maven** | 3.8+ | Verified with Maven 3.8.9 |
| **Node.js** | 20+ (LTS recommended) | Verified with Node v24.x / npm 11.x |
| **MySQL** | 8.x recommended | Database name default: `ma_creations` |

Optional: Git.

---

## Repository layout

```
MA-CREATIONS/
├── backend/ma-creations-api/     Spring Boot API
├── frontend/ma-creations-web/    Vite + React + Tailwind
├── database/migrations/          Flyway SQL (canonical)
├── docs/                         Planning / architecture markdown
└── PROJECT_SETUP.md              This file
```

---

## Environment variables (backend)

Copy the example file:

```text
backend/ma-creations-api/.env.example  →  set in your shell (or a local .env you do not commit)
```

| Variable | Purpose | Example |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active profile | `local` |
| `SERVER_PORT` | API port | `8080` |
| `DB_URL` | JDBC URL | `jdbc:mysql://localhost:3306/ma_creations?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `DB_USERNAME` | MySQL user | `root` |
| `DB_PASSWORD` | MySQL password | *(required — do not commit)* |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins (comma-separated) | `http://localhost:5173` |
| `PAYMENT_PROVIDER` | Online gateway id | `RAZORPAY` |
| `RAZORPAY_KEY_ID` | Razorpay **public** key (test: `rzp_test_…`) | *(test key — do not commit production)* |
| `RAZORPAY_KEY_SECRET` | Razorpay secret — **server only** | *(required for live calls — never expose to frontend)* |
| `RAZORPAY_WEBHOOK_SECRET` | Webhook HMAC secret — **server only** | *(configure in Razorpay Dashboard)* |
| `RAZORPAY_PAY_LATER_ENABLED` | `true` only after Pay Later enabled on merchant account | `false` |

Spring Boot does **not** auto-load `.env` files. Export variables in your shell before starting the API.

**PowerShell example:**

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:SPRING_PROFILES_ACTIVE = "local"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_mysql_password_here"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173"
# Razorpay test credentials (optional until sandbox verification):
# $env:RAZORPAY_KEY_ID = "rzp_test_xxxxxxxx"
# $env:RAZORPAY_KEY_SECRET = "your_test_secret"
# $env:RAZORPAY_WEBHOOK_SECRET = "your_webhook_secret"
# $env:RAZORPAY_PAY_LATER_ENABLED = "false"
```

**Never** put `RAZORPAY_KEY_SECRET` or `RAZORPAY_WEBHOOK_SECRET` in frontend env files.
---

## Environment variables (frontend)

| Variable | Purpose | Default |
|---|---|---|
| `VITE_API_BASE_URL` | Backend base URL | `http://localhost:8080` |

Copy `frontend/ma-creations-web/.env.example` to `.env.local` if you need overrides.

---

## MySQL

1. Start MySQL locally.
2. Ensure the user in `DB_USERNAME` can create/use database `ma_creations` (or create it manually).
3. Flyway will run `database/migrations/V1__baseline.sql` on startup (baseline only — **no business tables yet**).

---

## Start the backend

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:DB_PASSWORD = "your_mysql_password_here"

cd D:\Muskan\MA-CREATIONS\backend\ma-creations-api
mvn spring-boot:run
```

API listens on `http://localhost:8080` by default.

### Verify health

```powershell
curl http://localhost:8080/api/health
```

Expected:

```json
{"status":"UP"}
```

---

## Start the frontend

```powershell
cd D:\Muskan\MA-CREATIONS\frontend\ma-creations-web
npm install
npm run dev
```

App listens on `http://localhost:5173` by default.

You should see a **placeholder** page confirming React / Router / Tailwind — not the real storefront.

---

## Flyway notes

- Canonical SQL files: `database/migrations/`
- Maven copies them onto the classpath as `db/migration` at build time.
- `V1__baseline.sql` — Flyway wiring (`SELECT 1`)
- `V2__create_v1_schema.sql` — creates `category`, `product`, `product_image`, `admin_user`
- `V3__seed_categories.sql` — seeds the five PDF categories
- Details: `docs/DATABASE_IMPLEMENTATION.md`

---

## What is intentionally not included yet

- Checkout, payment gateway, shipping, GST, invoices
- Customer order history / guest order tracking
- Real SMS/OTP provider (DevOtpService is local/test only)
- Production deployment config

Customer **mobile OTP authentication** is **IMPLEMENTED**. Guest + customer cart/wishlist ownership and merge are **IMPLEMENTED** (V8). **Order core (V9)**, **checkout preview**, **Razorpay payment foundation (V10)**, and **place-order + checkout UI (Step 24)** are **IMPLEMENTED**. Remaining PENDING: shipping/GST/COD charge rules, order history, guest tracking, admin orders, invoices, notifications.

---

## Decisions needed before the next development step

See also `docs/CLIENT_CONFIRMATIONS.md` and `docs/DATABASE_REVIEW.md` section B.

Blocking or high-value before catalog DDL / APIs:

1. MySQL credentials and that local DB is available for your machine  
2. Admin login identifier shape (email vs username)  
3. Image storage strategy (disk vs S3)  
4. Rating data source (manual / Meesho import / later reviews)  
5. Whether Pay Later / UPI need DB columns or UI-only  
6. Bestsellers / Featured selection rules  
7. Selling model (WhatsApp-first vs web checkout) — affects later order tables, not this init step  

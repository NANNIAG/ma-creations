# MA CREATIONS — Admin Authentication & CMS Implementation

**Step:** 8 — Admin JWT auth + React CMS  
**Public catalog APIs remain unchanged and public.**

---

## ADMIN LOGIN IDENTIFIER

**CLIENT CONFIRMATION REQUIRED** (`CLIENT_CONFIRMATIONS.md` §7.3).

Development-safe default for this step:

- API/UI field name: **email**
- Stored in `admin_user.login_identifier`
- Values normalized to lowercase

Do not treat email as the permanently confirmed production identifier until the client answers.

---

## Authentication flow

```
POST /api/admin/auth/login  { email, password }
        ↓
Spring Security AuthenticationManager + BCrypt
        ↓
JWT (HS256) issued
        ↓
Client stores accessToken (localStorage)
        ↓
Authorization: Bearer <token>
        ↓
POST /api/admin/products  (ROLE_ADMIN required)
```

### Password hashing

- Algorithm: **BCrypt** (`BCryptPasswordEncoder`)
- Plain-text passwords are never stored or logged

### Token / session

| Item | Detail |
|---|---|
| Type | Stateless JWT |
| Header | `Authorization: Bearer …` |
| Secret | `JWT_SECRET` / `app.security.jwt.secret` (≥ 32 chars) |
| Expiry | `JWT_EXPIRATION_MS` (default 86400000 = 24h) |
| React storage | `localStorage` keys `ma_admin_token`, `ma_admin_email` |

Frontend route guards are UX only. **Backend authorization is mandatory.**

---

## Admin APIs

| Method | Path | Auth |
|---|---|---|
| POST | `/api/admin/auth/login` | Public |
| GET | `/api/admin/auth/me` | Admin JWT |
| GET | `/api/admin/products` | Admin JWT |
| GET | `/api/admin/products/{id}` | Admin JWT |
| POST | `/api/admin/products` | Admin JWT |
| PUT | `/api/admin/products/{id}` | Admin JWT |
| PATCH | `/api/admin/products/{id}/status` | Admin JWT — Hide/Publish |
| GET/PUT/POST | `/api/admin/products` … | Admin JWT (no hard DELETE) |

See also `docs/ADMIN_PRODUCT_MANAGEMENT.md` (Step 9 list/edit/delete).

### Login request

```json
{ "email": "admin@example.com", "password": "secret" }
```

### Login success

```json
{
  "data": {
    "accessToken": "…",
    "tokenType": "Bearer",
    "expiresInSeconds": 86400,
    "email": "admin@example.com"
  }
}
```

### Errors

| Code | Status |
|---|---|
| `INVALID_CREDENTIALS` | 401 |
| `VALIDATION_ERROR` | 400 |
| `UNAUTHORIZED` | 401 (missing/invalid token on protected routes) |

### Still public

- `GET /api/categories`
- `GET /api/products`
- `GET /api/products/{id}`
- `GET /api/media/{filename}`
- `GET /api/health`

---

## First admin bootstrap

When `admin_user` is empty, set:

```bash
ADMIN_BOOTSTRAP_EMAIL=admin@macreations.test
ADMIN_BOOTSTRAP_PASSWORD=change-me-now
```

`AdminBootstrapRunner` creates one BCrypt-hashed admin.  
Do **not** commit real passwords. Change the bootstrap password after first login in production scenarios.

---

## Admin React routes

| Path | Page |
|---|---|
| `/admin/login` | Login |
| `/admin` | Dashboard (protected) |
| `/admin/products` | Product list (protected) |
| `/admin/products/new` | Add Product (protected) |
| `/admin/products/:id/edit` | Edit Product (protected) |

### Add Product flow

Login → Dashboard → Add Product → title / category (from API) / image / selling price / MRP → `POST /api/admin/products` → success → dashboard.

Categories loaded from `GET /api/categories` (not hardcoded).

---

## Environment variables

### Backend

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | Signing key (≥ 32 chars) |
| `JWT_EXPIRATION_MS` | Token lifetime |
| `ADMIN_BOOTSTRAP_EMAIL` | Optional first admin |
| `ADMIN_BOOTSTRAP_PASSWORD` | Optional first admin (hashed on save) |
| `DB_*` | Existing MySQL config |

### Frontend

| Variable | Purpose |
|---|---|
| `VITE_API_BASE_URL` | API base URL |

No JWT secrets in the React app.

---

## Unresolved client decisions

- Email vs username for admin login (§7.3)
- Password reset flow
- Multiple admin roles
- Admin theme preference (§4.9)
- Product **Hide/Unpublish** API/UX (§12.1) — **IMPLEMENTED** (`PATCH .../status`, Admin Hide/Publish)
- Multiple images max count (§12.2)

---

## Intentionally not implemented in this step

Customer auth, cart, wishlist, checkout, orders, payments, reviews, coupons, shipping, variants, inventory, fake dashboard metrics.

These commerce items are **CONFIRMED FINAL** business scope for a later phase where noted in `CLIENT_CONFIRMATIONS.md` (not excluded forever).

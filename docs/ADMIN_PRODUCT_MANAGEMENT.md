# MA CREATIONS — Admin Product Management

**Step:** 9 — Admin product list, edit (updated for Hide/Publish)  
**Builds on:** Step 8 admin JWT auth + create CMS; Step 17 Hide/Unpublish  
**Document type:** As-built implementation.

**IMPLEMENTED:** Hide/Unpublish is the normal visibility control (hidden products off storefront, retained in Admin). Permanent delete is **not** part of the normal Admin workflow. Product images remain Admin-upload only (no fake catalog images).

---

## Overview

Admins manage catalog products end-to-end (within confirmed CMS fields):

```
Login → Dashboard → Products list → Edit / Hide / Publish
                 ↘ Add Product → Products list (new products published=true)
```

Public catalog APIs return **published products only**.

---

## Admin routes (React)

| Path | Page |
|---|---|
| `/admin` | Dashboard |
| `/admin/products` | Product list (protected) — status badge + Hide/Publish |
| `/admin/products/new` | Add Product (protected) |
| `/admin/products/:id/edit` | Edit Product (protected) — fields only; visibility via list |

Navigation: **Dashboard · Products · Add Product** + Logout.

---

## API endpoints

| Method | Path | Auth | Notes |
|---|---|---|---|
| `GET` | `/api/admin/products` | `ROLE_ADMIN` | Admin list (published + hidden; includes `published`) |
| `GET` | `/api/admin/products/{id}` | `ROLE_ADMIN` | Detail for edit form (hidden OK) |
| `POST` | `/api/admin/products` | `ROLE_ADMIN` | Create (multipart; `published=true`) |
| `PUT` | `/api/admin/products/{id}` | `ROLE_ADMIN` | Update (multipart; image optional) |
| `PATCH` | `/api/admin/products/{id}/status` | `ROLE_ADMIN` | Body `{ "published": true\|false }` |

Hard `DELETE /api/admin/products/{id}` has been **removed** from the Admin API.

Service layer reuses `ProductService` (no duplicated catalog logic).

### Admin list item fields

- id, title, category `{ id, name }`
- sellingPrice, mrp, discountPercent
- averageRating, ratingCount (nullable)
- primaryImageUrl
- createdAt
- **published** (boolean)

### Status body (JSON)

```json
{ "published": false }
```

### Update body (multipart)

Same confirmed fields as create:

- `title`, `categoryId`, `sellingPrice`, `mrp`
- `image` (optional) — if omitted, existing images are kept

Visibility is **not** edited via the multipart form.

---

## Image handling

Unchanged architecture:

- `ProductImageStorage` / `LocalFileProductImageStorage`
- On **replace image**: new file stored, old `ProductImage` rows orphan-removed, old files deleted best-effort
- On **Hide/Publish**: images and storage files are **not** deleted
- No S3 / cloud migration in this step

---

## Security

- `/api/admin/**` still requires `ROLE_ADMIN` (JWT Bearer)
- Frontend `ProtectedAdminRoute` is UX-only; backend remains authoritative
- Unauthenticated → `401 UNAUTHORIZED`
- Authenticated non-admin → `403 Forbidden`

---

## UI states

- Loading / empty list / API error
- Hide confirmation (`ConfirmDialog`)
- Publish one-click
- Success feedback after Hide / Publish / update
- Row remains in the table after Hide (status badge updates)
- Edit keeps current image unless a new file is chosen

---

## Schema

Flyway **V6** adds `product.published TINYINT(1) NOT NULL DEFAULT 1` and `idx_product_published`.

---

## Intentionally not in this feature

Checkout, customer auth, orders, payments, coupons, inventory, variants, reviews, shipping, guest → customer merge.

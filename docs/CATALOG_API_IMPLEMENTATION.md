# MA CREATIONS — Catalog API Implementation

**Step:** 6 — CURRENT V1 catalog REST APIs  
**Document type:** as-built catalog API snapshot.  
**Auth note:** later superseded by admin JWT (`ADMIN_IMPLEMENTATION.md`).  
**Not included in this step:** cart, wishlist, checkout, payments, reviews write, featured flags, payment gateway APIs, Hide/Unpublish.

**FINAL confirmed (later):** those commerce modules are approved business scope — contracts PENDING. Do not invent gateway endpoints here. Product images remain Admin-upload managed.

---

## Response envelope

Success:

```json
{ "data": ... }
```

Error:

```json
{
  "error": {
    "code": "PRODUCT_NOT_FOUND",
    "message": "Product not found: 99"
  }
}
```

---

## 1. GET /api/categories

| Item | Detail |
|---|---|
| Method | `GET` |
| Auth | Public |
| Params | None |
| Body | None |

**Response `200`**

```json
{
  "data": [
    { "id": 1, "name": "Hydration & Drinkware" },
    { "id": 2, "name": "Lunch & Meal Prep" },
    { "id": 3, "name": "Kitchen Gadgets & Prep" },
    { "id": 4, "name": "Storage & Kitchenware" },
    { "id": 5, "name": "Home Decor & Festivity" }
  ]
}
```

Ordered by `display_order`.

---

## 2. GET /api/products

| Item | Detail |
|---|---|
| Method | `GET` |
| Auth | Public |

**Query parameters**

| Param | Required | Values |
|---|---|---|
| `categoryId` | No | Existing category id |
| `sort` | No (default `newest`) | `price_asc`, `price_desc`, `newest` |

**Examples**

```http
GET /api/products
GET /api/products?categoryId=1
GET /api/products?sort=price_asc
GET /api/products?sort=price_desc
GET /api/products?sort=newest
GET /api/products?categoryId=1&sort=price_asc
```

**Response `200`**

```json
{
  "data": [
    {
      "id": 10,
      "title": "Classy Water Bottles",
      "sellingPrice": 147.00,
      "mrp": 188.00,
      "discountPercent": 22,
      "averageRating": 4.00,
      "ratingCount": 135,
      "primaryImageUrl": "/api/media/uuid.jpg"
    }
  ]
}
```

`discountPercent` = round((mrp − sellingPrice) / mrp × 100).  
`averageRating` / `ratingCount` may be `null`.  
`primaryImageUrl` may be `null` if no images.

**Errors**

| Code | Status | When |
|---|---|---|
| `CATEGORY_NOT_FOUND` | 404 | Unknown `categoryId` |
| `INVALID_SORT` | 400 | Unsupported `sort` |

---

## 3. GET /api/products/{id}

| Item | Detail |
|---|---|
| Method | `GET` |
| Auth | Public |
| Path | `id` — product id |

**Response `200`**

```json
{
  "data": {
    "id": 10,
    "title": "Classy Water Bottles",
    "sellingPrice": 147.00,
    "mrp": 188.00,
    "discountPercent": 22,
    "averageRating": 4.00,
    "ratingCount": 135,
    "category": {
      "id": 1,
      "name": "Hydration & Drinkware"
    },
    "images": [
      {
        "id": 1,
        "url": "/api/media/uuid.jpg",
        "sortOrder": 0
      }
    ]
  }
}
```

**Errors**

| Code | Status | When |
|---|---|---|
| `PRODUCT_NOT_FOUND` | 404 | Unknown id |

---

## 4. POST /api/admin/products

| Item | Detail |
|---|---|
| Method | `POST` |
| Content-Type | `multipart/form-data` |
| Auth | Temporarily public (awaiting client confirmation) |

**Parts / fields (confirmed CMS only)**

| Field | Type | Rules |
|---|---|---|
| `title` | text | Required, non-blank |
| `categoryId` | number | Required; must exist |
| `sellingPrice` | number | Required; ≥ 0 |
| `mrp` | number | Required; ≥ 0 |
| `image` | file | Required; JPEG/PNG/WEBP/GIF |

**Not accepted:** created_at, inventory, variants, featured, bestseller, UPI, Pay Later, reviews.

**Server-generated:** `slug` (from title, unique suffix if needed), `created_at` / `updated_at`, primary image `sort_order = 0`.

**Example (curl)**

```bash
curl -X POST http://localhost:8080/api/admin/products \
  -F "title=Modern Water Bottles" \
  -F "categoryId=1" \
  -F "sellingPrice=147.00" \
  -F "mrp=188.00" \
  -F "image=@./bottle.jpg;type=image/jpeg"
```

**Response `201`** — same shape as product detail.

**Errors**

| Code | Status | When |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Missing/invalid fields or image |
| `CATEGORY_NOT_FOUND` | 404 | Unknown category |
| `IMAGE_STORE_FAILED` | 400 | Local disk write failure |

---

## 5. Image handling

| Item | Detail |
|---|---|
| Abstraction | `ProductImageStorage` |
| Default impl | `LocalFileProductImageStorage` |
| DB column | `product_image.storage_path` (filename/key, not BLOB) |
| Public URL | `/api/media/{filename}` via `MediaController` |
| Config | `app.upload.dir`, `app.upload.public-base-path` |

Replace the storage bean later for S3/CDN without changing catalog services.

---

## 6. Layering

```
Controller → Service → Repository → Entity
              ↕
            Mapper / DTOs
```

---

## 7. Tests

| Type | Class | Needs MySQL? |
|---|---|---|
| Unit | `CatalogUtilsTest` | No |
| Unit | `CategoryServiceTest`, `ProductServiceTest` | No (mocked repos) |
| WebMvc | `CatalogControllerTest` | No (mocked services) |
| Integration | *(not added)* | Yes — see `src/test/.../integration/README.md` |

---

## 8. Out of scope for this step (CURRENT V1 catalog APIs)

Customer auth, cart, wishlist, checkout, orders, payments, coupons, shipping, inventory, variants, review submission, subcategories, hero banners, featured/bestseller APIs, payment gateway persistence, Hide/Unpublish.

Admin auth was added in a later step (`ADMIN_IMPLEMENTATION.md`).

**FINAL business scope** for cart/wishlist/checkout (guest + optional accounts)/payments (required methods: UPI, Credit/Debit Card, Net Banking, COD; gateway PENDING; COD charge rule PENDING)/search/hide is tracked in `CLIENT_CONFIRMATIONS.md` — approved for a later phase; not invented as APIs in this document.

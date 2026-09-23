# MA CREATIONS — Go-Live Readiness Checklist

**Step:** 11 — Client assets, configuration, and local go-live readiness  
**Scope:** Documentation + readiness only. **No production deploy** in this step.

Status legend:

| Status | Meaning |
|---|---|
| **READY** | Implemented and usable with current code (CURRENT V1) |
| **PENDING CLIENT** | Waiting for client asset, value, or confirmation |
| **PENDING TECHNICAL** | Needs hosting/ops or **next implementation phase** work |
| **CONFIRMED FINAL / NOT BUILT** | Business scope approved; not in CURRENT V1 code yet |
| **NOT APPLICABLE** | Not relevant to this checklist row |

**Selling model (CONFIRMED FINAL):** website shopping + checkout + online payment; WhatsApp = contact / alternate buy. Full e-commerce go-live is **CONFIRMED FINAL / NOT BUILT**, not “not applicable.”

---

## A. Environment

| Item | Status | Notes |
|---|---|---|
| Frontend `.env.example` documents all public vars | READY | Includes API, WhatsApp, Instagram, hero, logo, category images |
| Backend `.env.example` documents DB/JWT/bootstrap | READY | Secrets must be set in shell / secret store — Spring does not load `.env` automatically |
| `.env.local` gitignored | READY | Root + frontend gitignore |
| No JWT/DB secrets in `VITE_*` | READY | Public config only |
| `VITE_API_BASE_URL` matches running API | PENDING CLIENT / local ops | Use `http://localhost:8081` when API uses 8081 |
| Production env secrets | PENDING TECHNICAL | Not configured (intentionally) |

---

## B. Database

| Item | Status | Notes |
|---|---|---|
| MySQL schema via Flyway V1–V3 | READY | Auto-migrate on startup |
| Category seed (5 categories) | READY | |
| Local MySQL for development | READY | Machine-dependent |
| Production database | PENDING TECHNICAL | Not provisioned |
| Backups / migrations in prod | PENDING TECHNICAL | |

---

## C. Backend

| Item | Status | Notes |
|---|---|---|
| Health `GET /api/health` | READY | |
| Catalog APIs | READY | Categories, products, media |
| Admin auth JWT + BCrypt | READY | |
| Admin product CRUD | READY | List/create/update/Hide-Publish (no hard delete) |
| CORS for storefront origin | READY | `CORS_ALLOWED_ORIGINS` |
| Port conflict awareness (8080 vs 8081) | READY | Documented |
| Production hosting | PENDING TECHNICAL | Not deployed |

---

## D. Frontend

| Item | Status | Notes |
|---|---|---|
| Storefront Home / PLP / PDP | READY | |
| Design tokens `#FDFBF7` / `#D4A373` / `#2C2C2C` | READY | |
| Empty / loading / error states | READY | |
| No fake catalog products | READY | Empty states when none |
| Font final choice | PENDING CLIENT | Inter vs Montserrat — env override only |
| Production CDN / domain | PENDING TECHNICAL | |

---

## E. Admin

| Item | Status | Notes |
|---|---|---|
| Login `/admin/login` | READY | |
| Dashboard / Products / Add / Edit | READY | |
| Route guard + JWT on API | READY | |
| Admin login identifier shape | PENDING CLIENT | Email used as temporary default |
| Password reset | PENDING CLIENT | Not implemented |
| Bootstrap credentials rotated | PENDING CLIENT | Change after first login |

---

## F. Product management

| Item | Status | Notes |
|---|---|---|
| Fields: title, category, image, selling, MRP | READY | Confirmed CMS scope |
| Validation (required, numeric, image type, selling ≤ MRP) | READY | |
| Category dropdown from API | READY | |
| Soft-hide / unpublish | **IMPLEMENTED** | `product.published`; Admin Hide/Publish; public APIs filter |
| Multi-image gallery admin UX | PENDING CLIENT | Storage supports multiple; create flow uses one |

---

## G. WhatsApp

| Item | Status | Notes |
|---|---|---|
| `VITE_WHATSAPP_NUMBER` support | READY | |
| Safe UI when missing | READY | No fake number |
| Floating widget | READY | |
| Buy via WhatsApp with product message | READY | Title + product id |
| Real business number | **CONFIRMED** `6395700831` | Intl/`wa.me` digit formatting = config verify |
| Role | Contact / alternate buy | **Not** primary order flow (FINAL) |

---

## H. Instagram

| Item | Status | Notes |
|---|---|---|
| Handle / follow URL env | READY | |
| Follow CTA when configured | READY | |
| Non-live placeholders when missing | READY | Explicitly labeled |
| Live Instagram grid / Graph API | PENDING CLIENT | Embed vs API still open |
| Confirmed profile / Follow URL | **CONFIRMED** | `https://www.instagram.com/macreations.living/` |

---

## I. Images / assets

| Item | Status | Notes |
|---|---|---|
| Product images via admin upload (local disk) | READY | |
| Hero folder + JSON config | READY | `public/heroes/` |
| Category tile local images | READY | Map or auto-by-slug |
| Branding logo slot | READY | `public/branding/` + `VITE_BRAND_LOGO_URL` |
| Client hero photography | PENDING CLIENT | |
| Client category photography | PENDING CLIENT | |
| Client logo file | PENDING CLIENT | |
| S3 / cloud media | PENDING CLIENT | Local disk remains |

---

## J. Branding

| Item | Status | Notes |
|---|---|---|
| Text wordmark fallback | READY | |
| Optional logo via env | READY | |
| Color tokens | READY | |
| Final logo | PENDING CLIENT | |
| Final font | PENDING CLIENT | |

---

## K. Responsive testing

| Item | Status | Notes |
|---|---|---|
| Layout targets 390 / 768 / 1440 considered | READY | Tailwind breakpoints + sticky CTA / FAB offsets |
| Manual device QA sign-off | PENDING CLIENT | Client should verify on real devices |
| Admin table horizontal scroll on small screens | READY | `overflow-x-auto` |

---

## L. Security

| Item | Status | Notes |
|---|---|---|
| Public: health, categories, products, media | READY | |
| Admin login public; other `/api/admin/**` = `ROLE_ADMIN` | READY | |
| Unauthenticated admin mutations rejected | READY | Covered by tests |
| Passwords BCrypt hashed | READY | |
| No secrets in frontend bundle | READY | |
| Production JWT secret rotation | PENDING TECHNICAL | |
| HTTPS / TLS termination | PENDING TECHNICAL | |

---

## M. Testing

| Item | Status | Notes |
|---|---|---|
| Backend `mvn test` | READY | Unit + WebMvc |
| Frontend `npm test -- --run` | READY | |
| Backend `mvn package` | READY | |
| Frontend `npm run build` | READY | |
| Full E2E against MySQL in CI | PENDING TECHNICAL | Optional later |

---

## N. Deployment

| Item | Status | Notes |
|---|---|---|
| Local run documented | READY | `PROJECT_SETUP.md` + Step 8/10 docs |
| Production host chosen | PENDING TECHNICAL / CLIENT | |
| Domain / DNS / SSL | PENDING TECHNICAL | |
| CI/CD pipeline | PENDING TECHNICAL | |
| **This step does not deploy** | NOT APPLICABLE | Explicitly out of scope |

---

## O. Client confirmations — status after clarification phase

| Topic | Status | Ref |
|---|---|---|
| Admin login identifier (email vs username) | PENDING CLIENT | §7.3 |
| WhatsApp number | **CONFIRMED** `6395700831` (intl/`wa.me` format = config verify) | §2.1 |
| WhatsApp message templates | PENDING CLIENT | §2.2 |
| Instagram profile / Follow URL | **CONFIRMED** `https://www.instagram.com/macreations.living/` | §3.1 / §3.3 |
| Instagram live grid approach | PENDING CLIENT | §3.2 |
| Inter vs Montserrat | PENDING CLIENT | §4.1 |
| Hero assets | Partially provided | §4.4 |
| Shop Now destination | **PENDING CLIENT** | §4.4 |
| Category creatives | Provided for all 5 | §4.5 |
| Selling model (website checkout + online payment) | **CONFIRMED FINAL** | §1 |
| Search / Wishlist / persistent Cart | **CONFIRMED FINAL** (not CURRENT V1) | §8 / §11 |
| Bestseller / featured rules | **PENDING CLIENT** | §5.10 |
| Product Hide/Unpublish (not hard-delete normal flow) | **IMPLEMENTED** | §12.1 |
| Guest checkout + optional Customer Accounts | **CONFIRMED FINAL** | §7 / §9.1 — auth & guest-tracking mechanisms PENDING design |
| UPI / Card / Net Banking as required online checkout methods | **CONFIRMED FINAL**; gateway/provider PENDING | §6 / §9 |
| Cash on Delivery (COD) | **CONFIRMED FINAL**; additional charge; charge amount/rule PENDING | §6.1c / §9.3 |
| Return policy / payment badge asset set | PENDING CLIENT | §4.6–4.8 |

---

## Configuration quick reference (documented contacts; keep secrets out of git)

```env
# frontend/.env.local (gitignored) — example shape
VITE_API_BASE_URL=http://localhost:8081
VITE_WHATSAPP_NUMBER=916395700831
VITE_INSTAGRAM_HANDLE=macreations.living
VITE_INSTAGRAM_FOLLOW_URL=https://www.instagram.com/macreations.living/
# VITE_BRAND_FONT_FAMILY=
# VITE_BRAND_LOGO_URL=/branding/logo.png
# VITE_HERO_SHOP_NOW_TARGET=#categories
# VITE_HERO_SLIDES_JSON=
# VITE_CATEGORY_IMAGE_MAP_JSON=
# VITE_CATEGORY_IMAGES_AUTO=true
```

Note: client-documented WhatsApp number is **`6395700831`**. Env may use country-code digits for `wa.me` (e.g. `91…`) as a **configuration/verification** item — do not silently rewrite docs to a different number.

Asset drop folders:

- `frontend/ma-creations-web/public/heroes/`
- `frontend/ma-creations-web/public/branding/`
- `frontend/ma-creations-web/public/categories/`

---

## Overall readiness

| Area | Verdict |
|---|---|
| Local demo / client review (catalog + CMS) | **READY** (CURRENT V1) |
| Soft launch with real WhatsApp + Instagram + products | **READY** for contacts; products depend on Admin uploads |
| Full e-commerce go-live (checkout / persistent cart / search / wishlist / payments) | **CONFIRMED FINAL / NOT BUILT** — gateway/provider & storage PENDING |

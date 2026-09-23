# MA CREATIONS documentation

Architecture, planning, and as-built implementation notes.

**How to read these docs**

| Layer | Meaning |
|---|---|
| **CURRENT V1** | Catalog storefront + admin CMS as built (`*_IMPLEMENTATION.md`, go-live checklist) |
| **FINAL confirmed** | Website checkout + payment (**UPI**, **Credit/Debit Card**, **Net Banking**, **COD**); **guest checkout** + optional **customer accounts** — approved, **not all built**. Gateway/provider, COD charge rule, and auth/tracking **mechanisms PENDING design**. **Search**, **guest cart**, **guest wishlist**, and **Hide/Unpublish** are **IMPLEMENTED**. |
| **PENDING** | Gateway/provider names, **COD charge rule**, shipping, auth/tracking mechanism design, cart/wishlist storage, Shop Now destination, bestseller rules |

Authoritative confirmation log: [`CLIENT_CONFIRMATIONS.md`](./CLIENT_CONFIRMATIONS.md).

| Document | Description |
|---|---|
| REQUIREMENTS.md | PDF-extracted requirements + FINAL scope status |
| SYSTEM_ARCHITECTURE.md | Frontend / backend / DB architecture |
| DATABASE_DESIGN.md | Entity notes (planning) |
| DATABASE_ER_DESIGN.md | CURRENT V1 ER + FINAL commerce notes |
| DATABASE_REVIEW.md | Pre-SQL field review |
| DATABASE_IMPLEMENTATION.md | As-built Flyway/JPA (CURRENT V1) |
| API_DESIGN.md | REST module outline |
| CATALOG_API_IMPLEMENTATION.md | As-built catalog APIs |
| USER_WORKFLOW.md | Customer / admin flows |
| FIGMA_SPECIFICATION.md | UI/UX Figma-ready spec |
| CLIENT_CONFIRMATIONS.md | Confirmed + pending client decisions |
| DEVELOPMENT_PLAN.md | Phased delivery plan |
| FRONTEND_CATALOG_IMPLEMENTATION.md | As-built storefront catalog |
| ADMIN_IMPLEMENTATION.md | As-built admin auth + CMS |
| ADMIN_PRODUCT_MANAGEMENT.md | As-built product list/edit/Hide-Publish |
| STOREFRONT_POLISH.md | As-built storefront polish |
| GO_LIVE_CHECKLIST.md | Local readiness checklist |

Setup instructions: see [`../PROJECT_SETUP.md`](../PROJECT_SETUP.md).

# MA CREATIONS — Order Tracking Analysis

**Step:** 26B — analysis + **implementation (manual AWB + guest track)**  
**Status:** V1 manual tracking is **IMPLEMENTED**. Courier integration remains **PENDING**.

**V1 tracking is manual and stores only a tracking/AWB number. Courier integration is intentionally deferred.**

## Implemented (26B)

| Capability | Status |
|---|---|
| Admin manual tracking (`tracking_number` only) | **IMPLEMENTED** |
| `PATCH /api/admin/orders/{orderNumber}/tracking` | **IMPLEMENTED** |
| Customer order detail shows AWB | **IMPLEMENTED** |
| Guest track: order number + mobile | **IMPLEMENTED** (`GET /api/orders/track`) |
| UI `/track-order`, admin tracking form | **IMPLEMENTED** |
| Schema V11 `orders.tracking_number` | **IMPLEMENTED** |

## Still PENDING

| Capability | Status |
|---|---|
| Courier / shipping provider | **PENDING** |
| Courier API / webhooks / auto sync | **PENDING** |
| Tracking URL / provider field | **NOT IN V1** (deferred) |
| Cancellation / refund / return | **PENDING** |
| COD collected → PAID | **PENDING** |
| Invoices / notifications | **PENDING** |

## Security notes (guest)

- Order number alone is insufficient.
- Mobile must match stored normalized contact/guest mobile (`MobileNumberUtils`).
- Registered-customer orders (`customer_id` set) are never returned on the guest path.
- Wrong mobile / registered order / missing order → same `ORDER_NOT_FOUND`.

## Client decisions locked for V1

1. Manual admin tracking — **YES**  
2. Fields — **tracking/AWB number only**  
3. Courier integration — **NOT NOW**  
4. Tracking URL — **NOT REQUIRED**  
5. Guest tracking — **YES**  
6. Guest verification — **order number + mobile**  

See historical options analysis in git history / prior 26B draft if needed; this file now reflects **post-implementation** status.

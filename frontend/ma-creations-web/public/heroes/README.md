# Hero assets (client-provided)

Place production hero images in this folder, then reference them from env.

## Suggested file naming

- `slide-1.jpg` (or `.webp` / `.png`)
- `slide-2.jpg`
- `slide-3.jpg`

## Supported formats

JPEG, PNG, WEBP (any format the browser can display).

## Recommended size

Wide images ~1440×600 or larger, optimized for web (&lt; 400KB each if possible).

## Wire into the app

In `.env.local` (example — do not invent client photography):

```env
VITE_HERO_SHOP_NOW_TARGET=#categories
VITE_HERO_SLIDES_JSON=[{"id":"1","title":"Festive candles","subtitle":"Boutique home essentials","image":"/heroes/slide-1.jpg"},{"id":"2","title":"Hydration","subtitle":"Drinkware favourites","image":"/heroes/slide-2.jpg"}]
```

### Slide JSON fields

| Field | Required | Notes |
|---|---|---|
| `id` | Recommended | Stable key |
| `title` | Recommended | Headline |
| `subtitle` | Optional | Supporting line |
| `image` | Optional | Public path under `/heroes/...` |
| `tone` | Optional | Tailwind gradient classes for placeholder fallback |

### Shop Now destination

| Value | Behavior |
|---|---|
| `#categories` (default) | Smooth-scroll to homepage category section |
| `/categories/1` | Navigate to that PLP route |
| `/` | Navigate home |

Until images are added, the storefront shows clean gradient placeholders (not fake product photos).

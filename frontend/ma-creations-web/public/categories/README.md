# Category tile images (client-provided)

Category circles fall back to colored placeholders until images are configured.

## Option A — explicit map (recommended)

1. Place files here, e.g. `hydration-drinkware.jpg`
2. In `.env.local`:

```env
VITE_CATEGORY_IMAGE_MAP_JSON={"hydration-drinkware":"/categories/hydration-drinkware.jpg","lunch-meal-prep":"/categories/lunch-meal-prep.jpg","kitchen-gadgets-prep":"/categories/kitchen-gadgets-prep.jpg","storage-kitchenware":"/categories/storage-kitchenware.jpg","home-decor-festivity":"/categories/home-decor-festivity.jpg"}
```

Slugs match the database seed (`docs/DATABASE_IMPLEMENTATION.md`).

## Option B — auto by slug

1. Place `{slug}.jpg` files in this folder
2. Set:

```env
VITE_CATEGORY_IMAGES_AUTO=true
```

The UI loads `/categories/{slug}.jpg` and falls back to the color tile if the file is missing.

## Formats

JPEG / PNG / WEBP. Square crops (~400×400) work best for circular tiles.

Do not invent photography — only add real client images.

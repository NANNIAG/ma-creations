-- V3: Seed the five PDF-confirmed categories only.
-- No products. No admin credentials.

INSERT INTO category (name, slug, display_order, created_at, updated_at) VALUES
    ('Hydration & Drinkware', 'hydration-drinkware', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Lunch & Meal Prep', 'lunch-meal-prep', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Kitchen Gadgets & Prep', 'kitchen-gadgets-prep', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Storage & Kitchenware', 'storage-kitchenware', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Home Decor & Festivity', 'home-decor-festivity', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

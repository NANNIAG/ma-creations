-- V6: Product Hide/Unpublish — published flag for storefront visibility.
-- Existing rows receive published = 1 via DEFAULT. No separate backfill.

ALTER TABLE product
    ADD COLUMN published TINYINT(1) NOT NULL DEFAULT 1 AFTER rating_count,
    ADD KEY idx_product_published (published);

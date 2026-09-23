-- V11: Manual order tracking (AWB / tracking number only).
-- Courier provider, tracking URL, and shipment tables intentionally deferred.

ALTER TABLE orders
    ADD COLUMN tracking_number VARCHAR(64) NULL AFTER notes;

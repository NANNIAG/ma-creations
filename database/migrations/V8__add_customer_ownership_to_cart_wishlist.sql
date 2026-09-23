-- V8: Customer-owned cart/wishlist + guest ownership (mutually exclusive).
-- Guest: guest_token set, customer_id NULL
-- Customer: customer_id set, guest_token NULL
-- MERGED guest carts/wishlists keep guest_token for history; customer_id stays NULL.

ALTER TABLE cart
    MODIFY COLUMN guest_token CHAR(36) NULL,
    ADD COLUMN customer_id BIGINT NULL AFTER guest_token,
    ADD CONSTRAINT fk_cart_customer
        FOREIGN KEY (customer_id) REFERENCES customer (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    ADD UNIQUE KEY uk_cart_customer_id (customer_id);

ALTER TABLE wishlist
    MODIFY COLUMN guest_token CHAR(36) NULL,
    ADD COLUMN customer_id BIGINT NULL AFTER guest_token,
    ADD CONSTRAINT fk_wishlist_customer
        FOREIGN KEY (customer_id) REFERENCES customer (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    ADD UNIQUE KEY uk_wishlist_customer_id (customer_id);

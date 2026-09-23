-- V4: Guest shopping cart (ACTIVE / MERGED). No customer_id, checkout, or payment fields.

CREATE TABLE cart (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    guest_token     CHAR(36)     NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_guest_token (guest_token),
    KEY idx_cart_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cart_item (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    cart_id         BIGINT       NOT NULL,
    product_id      BIGINT       NOT NULL,
    quantity        INT          NOT NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_item_cart_product (cart_id, product_id),
    KEY idx_cart_item_cart_id (cart_id),
    KEY idx_cart_item_product_id (product_id),
    CONSTRAINT fk_cart_item_cart
        FOREIGN KEY (cart_id) REFERENCES cart (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_cart_item_product
        FOREIGN KEY (product_id) REFERENCES product (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_cart_item_quantity_min CHECK (quantity >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

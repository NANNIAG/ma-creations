-- V5: Guest wishlist (ACTIVE / MERGED). No customer_id, quantity, or payment fields.

CREATE TABLE wishlist (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    guest_token     CHAR(36)     NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_wishlist_guest_token (guest_token),
    KEY idx_wishlist_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wishlist_item (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    wishlist_id     BIGINT       NOT NULL,
    product_id      BIGINT       NOT NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_wishlist_item_wishlist_product (wishlist_id, product_id),
    KEY idx_wishlist_item_wishlist_id (wishlist_id),
    KEY idx_wishlist_item_product_id (product_id),
    CONSTRAINT fk_wishlist_item_wishlist
        FOREIGN KEY (wishlist_id) REFERENCES wishlist (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_wishlist_item_product
        FOREIGN KEY (product_id) REFERENCES product (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

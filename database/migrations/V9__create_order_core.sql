-- V9: Order core (orders, order_items, order_addresses). No payment tables.
-- Guest: customer_id NULL. Registered: customer_id set from JWT at place-order (later step).
-- Monetary columns are DECIMAL(12,2). Item prices are snapshots at order time.

CREATE TABLE orders (
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    order_number        VARCHAR(32)    NOT NULL,
    customer_id         BIGINT         NULL,
    cart_id             BIGINT         NULL,
    status              VARCHAR(32)    NOT NULL,
    payment_status      VARCHAR(32)    NOT NULL,
    payment_method      VARCHAR(32)    NULL,
    currency            CHAR(3)        NOT NULL DEFAULT 'INR',
    items_subtotal      DECIMAL(12,2)  NOT NULL,
    shipping_charge     DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    cod_charge          DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    tax_amount          DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    discount_amount     DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    grand_total         DECIMAL(12,2)  NOT NULL,
    contact_name        VARCHAR(150)   NOT NULL,
    contact_mobile      VARCHAR(20)    NOT NULL,
    contact_email       VARCHAR(255)   NULL,
    guest_mobile        VARCHAR(20)    NULL,
    guest_email         VARCHAR(255)   NULL,
    preview_hash        VARCHAR(128)   NULL,
    idempotency_key     VARCHAR(64)    NULL,
    notes               VARCHAR(500)   NULL,
    placed_at           DATETIME(6)    NOT NULL,
    confirmed_at        DATETIME(6)    NULL,
    cancelled_at        DATETIME(6)    NULL,
    created_at          DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_order_number (order_number),
    UNIQUE KEY uk_orders_idempotency_key (idempotency_key),
    KEY idx_orders_customer_placed (customer_id, placed_at),
    KEY idx_orders_status (status),
    KEY idx_orders_payment_status (payment_status),
    KEY idx_orders_contact_mobile (contact_mobile),
    KEY idx_orders_guest_mobile (guest_mobile),
    CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id) REFERENCES customer (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_items (
    id                      BIGINT         NOT NULL AUTO_INCREMENT,
    order_id                BIGINT         NOT NULL,
    product_id              BIGINT         NULL,
    product_title_snapshot  VARCHAR(255)   NOT NULL,
    product_slug_snapshot   VARCHAR(255)   NULL,
    unit_selling_price      DECIMAL(12,2)  NOT NULL,
    unit_mrp_snapshot       DECIMAL(12,2)  NULL,
    quantity                INT            NOT NULL,
    line_subtotal           DECIMAL(12,2)  NOT NULL,
    created_at              DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_item_order_product (order_id, product_id),
    KEY idx_order_item_order_id (order_id),
    KEY idx_order_item_product_id (product_id),
    CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_order_item_product
        FOREIGN KEY (product_id) REFERENCES product (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT chk_order_item_quantity_min CHECK (quantity >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_addresses (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    order_id        BIGINT         NOT NULL,
    full_name       VARCHAR(150)   NOT NULL,
    mobile          VARCHAR(20)    NOT NULL,
    email           VARCHAR(255)   NULL,
    line1           VARCHAR(255)   NOT NULL,
    line2           VARCHAR(255)   NULL,
    landmark        VARCHAR(255)   NULL,
    city            VARCHAR(100)   NOT NULL,
    state           VARCHAR(100)   NOT NULL,
    postal_code     VARCHAR(20)    NOT NULL,
    country         VARCHAR(100)   NOT NULL DEFAULT 'India',
    created_at      DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_addresses_order_id (order_id),
    CONSTRAINT fk_order_address_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

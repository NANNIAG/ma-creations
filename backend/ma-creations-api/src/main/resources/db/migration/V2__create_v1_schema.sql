-- V2: Confirmed V1 business schema
-- Source: docs/DATABASE_ER_DESIGN.md + docs/DATABASE_REVIEW.md (section A — safe to implement)
-- Excludes proposed/unconfirmed columns: tile_image_url, focus_notes, alt_text, display_name,
-- created_by_admin_id, UPI/Pay Later, featured/bestseller, inventory, variants, reviews, subcategory.

CREATE TABLE category (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(120) NOT NULL,
    display_order   INT          NOT NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_name (name),
    UNIQUE KEY uk_category_slug (slug),
    KEY idx_category_display_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE admin_user (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    login_identifier  VARCHAR(255) NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    enabled           BIT(1)       NOT NULL DEFAULT b'1',
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_user_login_identifier (login_identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    title           VARCHAR(255)   NOT NULL,
    category_id     BIGINT         NOT NULL,
    selling_price   DECIMAL(10, 2) NOT NULL,
    mrp             DECIMAL(10, 2) NOT NULL,
    slug            VARCHAR(255)   NOT NULL,
    average_rating  DECIMAL(3, 2)  NULL,
    rating_count    INT            NULL,
    created_at      DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_slug (slug),
    KEY idx_product_category_id (category_id),
    KEY idx_product_created_at (created_at),
    KEY idx_product_selling_price (selling_price),
    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id) REFERENCES category (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_image (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    product_id    BIGINT       NOT NULL,
    storage_path  VARCHAR(512) NOT NULL,
    sort_order    INT          NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_product_image_product_id (product_id),
    KEY idx_product_image_product_sort (product_id, sort_order),
    CONSTRAINT fk_product_image_product
        FOREIGN KEY (product_id) REFERENCES product (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

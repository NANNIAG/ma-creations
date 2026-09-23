-- V10: Payment transaction foundation for Razorpay (and future providers).
-- Does not store card numbers, CVV, or gateway secrets.
-- COD does not require a provider payment row with Razorpay IDs (provider may be NONE).

CREATE TABLE payment_transaction (
    id                      BIGINT         NOT NULL AUTO_INCREMENT,
    order_id                BIGINT         NOT NULL,
    provider                VARCHAR(32)    NOT NULL,
    provider_order_id       VARCHAR(128)   NULL,
    provider_payment_id     VARCHAR(128)   NULL,
    payment_method          VARCHAR(32)    NOT NULL,
    amount                  DECIMAL(12,2)  NOT NULL,
    currency                CHAR(3)        NOT NULL DEFAULT 'INR',
    status                  VARCHAR(32)    NOT NULL,
    idempotency_key         VARCHAR(64)    NOT NULL,
    provider_reference      VARCHAR(512)   NULL,
    last_webhook_event_id   VARCHAR(128)   NULL,
    failure_code            VARCHAR(64)    NULL,
    created_at              DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_tx_idempotency (idempotency_key),
    UNIQUE KEY uk_payment_tx_webhook_event (last_webhook_event_id),
    KEY idx_payment_tx_order_id (order_id),
    KEY idx_payment_tx_provider_order (provider, provider_order_id),
    KEY idx_payment_tx_provider_payment (provider, provider_payment_id),
    KEY idx_payment_tx_status (status),
    CONSTRAINT fk_payment_tx_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_payment_tx_amount_nonneg CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

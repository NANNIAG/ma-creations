-- V7: Customer accounts (mobile OTP auth). No password. No plaintext OTP on customer.

CREATE TABLE customer (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    mobile_number   VARCHAR(20)  NOT NULL,
    first_name      VARCHAR(100) NULL,
    last_name       VARCHAR(100) NULL,
    email           VARCHAR(255) NULL,
    enabled         BIT(1)       NOT NULL DEFAULT b'1',
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_mobile_number (mobile_number),
    KEY idx_customer_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Short-lived OTP challenges (hashed code only). Not stored on customer.
CREATE TABLE customer_otp_challenge (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    mobile_number   VARCHAR(20)  NOT NULL,
    otp_hash        VARCHAR(255) NOT NULL,
    expires_at      DATETIME(6)  NOT NULL,
    attempt_count   INT          NOT NULL DEFAULT 0,
    max_attempts    INT          NOT NULL DEFAULT 5,
    consumed_at     DATETIME(6)  NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_customer_otp_mobile (mobile_number),
    KEY idx_customer_otp_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

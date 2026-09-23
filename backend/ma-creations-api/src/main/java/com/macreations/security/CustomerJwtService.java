package com.macreations.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Customer-only JWT (separate secret from admin). Claims include role CUSTOMER and customerId.
 */
@Service
public class CustomerJwtService {

    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_CUSTOMER_ID = "customerId";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    private final SecretKey secretKey;
    private final long expirationMs;

    public CustomerJwtService(
            @Value("${app.security.customer.jwt.secret}") String secret,
            @Value("${app.security.customer.jwt.expiration-ms:86400000}") long expirationMs) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "app.security.customer.jwt.secret must be set and at least 32 characters (use CUSTOMER_JWT_SECRET)");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long customerId, String mobileNumber) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(mobileNumber)
                .claim(CLAIM_ROLE, ROLE_CUSTOMER)
                .claim(CLAIM_CUSTOMER_ID, customerId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isCustomerToken(Claims claims) {
        return ROLE_CUSTOMER.equals(claims.get(CLAIM_ROLE, String.class));
    }

    public Long getCustomerId(Claims claims) {
        Object raw = claims.get(CLAIM_CUSTOMER_ID);
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        return null;
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}

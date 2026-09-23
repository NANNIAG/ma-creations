package com.macreations.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.macreations.repository.OrderRepository;

/**
 * Generates customer-facing order numbers: {@code MAC-yyyyMMdd-<12 base32 chars>}.
 * Uniqueness enforced by DB unique constraint with retry on rare collision.
 */
@Service
public class OrderNumberGenerator {

    private static final String PREFIX = "MAC-";
    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final int RANDOM_LEN = 12;
    private static final int MAX_ATTEMPTS = 8;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;

    public OrderNumberGenerator(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public String nextOrderNumber() {
        String day = LocalDate.now(ZoneOffset.UTC).format(DAY);
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String candidate = PREFIX + day + "-" + randomSegment();
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to allocate a unique order number");
    }

    private static String randomSegment() {
        char[] buf = new char[RANDOM_LEN];
        for (int i = 0; i < RANDOM_LEN; i++) {
            buf[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(buf);
    }
}

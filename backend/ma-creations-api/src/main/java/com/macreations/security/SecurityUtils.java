package com.macreations.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Resolves the authenticated storefront customer from the security context.
 * Never trust client-supplied customer IDs.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<Long> currentCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomerUserDetails customerUserDetails) {
            return Optional.ofNullable(customerUserDetails.getCustomerId());
        }
        return Optional.empty();
    }
}

package com.macreations.security;

import java.io.IOException;
import java.util.Date;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.macreations.repository.CustomerRepository;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authenticates customer JWTs only (separate secret from admin).
 */
public class CustomerJwtAuthenticationFilter extends OncePerRequestFilter {

    private final CustomerJwtService customerJwtService;
    private final CustomerRepository customerRepository;

    public CustomerJwtAuthenticationFilter(
            CustomerJwtService customerJwtService,
            CustomerRepository customerRepository) {
        this.customerJwtService = customerJwtService;
        this.customerRepository = customerRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = customerJwtService.parseClaims(token);
            if (!customerJwtService.isCustomerToken(claims)) {
                filterChain.doFilter(request, response);
                return;
            }
            if (claims.getExpiration() == null || !claims.getExpiration().after(new Date())) {
                filterChain.doFilter(request, response);
                return;
            }
            Long customerId = customerJwtService.getCustomerId(claims);
            if (customerId == null) {
                filterChain.doFilter(request, response);
                return;
            }
            customerRepository.findById(customerId).ifPresent(customer -> {
                if (!customer.isEnabled()) {
                    return;
                }
                CustomerUserDetails principal = new CustomerUserDetails(customer);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        } catch (Exception ignored) {
            // Not a customer token (e.g. admin JWT with different secret) — leave context empty
        }

        filterChain.doFilter(request, response);
    }
}

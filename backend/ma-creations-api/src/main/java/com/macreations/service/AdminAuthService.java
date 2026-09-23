package com.macreations.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.macreations.dto.LoginRequest;
import com.macreations.dto.LoginResponse;
import com.macreations.exception.ApiException;
import com.macreations.security.AdminUserDetails;
import com.macreations.security.JwtService;

import org.springframework.http.HttpStatus;

@Service
public class AdminAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AdminAuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().trim().toLowerCase(),
                            request.getPassword()));

            AdminUserDetails principal = (AdminUserDetails) authentication.getPrincipal();
            String token = jwtService.generateToken(principal.getUsername());
            return new LoginResponse(
                    token,
                    jwtService.getExpirationMs() / 1000,
                    principal.getUsername());
        } catch (AuthenticationException ex) {
            throw new ApiException("INVALID_CREDENTIALS", "Invalid email or password", HttpStatus.UNAUTHORIZED);
        }
    }
}

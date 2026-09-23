package com.macreations.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.macreations.dto.ApiResponse;
import com.macreations.dto.LoginRequest;
import com.macreations.dto.LoginResponse;
import com.macreations.security.AdminUserDetails;
import com.macreations.service.AdminAuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/auth")
@Validated
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminAuthService.login(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse>> me(@AuthenticationPrincipal AdminUserDetails principal) {
        LoginResponse body = new LoginResponse(null, null, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(body));
    }
}

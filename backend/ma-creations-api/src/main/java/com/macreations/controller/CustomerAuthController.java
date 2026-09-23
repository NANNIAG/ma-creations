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
import com.macreations.dto.CustomerAuthResponse;
import com.macreations.dto.CustomerResponse;
import com.macreations.dto.RequestOtpRequest;
import com.macreations.dto.RequestOtpResponse;
import com.macreations.dto.VerifyOtpRequest;
import com.macreations.security.CustomerUserDetails;
import com.macreations.service.CustomerAuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customer/auth")
@Validated
public class CustomerAuthController {

    private final CustomerAuthService customerAuthService;

    public CustomerAuthController(CustomerAuthService customerAuthService) {
        this.customerAuthService = customerAuthService;
    }

    @PostMapping("/request-otp")
    public ResponseEntity<ApiResponse<RequestOtpResponse>> requestOtp(
            @Valid @RequestBody RequestOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(customerAuthService.requestOtp(request.getMobileNumber())));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<CustomerAuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                customerAuthService.verifyOtp(request.getMobileNumber(), request.getOtp())));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CustomerResponse>> me(
            @AuthenticationPrincipal CustomerUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(
                customerAuthService.getCurrentCustomer(principal.getCustomerId())));
    }

    /**
     * Stateless JWT logout — client discards the token. Endpoint exists for API symmetry.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

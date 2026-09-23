package com.macreations.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.macreations.dto.ApiResponse;
import com.macreations.dto.InitiatePaymentRequest;
import com.macreations.dto.InitiatePaymentResponse;
import com.macreations.dto.ReportPaymentFailureRequest;
import com.macreations.dto.VerifyPaymentRequest;
import com.macreations.dto.VerifyPaymentResponse;
import com.macreations.service.payment.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    public static final String RAZORPAY_SIGNATURE_HEADER = "X-Razorpay-Signature";

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<InitiatePaymentResponse>> initiate(
            @Valid @RequestBody InitiatePaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.initiate(request)));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<VerifyPaymentResponse>> verify(
            @Valid @RequestBody VerifyPaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.verify(request)));
    }

    @PostMapping("/fail")
    public ResponseEntity<ApiResponse<VerifyPaymentResponse>> reportFailure(
            @Valid @RequestBody ReportPaymentFailureRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.reportFailure(request.getOrderNumber())));
    }

    @PostMapping("/webhook/razorpay")
    public ResponseEntity<ApiResponse<Void>> razorpayWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = RAZORPAY_SIGNATURE_HEADER, required = false) String signature) {
        paymentService.handleRazorpayWebhook(rawBody, signature);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

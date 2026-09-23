package com.macreations.service.payment;

import java.math.BigDecimal;
import java.util.Map;

import com.macreations.entity.PaymentMethod;
import com.macreations.entity.PaymentProvider;

/**
 * Provider-agnostic payment gateway port. Implementations must not leak SDK types.
 */
public interface PaymentGateway {

    PaymentProvider provider();

    CreatePaymentResult createPayment(CreatePaymentCommand command);

    boolean verifyCheckoutSignature(VerifyPaymentCommand command);

    boolean verifyWebhookSignature(String rawBody, String signatureHeader);

    ParsedWebhookEvent parseWebhookEvent(String rawBody);

    /**
     * Future order-management use. Not exposed via public refund APIs in Step 23.
     */
    RefundResult refund(RefundCommand command);

    record CreatePaymentCommand(
            String receipt,
            BigDecimal amountInr,
            String currency,
            PaymentMethod paymentMethod,
            Map<String, String> notes) {
    }

    record CreatePaymentResult(
            String providerOrderId,
            long amountPaise,
            String currency,
            String providerReference) {
    }

    record VerifyPaymentCommand(
            String providerOrderId,
            String providerPaymentId,
            String signature) {
    }

    record ParsedWebhookEvent(
            String eventId,
            String eventType,
            String providerOrderId,
            String providerPaymentId,
            String paymentStatus,
            String methodHint) {
    }

    record RefundCommand(
            String providerPaymentId,
            BigDecimal amountInr,
            String currency,
            String notes) {
    }

    record RefundResult(
            String providerRefundId,
            String status) {
    }
}

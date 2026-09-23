package com.macreations.service.payment.razorpay;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.macreations.entity.PaymentProvider;
import com.macreations.service.payment.PaymentGateway;
import com.macreations.service.payment.PaymentGatewayException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import com.razorpay.Utils;

/**
 * Razorpay adapter. Domain layer must not depend on Razorpay types outside this package.
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "RAZORPAY", matchIfMissing = true)
public class RazorpayPaymentGateway implements PaymentGateway {

    private final RazorpayProperties properties;
    private final RazorpayClientFactory clientFactory;

    public RazorpayPaymentGateway(RazorpayProperties properties, RazorpayClientFactory clientFactory) {
        this.properties = properties;
        this.clientFactory = clientFactory;
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.RAZORPAY;
    }

    @Override
    public CreatePaymentResult createPayment(CreatePaymentCommand command) {
        requireConfigured();
        long amountPaise = toPaise(command.amountInr());
        String currency = command.currency() == null ? "INR" : command.currency();
        if (!"INR".equalsIgnoreCase(currency)) {
            throw new PaymentGatewayException("UNSUPPORTED_CURRENCY", "Only INR is supported");
        }

        try {
            RazorpayClient client = clientFactory.getClient();
            JSONObject request = new JSONObject();
            request.put("amount", amountPaise);
            request.put("currency", "INR");
            request.put("receipt", truncate(command.receipt(), 40));
            if (command.notes() != null && !command.notes().isEmpty()) {
                JSONObject notes = new JSONObject();
                for (Map.Entry<String, String> entry : command.notes().entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        notes.put(entry.getKey(), entry.getValue());
                    }
                }
                request.put("notes", notes);
            }

            Order order = client.orders.create(request);
            String providerOrderId = order.get("id");
            return new CreatePaymentResult(
                    providerOrderId,
                    amountPaise,
                    "INR",
                    providerOrderId);
        } catch (RazorpayException ex) {
            throw new PaymentGatewayException("RAZORPAY_ORDER_CREATE_FAILED", "Unable to create Razorpay order", ex);
        }
    }

    @Override
    public boolean verifyCheckoutSignature(VerifyPaymentCommand command) {
        requireConfigured();
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", command.providerOrderId());
            attributes.put("razorpay_payment_id", command.providerPaymentId());
            attributes.put("razorpay_signature", command.signature());
            return Utils.verifyPaymentSignature(attributes, properties.getKeySecret());
        } catch (RazorpayException ex) {
            throw new PaymentGatewayException("RAZORPAY_SIGNATURE_INVALID", "Payment signature verification failed", ex);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        requireWebhookConfigured();
        if (rawBody == null || signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        try {
            return Utils.verifyWebhookSignature(rawBody, signatureHeader, properties.getWebhookSecret());
        } catch (RazorpayException ex) {
            return false;
        }
    }

    @Override
    public ParsedWebhookEvent parseWebhookEvent(String rawBody) {
        try {
            JSONObject root = new JSONObject(rawBody);
            String eventId = root.optString("id", null);
            String eventType = root.optString("event", null);
            JSONObject payload = root.optJSONObject("payload");
            String providerPaymentId = null;
            String providerOrderId = null;
            String paymentStatus = null;
            String methodHint = null;

            if (payload != null) {
                JSONObject paymentWrapper = payload.optJSONObject("payment");
                if (paymentWrapper != null) {
                    JSONObject payment = paymentWrapper.optJSONObject("entity");
                    if (payment != null) {
                        providerPaymentId = payment.optString("id", null);
                        providerOrderId = payment.optString("order_id", null);
                        paymentStatus = payment.optString("status", null);
                        methodHint = payment.optString("method", null);
                    }
                }
                if (providerOrderId == null) {
                    JSONObject orderWrapper = payload.optJSONObject("order");
                    if (orderWrapper != null) {
                        JSONObject order = orderWrapper.optJSONObject("entity");
                        if (order != null) {
                            providerOrderId = order.optString("id", null);
                        }
                    }
                }
            }

            return new ParsedWebhookEvent(
                    eventId,
                    eventType,
                    providerOrderId,
                    providerPaymentId,
                    paymentStatus,
                    methodHint);
        } catch (Exception ex) {
            throw new PaymentGatewayException("RAZORPAY_WEBHOOK_PARSE_FAILED", "Unable to parse Razorpay webhook", ex);
        }
    }

    @Override
    public RefundResult refund(RefundCommand command) {
        requireConfigured();
        try {
            RazorpayClient client = clientFactory.getClient();
            JSONObject request = new JSONObject();
            if (command.amountInr() != null) {
                request.put("amount", toPaise(command.amountInr()));
            }
            if (command.notes() != null) {
                request.put("notes", new JSONObject().put("reason", command.notes()));
            }
            Refund refund = client.payments.refund(command.providerPaymentId(), request);
            return new RefundResult(refund.get("id"), refund.get("status"));
        } catch (RazorpayException ex) {
            throw new PaymentGatewayException("RAZORPAY_REFUND_FAILED", "Unable to create Razorpay refund", ex);
        }
    }

    public static long toPaise(BigDecimal amountInr) {
        if (amountInr == null) {
            throw new PaymentGatewayException("AMOUNT_REQUIRED", "Amount is required");
        }
        return amountInr
                .setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }

    private void requireConfigured() {
        if (!properties.hasApiCredentials()) {
            throw new PaymentGatewayException(
                    "RAZORPAY_NOT_CONFIGURED",
                    "Razorpay key id/secret are not configured");
        }
    }

    private void requireWebhookConfigured() {
        if (!properties.hasWebhookSecret()) {
            throw new PaymentGatewayException(
                    "RAZORPAY_WEBHOOK_NOT_CONFIGURED",
                    "Razorpay webhook secret is not configured");
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}

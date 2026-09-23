package com.macreations.service.payment.razorpay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.macreations.entity.PaymentMethod;
import com.macreations.service.payment.PaymentGateway;
import com.macreations.service.payment.PaymentGatewayException;

@ExtendWith(MockitoExtension.class)
class RazorpayPaymentGatewayTest {

    @Mock
    private RazorpayClientFactory clientFactory;

    private RazorpayProperties properties;
    private RazorpayPaymentGateway gateway;

    @BeforeEach
    void setUp() {
        properties = new RazorpayProperties();
        properties.setKeyId("rzp_test_1");
        properties.setKeySecret("test_secret_key_value_123456");
        properties.setWebhookSecret("whsec_test_1234567890");
        gateway = new RazorpayPaymentGateway(properties, clientFactory);
    }

    @Test
    void toPaiseConvertsInrToSmallestUnit() {
        assertThat(RazorpayPaymentGateway.toPaise(new BigDecimal("567.00"))).isEqualTo(56700L);
        assertThat(RazorpayPaymentGateway.toPaise(new BigDecimal("10.50"))).isEqualTo(1050L);
    }

    @Test
    void createPaymentRequiresCredentials() {
        properties.setKeySecret("");
        assertThatThrownBy(() -> gateway.createPayment(new PaymentGateway.CreatePaymentCommand(
                "MAC-1", new BigDecimal("10.00"), "INR", PaymentMethod.UPI, Map.of())))
                .isInstanceOf(PaymentGatewayException.class)
                .extracting("code")
                .isEqualTo("RAZORPAY_NOT_CONFIGURED");
    }

    @Test
    void verifyWebhookSignatureAcceptsValidHmac() throws Exception {
        String body = "{\"event\":\"payment.captured\",\"id\":\"evt_1\"}";
        String signature = hmacHex(body, properties.getWebhookSecret());
        assertThat(gateway.verifyWebhookSignature(body, signature)).isTrue();
    }

    @Test
    void verifyWebhookSignatureRejectsInvalid() {
        assertThat(gateway.verifyWebhookSignature("{\"event\":\"payment.captured\"}", "deadbeef")).isFalse();
    }

    @Test
    void parseWebhookEventExtractsPaymentFields() {
        String body = """
                {
                  "id": "evt_99",
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_abc",
                        "order_id": "order_xyz",
                        "status": "captured",
                        "method": "upi"
                      }
                    }
                  }
                }
                """;
        PaymentGateway.ParsedWebhookEvent event = gateway.parseWebhookEvent(body);
        assertThat(event.eventId()).isEqualTo("evt_99");
        assertThat(event.eventType()).isEqualTo("payment.captured");
        assertThat(event.providerPaymentId()).isEqualTo("pay_abc");
        assertThat(event.providerOrderId()).isEqualTo("order_xyz");
        assertThat(event.paymentStatus()).isEqualTo("captured");
        assertThat(event.methodHint()).isEqualTo("upi");
    }

    private static String hmacHex(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

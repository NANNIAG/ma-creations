package com.macreations.service.payment.razorpay;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "app.payment.razorpay")
public class RazorpayProperties {

    private String keyId = "";
    private String keySecret = "";
    private String webhookSecret = "";
    /**
     * When false, PAY_LATER initiation returns PAY_LATER_UNAVAILABLE.
     * Enable only after Razorpay merchant account has Pay Later / BNPL configured.
     * Do not hard-code a BNPL provider name here.
     */
    private boolean payLaterEnabled = false;

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getKeySecret() {
        return keySecret;
    }

    public void setKeySecret(String keySecret) {
        this.keySecret = keySecret;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public boolean isPayLaterEnabled() {
        return payLaterEnabled;
    }

    public void setPayLaterEnabled(boolean payLaterEnabled) {
        this.payLaterEnabled = payLaterEnabled;
    }

    public boolean hasApiCredentials() {
        return StringUtils.hasText(keyId) && StringUtils.hasText(keySecret);
    }

    public boolean hasWebhookSecret() {
        return StringUtils.hasText(webhookSecret);
    }
}

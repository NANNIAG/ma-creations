package com.macreations.service.payment.razorpay;

import org.springframework.stereotype.Component;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.macreations.service.payment.PaymentGatewayException;

@Component
public class RazorpayClientFactory {

    private final RazorpayProperties properties;

    public RazorpayClientFactory(RazorpayProperties properties) {
        this.properties = properties;
    }

    public RazorpayClient getClient() {
        try {
            return new RazorpayClient(properties.getKeyId(), properties.getKeySecret());
        } catch (RazorpayException ex) {
            throw new PaymentGatewayException("RAZORPAY_CLIENT_INIT_FAILED", "Unable to initialize Razorpay client", ex);
        }
    }
}

package com.macreations.service.otp;

/**
 * Pluggable OTP delivery/verification. Real SMS providers implement this later.
 */
public interface OtpService {

    /**
     * Generate and "send" an OTP for the normalized mobile number.
     * Implementations must not expose the OTP via this return value in production.
     */
    void sendOtp(String normalizedMobile);

    /**
     * Verify OTP for the mobile. Consumes the challenge on success.
     *
     * @throws com.macreations.exception.ApiException on invalid/expired/exhausted OTP
     */
    void verifyOtp(String normalizedMobile, String otp);
}

package com.macreations.util;

import com.macreations.exception.BadRequestException;

/**
 * Normalizes Indian mobile numbers for customer OTP auth.
 * Accepts 10-digit numbers (6–9…) or 91XXXXXXXXXX.
 */
public final class MobileNumberUtils {

    private MobileNumberUtils() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("INVALID_MOBILE", "Mobile number is required");
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.startsWith("91") && digits.length() == 12) {
            digits = digits.substring(2);
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        if (!digits.matches("[6-9]\\d{9}")) {
            throw new BadRequestException(
                    "INVALID_MOBILE",
                    "Enter a valid 10-digit Indian mobile number");
        }
        return digits;
    }
}

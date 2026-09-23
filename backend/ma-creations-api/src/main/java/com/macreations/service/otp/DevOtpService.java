package com.macreations.service.otp;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.macreations.entity.CustomerOtpChallenge;
import com.macreations.exception.ApiException;
import com.macreations.repository.CustomerOtpChallengeRepository;

/**
 * Development OTP service: hashes OTP, enforces expiry/attempts/one-time use.
 * Does not call any SMS provider. When {@code app.security.customer.otp.dev-fixed-otp}
 * is set (local/tests), that code is used instead of a random one.
 * When {@code app.security.customer.otp.dev-log-otp=true}, logs OTP at INFO for local testing only.
 */
@Service
public class DevOtpService implements OtpService {

    private static final Logger log = LoggerFactory.getLogger(DevOtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CustomerOtpChallengeRepository challengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final long expiryMinutes;
    private final int maxAttempts;
    private final String fixedOtp;
    private final boolean logOtp;

    public DevOtpService(
            CustomerOtpChallengeRepository challengeRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.customer.otp.expiry-minutes:5}") long expiryMinutes,
            @Value("${app.security.customer.otp.max-attempts:5}") int maxAttempts,
            @Value("${app.security.customer.otp.dev-fixed-otp:}") String fixedOtp,
            @Value("${app.security.customer.otp.dev-log-otp:false}") boolean logOtp) {
        this.challengeRepository = challengeRepository;
        this.passwordEncoder = passwordEncoder;
        this.expiryMinutes = expiryMinutes;
        this.maxAttempts = maxAttempts;
        this.fixedOtp = fixedOtp == null ? "" : fixedOtp.trim();
        this.logOtp = logOtp;
    }

    @Override
    @Transactional
    public void sendOtp(String normalizedMobile) {
        String otp = resolveOtpCode();
        CustomerOtpChallenge challenge = new CustomerOtpChallenge();
        challenge.setMobileNumber(normalizedMobile);
        challenge.setOtpHash(passwordEncoder.encode(otp));
        challenge.setExpiresAt(Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES));
        challenge.setMaxAttempts(maxAttempts);
        challenge.setAttemptCount(0);
        challengeRepository.save(challenge);

        if (logOtp) {
            log.info("DEV OTP for mobile ending …{}: {} (not for production)",
                    normalizedMobile.substring(Math.max(0, normalizedMobile.length() - 4)),
                    otp);
        }
    }

    @Override
    @Transactional
    public void verifyOtp(String normalizedMobile, String otp) {
        if (otp == null || otp.isBlank()) {
            throw new ApiException("INVALID_OTP", "OTP is required", HttpStatus.BAD_REQUEST);
        }
        String trimmed = otp.trim();

        CustomerOtpChallenge challenge = challengeRepository
                .findFirstByMobileNumberAndConsumedAtIsNullOrderByCreatedAtDesc(normalizedMobile)
                .orElseThrow(() -> new ApiException(
                        "INVALID_OTP",
                        "Invalid or expired OTP",
                        HttpStatus.UNAUTHORIZED));

        Instant now = Instant.now();
        if (challenge.isExpired(now)) {
            throw new ApiException("OTP_EXPIRED", "OTP has expired. Request a new one.", HttpStatus.UNAUTHORIZED);
        }
        if (challenge.getAttemptCount() >= challenge.getMaxAttempts()) {
            throw new ApiException(
                    "OTP_ATTEMPTS_EXCEEDED",
                    "Too many invalid attempts. Request a new OTP.",
                    HttpStatus.UNAUTHORIZED);
        }

        if (!passwordEncoder.matches(trimmed, challenge.getOtpHash())) {
            challenge.setAttemptCount(challenge.getAttemptCount() + 1);
            challengeRepository.save(challenge);
            throw new ApiException("INVALID_OTP", "Invalid or expired OTP", HttpStatus.UNAUTHORIZED);
        }

        challenge.setConsumedAt(now);
        challengeRepository.save(challenge);
    }

    private String resolveOtpCode() {
        if (!fixedOtp.isEmpty()) {
            return fixedOtp;
        }
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }
}

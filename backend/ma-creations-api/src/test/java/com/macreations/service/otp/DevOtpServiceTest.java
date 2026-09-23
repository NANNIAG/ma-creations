package com.macreations.service.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.macreations.entity.CustomerOtpChallenge;
import com.macreations.exception.ApiException;
import com.macreations.repository.CustomerOtpChallengeRepository;

@ExtendWith(MockitoExtension.class)
class DevOtpServiceTest {

    @Mock
    private CustomerOtpChallengeRepository challengeRepository;

    private PasswordEncoder passwordEncoder;
    private DevOtpService otpService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        otpService = new DevOtpService(
                challengeRepository, passwordEncoder, 5, 5, "123456", false);
    }

    @Test
    void sendOtpPersistsHashedChallenge() {
        when(challengeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        otpService.sendOtp("9876543210");

        ArgumentCaptor<CustomerOtpChallenge> captor = ArgumentCaptor.forClass(CustomerOtpChallenge.class);
        verify(challengeRepository).save(captor.capture());
        CustomerOtpChallenge challenge = captor.getValue();
        assertThat(challenge.getMobileNumber()).isEqualTo("9876543210");
        assertThat(challenge.getOtpHash()).isNotEqualTo("123456");
        assertThat(passwordEncoder.matches("123456", challenge.getOtpHash())).isTrue();
        assertThat(challenge.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void verifyOtpSucceedsAndConsumes() {
        CustomerOtpChallenge challenge = activeChallenge("9876543210", "123456");
        when(challengeRepository.findFirstByMobileNumberAndConsumedAtIsNullOrderByCreatedAtDesc("9876543210"))
                .thenReturn(Optional.of(challenge));
        when(challengeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        otpService.verifyOtp("9876543210", "123456");

        assertThat(challenge.getConsumedAt()).isNotNull();
    }

    @Test
    void verifyOtpRejectsWrongCodeAndIncrementsAttempts() {
        CustomerOtpChallenge challenge = activeChallenge("9876543210", "123456");
        when(challengeRepository.findFirstByMobileNumberAndConsumedAtIsNullOrderByCreatedAtDesc("9876543210"))
                .thenReturn(Optional.of(challenge));
        when(challengeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> otpService.verifyOtp("9876543210", "000000"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("INVALID_OTP");
        assertThat(challenge.getAttemptCount()).isEqualTo(1);
        assertThat(challenge.getConsumedAt()).isNull();
    }

    @Test
    void verifyOtpRejectsExpired() {
        CustomerOtpChallenge challenge = activeChallenge("9876543210", "123456");
        challenge.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(challengeRepository.findFirstByMobileNumberAndConsumedAtIsNullOrderByCreatedAtDesc("9876543210"))
                .thenReturn(Optional.of(challenge));

        assertThatThrownBy(() -> otpService.verifyOtp("9876543210", "123456"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("OTP_EXPIRED");
    }

    @Test
    void verifyOtpRejectsReusedAfterConsume() {
        when(challengeRepository.findFirstByMobileNumberAndConsumedAtIsNullOrderByCreatedAtDesc("9876543210"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.verifyOtp("9876543210", "123456"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("INVALID_OTP");
    }

    @Test
    void verifyOtpRejectsWhenAttemptsExceeded() {
        CustomerOtpChallenge challenge = activeChallenge("9876543210", "123456");
        challenge.setAttemptCount(5);
        when(challengeRepository.findFirstByMobileNumberAndConsumedAtIsNullOrderByCreatedAtDesc("9876543210"))
                .thenReturn(Optional.of(challenge));

        assertThatThrownBy(() -> otpService.verifyOtp("9876543210", "123456"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("OTP_ATTEMPTS_EXCEEDED");
    }

    private CustomerOtpChallenge activeChallenge(String mobile, String otp) {
        CustomerOtpChallenge challenge = new CustomerOtpChallenge();
        challenge.setId(1L);
        challenge.setMobileNumber(mobile);
        challenge.setOtpHash(passwordEncoder.encode(otp));
        challenge.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        challenge.setMaxAttempts(5);
        challenge.setAttemptCount(0);
        return challenge;
    }
}

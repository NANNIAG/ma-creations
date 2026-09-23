package com.macreations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.macreations.dto.CustomerAuthResponse;
import com.macreations.dto.RequestOtpResponse;
import com.macreations.entity.Customer;
import com.macreations.exception.ApiException;
import com.macreations.exception.BadRequestException;
import com.macreations.repository.CustomerRepository;
import com.macreations.security.CustomerJwtService;
import com.macreations.service.otp.OtpService;

@ExtendWith(MockitoExtension.class)
class CustomerAuthServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OtpService otpService;
    @Mock
    private CustomerJwtService customerJwtService;

    private CustomerAuthService customerAuthService;

    @BeforeEach
    void setUp() {
        customerAuthService = new CustomerAuthService(
                customerRepository, otpService, customerJwtService, 5);
    }

    @Test
    void requestOtpNormalizesMobileAndDoesNotRevealExistence() {
        RequestOtpResponse response = customerAuthService.requestOtp("9876543210");

        verify(otpService).sendOtp("9876543210");
        assertThat(response.getMessage()).containsIgnoringCase("OTP");
        assertThat(response.getRetryAfterSeconds()).isEqualTo(300);
    }

    @Test
    void requestOtpAcceptsCountryCodePrefix() {
        customerAuthService.requestOtp("919876543210");
        verify(otpService).sendOtp("9876543210");
    }

    @Test
    void requestOtpRejectsInvalidMobile() {
        assertThatThrownBy(() -> customerAuthService.requestOtp("12345"))
                .isInstanceOf(BadRequestException.class)
                .extracting("code")
                .isEqualTo("INVALID_MOBILE");
        verify(otpService, never()).sendOtp(any());
    }

    @Test
    void verifyOtpCreatesCustomerOnFirstLogin() {
        when(customerRepository.findByMobileNumber("9876543210")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(42L);
            return c;
        });
        when(customerJwtService.generateToken(42L, "9876543210")).thenReturn("customer.jwt.token");
        when(customerJwtService.getExpirationMs()).thenReturn(86_400_000L);

        CustomerAuthResponse response = customerAuthService.verifyOtp("9876543210", "123456");

        verify(otpService).verifyOtp("9876543210", "123456");
        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getMobileNumber()).isEqualTo("9876543210");
        assertThat(captor.getValue().isEnabled()).isTrue();
        assertThat(response.getAccessToken()).isEqualTo("customer.jwt.token");
        assertThat(response.getCustomer().getId()).isEqualTo(42L);
        assertThat(response.getCustomer().getMobileNumber()).isEqualTo("9876543210");
    }

    @Test
    void verifyOtpLogsInExistingCustomer() {
        Customer existing = new Customer();
        existing.setId(7L);
        existing.setMobileNumber("9876543210");
        existing.setEnabled(true);
        when(customerRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(existing));
        when(customerJwtService.generateToken(7L, "9876543210")).thenReturn("tok");
        when(customerJwtService.getExpirationMs()).thenReturn(1000L);

        CustomerAuthResponse response = customerAuthService.verifyOtp("9876543210", "999999");

        verify(customerRepository, never()).save(any());
        assertThat(response.getAccessToken()).isEqualTo("tok");
        assertThat(response.getCustomer().getId()).isEqualTo(7L);
    }

    @Test
    void verifyOtpPropagatesInvalidOtp() {
        doThrow(new ApiException("INVALID_OTP", "Invalid or expired OTP",
                org.springframework.http.HttpStatus.UNAUTHORIZED))
                .when(otpService).verifyOtp(eq("9876543210"), eq("000000"));

        assertThatThrownBy(() -> customerAuthService.verifyOtp("9876543210", "000000"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("INVALID_OTP");
        verify(customerRepository, never()).save(any());
    }

    @Test
    void verifyOtpRejectsDisabledAccount() {
        Customer existing = new Customer();
        existing.setId(7L);
        existing.setMobileNumber("9876543210");
        existing.setEnabled(false);
        when(customerRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> customerAuthService.verifyOtp("9876543210", "123456"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("ACCOUNT_DISABLED");
    }
}

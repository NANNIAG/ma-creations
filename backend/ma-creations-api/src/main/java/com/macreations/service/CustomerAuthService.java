package com.macreations.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.macreations.dto.CustomerAuthResponse;
import com.macreations.dto.CustomerResponse;
import com.macreations.dto.RequestOtpResponse;
import com.macreations.entity.Customer;
import com.macreations.exception.ApiException;
import com.macreations.repository.CustomerRepository;
import com.macreations.security.CustomerJwtService;
import com.macreations.service.otp.OtpService;
import com.macreations.util.MobileNumberUtils;

@Service
public class CustomerAuthService {

    private final CustomerRepository customerRepository;
    private final OtpService otpService;
    private final CustomerJwtService customerJwtService;
    private final int otpExpiryMinutes;

    public CustomerAuthService(
            CustomerRepository customerRepository,
            OtpService otpService,
            CustomerJwtService customerJwtService,
            @Value("${app.security.customer.otp.expiry-minutes:5}") int otpExpiryMinutes) {
        this.customerRepository = customerRepository;
        this.otpService = otpService;
        this.customerJwtService = customerJwtService;
        this.otpExpiryMinutes = otpExpiryMinutes;
    }

    @Transactional
    public RequestOtpResponse requestOtp(String mobileNumberRaw) {
        String mobile = MobileNumberUtils.normalize(mobileNumberRaw);
        // Do not reveal whether the mobile is already registered
        otpService.sendOtp(mobile);
        return new RequestOtpResponse(
                "If this number can receive SMS, an OTP has been sent.",
                otpExpiryMinutes * 60);
    }

    @Transactional
    public CustomerAuthResponse verifyOtp(String mobileNumberRaw, String otp) {
        String mobile = MobileNumberUtils.normalize(mobileNumberRaw);
        otpService.verifyOtp(mobile, otp);

        Customer customer = customerRepository.findByMobileNumber(mobile)
                .orElseGet(() -> createCustomer(mobile));

        if (!customer.isEnabled()) {
            throw new ApiException("ACCOUNT_DISABLED", "This account is disabled", HttpStatus.FORBIDDEN);
        }

        String token = customerJwtService.generateToken(customer.getId(), customer.getMobileNumber());
        return new CustomerAuthResponse(
                token,
                customerJwtService.getExpirationMs() / 1000,
                toResponse(customer));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCurrentCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ApiException(
                        "UNAUTHORIZED",
                        "Customer not found",
                        HttpStatus.UNAUTHORIZED));
        if (!customer.isEnabled()) {
            throw new ApiException("ACCOUNT_DISABLED", "This account is disabled", HttpStatus.FORBIDDEN);
        }
        return toResponse(customer);
    }

    private Customer createCustomer(String mobile) {
        Customer customer = new Customer();
        customer.setMobileNumber(mobile);
        customer.setEnabled(true);
        return customerRepository.save(customer);
    }

    private static CustomerResponse toResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setMobileNumber(customer.getMobileNumber());
        response.setFirstName(customer.getFirstName());
        response.setLastName(customer.getLastName());
        response.setEmail(customer.getEmail());
        return response;
    }
}

package com.macreations.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.macreations.dto.CustomerAuthResponse;
import com.macreations.dto.CustomerResponse;
import com.macreations.dto.RequestOtpResponse;
import com.macreations.exception.GlobalExceptionHandler;
import com.macreations.security.AdminUserDetailsService;
import com.macreations.security.JwtService;
import com.macreations.service.CustomerAuthService;

@WebMvcTest(controllers = CustomerAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CustomerAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerAuthService customerAuthService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AdminUserDetailsService adminUserDetailsService;

    @Test
    void requestOtpReturnsGenericMessage() throws Exception {
        when(customerAuthService.requestOtp(any()))
                .thenReturn(new RequestOtpResponse("If this number can receive SMS, an OTP has been sent.", 300));

        mockMvc.perform(post("/api/customer/auth/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobileNumber\":\"9876543210\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").exists())
                .andExpect(jsonPath("$.data.devOtp").doesNotExist());
    }

    @Test
    void verifyOtpReturnsCustomerJwt() throws Exception {
        CustomerResponse customer = new CustomerResponse();
        customer.setId(1L);
        customer.setMobileNumber("9876543210");
        when(customerAuthService.verifyOtp(eq("9876543210"), eq("123456")))
                .thenReturn(new CustomerAuthResponse("cust.jwt", 3600L, customer));

        mockMvc.perform(post("/api/customer/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobileNumber\":\"9876543210\",\"otp\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("cust.jwt"))
                .andExpect(jsonPath("$.data.customer.mobileNumber").value("9876543210"));
    }
}

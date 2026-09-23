package com.macreations.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.macreations.dto.LoginResponse;
import com.macreations.exception.ApiException;
import com.macreations.exception.GlobalExceptionHandler;
import com.macreations.security.AdminUserDetailsService;
import com.macreations.security.JsonAuthenticationEntryPoint;
import com.macreations.security.JwtService;
import com.macreations.service.AdminAuthService;

@WebMvcTest(controllers = AdminAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAuthService adminAuthService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AdminUserDetailsService adminUserDetailsService;

    @MockitoBean
    private JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;

    @Test
    void loginSuccess() throws Exception {
        when(adminAuthService.login(any()))
                .thenReturn(new LoginResponse("token", 3600L, "admin@macreations.test"));

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@macreations.test","password":"secret"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("token"))
                .andExpect(jsonPath("$.data.email").value("admin@macreations.test"));
    }

    @Test
    void loginInvalidCredentials() throws Exception {
        when(adminAuthService.login(any()))
                .thenThrow(new ApiException("INVALID_CREDENTIALS", "Invalid email or password", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@macreations.test","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginValidationFailure() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}

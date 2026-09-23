package com.macreations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.macreations.dto.LoginRequest;
import com.macreations.dto.LoginResponse;
import com.macreations.entity.AdminUser;
import com.macreations.exception.ApiException;
import com.macreations.security.AdminUserDetails;
import com.macreations.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private AdminAuthService adminAuthService;

    @Test
    void loginSuccessReturnsToken() {
        AdminUser admin = new AdminUser();
        admin.setLoginIdentifier("admin@macreations.test");
        admin.setPasswordHash("hash");
        admin.setEnabled(true);
        AdminUserDetails details = new AdminUserDetails(admin);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken("admin@macreations.test")).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(86_400_000L);

        LoginRequest request = new LoginRequest();
        request.setEmail("admin@macreations.test");
        request.setPassword("secret");

        LoginResponse response = adminAuthService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("admin@macreations.test");
        assertThat(response.getExpiresInSeconds()).isEqualTo(86_400L);
    }

    @Test
    void loginInvalidPasswordThrowsUnauthorized() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        LoginRequest request = new LoginRequest();
        request.setEmail("admin@macreations.test");
        request.setPassword("wrong");

        assertThatThrownBy(() -> adminAuthService.login(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getCode()).isEqualTo("INVALID_CREDENTIALS");
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }
}

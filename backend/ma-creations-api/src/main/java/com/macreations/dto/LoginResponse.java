package com.macreations.dto;

public class LoginResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private Long expiresInSeconds;
    private String email;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken, Long expiresInSeconds, String email) {
        this.accessToken = accessToken;
        this.expiresInSeconds = expiresInSeconds;
        this.email = email;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(Long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

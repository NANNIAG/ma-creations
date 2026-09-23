package com.macreations.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private T data;
    private ApiError error;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.data = data;
        return response;
    }

    public static ApiResponse<Void> error(String code, String message) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.error = new ApiError(code, message);
        return response;
    }

    public T getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }

    public static class ApiError {
        private final String code;
        private final String message;

        public ApiError(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}

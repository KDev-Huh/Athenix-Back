package com.community.athenixback.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ErrorDetail error;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .error(null)
            .build();
    }

    public static <T> ApiResponse<T> failure(String code, String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .data(null)
            .error(ErrorDetail.builder()
                .code(code)
                .message(message)
                .build())
            .build();
    }
}

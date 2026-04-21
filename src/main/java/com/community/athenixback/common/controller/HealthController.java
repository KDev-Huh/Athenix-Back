package com.community.athenixback.common.controller;

import com.community.athenixback.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<HealthResponse>> health() {
        return ResponseEntity.ok(ApiResponse.success(HealthResponse.builder()
            .status("UP")
            .build()));
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class HealthResponse {
        private String status;
    }
}

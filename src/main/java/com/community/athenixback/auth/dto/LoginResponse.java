package com.community.athenixback.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    @JsonProperty("accessToken")
    private String accessToken;

    @JsonProperty("refreshToken")
    private String refreshToken;

    @JsonProperty("session")
    private SessionInfo session;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SessionInfo {
        private Long userId;
        private String name;
        private String email;
        private LocalDateTime loggedInAt;
    }
}

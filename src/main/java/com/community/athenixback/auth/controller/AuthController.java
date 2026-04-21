package com.community.athenixback.auth.controller;

import com.community.athenixback.auth.dto.*;
import com.community.athenixback.auth.service.AuthService;
import com.community.athenixback.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@RequestBody SignupRequest request) {
        log.info("회원가입 요청: {}", request.getEmail());
        UserResponse user = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(SignupResponse.builder()
                .user(user)
                .build()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        log.info("로그인 요청: {}", request.getEmail());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            log.info("로그아웃 요청: {}", userDetails.getUsername());
            authService.logout(userDetails.getUsername());
        }
        return ResponseEntity.ok(ApiResponse.success(LogoutResponse.builder()
            .message("로그아웃 되었습니다.")
            .build()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(@RequestHeader("Authorization") String authHeader) {
        log.info("토큰 갱신 요청");

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure("INVALID_TOKEN", "유효하지 않은 토큰입니다."));
        }

        String token = authHeader.substring(7);
        TokenRefreshResponse response = authService.refreshToken(token);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

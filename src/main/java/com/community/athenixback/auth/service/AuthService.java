package com.community.athenixback.auth.service;

import com.community.athenixback.auth.dto.*;
import com.community.athenixback.auth.entity.RefreshToken;
import com.community.athenixback.auth.entity.User;
import com.community.athenixback.auth.repository.RefreshTokenRepository;
import com.community.athenixback.auth.repository.UserRepository;
import com.community.athenixback.auth.security.JwtTokenProvider;
import com.community.athenixback.common.exception.AuthenticationException;
import com.community.athenixback.common.exception.DuplicateResourceException;
import com.community.athenixback.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse signup(SignupRequest request) {
        // 이메일 중복 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("DUPLICATE_EMAIL", "이미 존재하는 이메일입니다.");
        }

        // 사용자 생성
        User user = User.builder()
            .name(request.getName())
            .position(request.getPosition())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .build();

        userRepository.save(user);

        return UserResponse.builder()
            .id(user.getId())
            .name(user.getName())
            .position(user.getPosition())
            .email(user.getEmail())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new AuthenticationException("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        // Refresh Token 저장
        RefreshToken refreshTokenEntity = RefreshToken.builder()
            .user(user)
            .token(refreshToken)
            .expiresAt(LocalDateTime.now().plusDays(7))
            .build();
        refreshTokenRepository.save(refreshTokenEntity);

        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .session(LoginResponse.SessionInfo.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .loggedInAt(LocalDateTime.now())
                .build())
            .build();
    }

    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

        // 해당 사용자의 모든 Refresh Token 삭제
        refreshTokenRepository.deleteByUser(user);
    }

    @Transactional
    public TokenRefreshResponse refreshToken(String token) {
        // Refresh Token 검증
        if (!jwtTokenProvider.validateToken(token)) {
            throw new AuthenticationException("INVALID_TOKEN", "유효하지 않은 토큰입니다.");
        }

        // Refresh Token 조회
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new AuthenticationException("INVALID_TOKEN", "유효하지 않은 토큰입니다."));

        // 토큰 만료 확인
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new AuthenticationException("EXPIRED_TOKEN", "토큰이 만료되었습니다.");
        }

        // 사용자 조회
        User user = refreshToken.getUser();

        // 새 토큰 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        // 기존 Refresh Token 삭제 후 새로운 것 저장 (회전 정책)
        refreshTokenRepository.delete(refreshToken);
        RefreshToken newRefreshTokenEntity = RefreshToken.builder()
            .user(user)
            .token(newRefreshToken)
            .expiresAt(LocalDateTime.now().plusDays(7))
            .build();
        refreshTokenRepository.save(newRefreshTokenEntity);

        return TokenRefreshResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .expiresIn(jwtTokenProvider.getExpirationTime() / 1000)
            .build();
    }
}

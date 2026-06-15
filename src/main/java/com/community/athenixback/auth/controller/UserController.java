package com.community.athenixback.auth.controller;

import com.community.athenixback.auth.dto.UserSummaryResponse;
import com.community.athenixback.auth.entity.User;
import com.community.athenixback.auth.repository.UserRepository;
import com.community.athenixback.common.exception.AuthenticationException;
import com.community.athenixback.common.exception.ResourceNotFoundException;
import com.community.athenixback.common.response.ApiResponse;
import com.community.athenixback.match.repository.MatchRepository;
import com.community.athenixback.match.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private static final String AI_FEEDBACK_LABEL = "AI 피드백";

    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final MemoRepository memoRepository;

    @GetMapping("/me/summary")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getMySummary(@AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);

        UserSummaryResponse response = UserSummaryResponse.builder()
            .totalMatches(Math.toIntExact(matchRepository.countByUser(user)))
            .totalMemos(Math.toIntExact(memoRepository.countByMatchUser(user)))
            .aiAnalysisCount(Math.toIntExact(memoRepository.countByMatchUserAndLabel(user, AI_FEEDBACK_LABEL)))
            .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private User resolveUser(UserDetails principal) {
        if (principal == null) {
            throw new AuthenticationException("UNAUTHORIZED", "인증이 필요합니다.");
        }

        if (principal instanceof User user) {
            return user;
        }

        return userRepository.findByEmail(principal.getUsername())
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
    }

}

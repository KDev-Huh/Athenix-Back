package com.community.athenixback.match.controller;

import com.community.athenixback.common.response.ApiResponse;
import com.community.athenixback.match.dto.AIFeedbackRequest;
import com.community.athenixback.match.dto.AIFeedbackResponse;
import com.community.athenixback.match.dto.MemoResponse;
import com.community.athenixback.match.entity.AIFeedback;
import com.community.athenixback.match.entity.Match;
import com.community.athenixback.match.service.AIFeedbackService;
import com.community.athenixback.match.service.MatchService;
import com.community.athenixback.match.service.MemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/matches/{matchId}/ai-feedback")
@RequiredArgsConstructor
public class AIFeedbackController {

    private final AIFeedbackService aiFeedbackService;
    private final MatchService matchService;
    private final MemoService memoService;

    /**
     * 7.1 AI 피드백 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AIFeedbackResponse>> createFeedback(
        @PathVariable("matchId") Long matchId,
        @RequestBody AIFeedbackRequest request) {

        log.info("AI 피드백 생성 요청: matchId={}, timeMs={}, isRtl={}", matchId, request.getTimeMs(), request.getIsRtl());

        // 경기 존재 여부 확인 - Optional 반환 메서드 사용
        Match match = matchService.getMatchById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("경기를 찾을 수 없습니다."));

        // AI 서버에서 피드백 생성
        AIFeedbackResponse response = aiFeedbackService.createFeedback(match, request.getTimeMs(), request.getIsRtl());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response));
    }

    /**
     * 7.2 AI 피드백 이력 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getFeedbacksByMatch(
        @PathVariable("matchId") Long matchId,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "20") int size,
        @RequestParam(value = "sort", defaultValue = "latest") String sort) {

        log.info("AI 피드백 이력 조회: matchId={}, page={}, size={}, sort={}", matchId, page, size, sort);

        // 경기 존재 여부 확인 - Optional 반환 메서드 사용
        matchService.getMatchById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("경기를 찾을 수 없습니다."));

        // 정렬 방향 설정
        int pageIndex = Math.max(0, page - 1);
        Sort.Direction direction = "oldest".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by(direction, "createdAt"));

        Page<AIFeedbackResponse> feedbacks = aiFeedbackService.getFeedbacksByMatch(matchId, pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("items", feedbacks.getContent());
        data.put("page", page);
        data.put("size", size);
        data.put("total", feedbacks.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 7.3 AI 피드백을 메모로 추가
     */
    @PostMapping("/memo")
    public ResponseEntity<ApiResponse<MemoResponse>> addFeedbackAsMemo(
        @PathVariable("matchId") Long matchId,
        @RequestBody Map<String, Object> request) {

        Long feedbackId = ((Number) request.get("feedbackId")).longValue();
        String label = (String) request.getOrDefault("label", "AI 피드백");

        log.info("AI 피드백을 메모로 추가: matchId={}, feedbackId={}", matchId, feedbackId);

        // 경기 존재 여부 확인 - Optional 반환 메서드 사용
        Match match = matchService.getMatchById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("경기를 찾을 수 없습니다."));

        // 피드백 존재 여부 확인
        AIFeedback feedback = aiFeedbackService.getFeedbackById(feedbackId)
            .orElseThrow(() -> new IllegalArgumentException("피드백을 찾을 수 없습니다."));

        // 피드백을 메모로 추가
        MemoResponse memoResponse = memoService.addFeedbackAsMemo(match, feedback, label);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(memoResponse));
    }
}

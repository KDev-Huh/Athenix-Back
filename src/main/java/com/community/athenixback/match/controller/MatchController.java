package com.community.athenixback.match.controller;

import com.community.athenixback.auth.entity.User;
import com.community.athenixback.common.response.ApiResponse;
import com.community.athenixback.match.dto.MatchResponse;
import com.community.athenixback.match.dto.MatchUploadRequest;
import com.community.athenixback.match.dto.MemoCreateRequest;
import com.community.athenixback.match.dto.MemoResponse;
import com.community.athenixback.match.dto.StatusUpdateRequest;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;
    private final MemoService memoService;

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MatchResponse>> uploadMatch(
        @RequestParam(value = "file", required = false) MultipartFile file,
        @ModelAttribute MatchUploadRequest request) throws IOException {

        log.info("경기 업로드 요청: {}", request.getTitle());
        User user = getCurrentUser();
        MatchResponse response = matchService.uploadMatch(user, file, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getMatches(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "sort", defaultValue = "latest") String sort,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "20") int size) {

        log.info("경기 목록 조회: status={}, sort={}, page={}, size={}", status, sort, page, size);
        User user = getCurrentUser();

        // page는 1부터 시작하므로 0 기반 인덱스로 변환
        int pageIndex = page - 1;
        if (pageIndex < 0) {
            pageIndex = 0;
        }

        Sort.Direction direction = "oldest".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by(direction, "createdAt"));

        Page<MatchResponse> matches = matchService.getMatches(user, status, pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("items", matches.getContent());
        data.put("page", page);
        data.put("size", size);
        data.put("total", matches.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<Object>> getRecentMatches(
        @RequestParam(value = "limit", defaultValue = "3") int limit) {

        log.info("최근 경기 조회: limit={}", limit);
        User user = getCurrentUser();
        List<MatchResponse> matches = matchService.getRecentMatches(user, limit);

        Map<String, Object> data = new HashMap<>();
        data.put("items", matches);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<ApiResponse<MatchResponse>> getMatch(@PathVariable Long matchId) {
        log.info("경기 단건 조회: matchId={}", matchId);
        User user = getCurrentUser();
        MatchResponse response = matchService.getMatchById(matchId, user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{matchId}/status")
    public ResponseEntity<ApiResponse<MatchResponse>> updateMatchStatus(
        @PathVariable Long matchId,
        @RequestBody StatusUpdateRequest request) {

        log.info("경기 상태 변경: matchId={}, status={}", matchId, request.getStatus());
        User user = getCurrentUser();
        MatchResponse response = matchService.updateMatchStatus(matchId, user, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{matchId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteMatch(@PathVariable Long matchId) {
        log.info("경기 삭제: matchId={}", matchId);
        User user = getCurrentUser();
        matchService.deleteMatch(matchId, user);

        Map<String, String> data = new HashMap<>();
        data.put("message", "경기가 삭제되었습니다.");
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // ============ 경기별 메모 API ============

    @PostMapping("/{matchId}/memos")
    public ResponseEntity<ApiResponse<MemoResponse>> createMemo(
        @PathVariable Long matchId,
        @RequestBody MemoCreateRequest request) {

        log.info("메모 생성: matchId={}", matchId);
        User user = getCurrentUser();
        MemoResponse response = memoService.createMemo(matchId, user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{matchId}/memos")
    public ResponseEntity<ApiResponse<Object>> getMemosByMatch(
        @PathVariable Long matchId,
        @RequestParam(value = "sort", defaultValue = "latest") String sort) {

        log.info("경기별 메모 조회: matchId={}", matchId);
        User user = getCurrentUser();
        List<MemoResponse> memos = memoService.getMemosByMatch(matchId, user, sort);

        Map<String, Object> data = new HashMap<>();
        data.put("matchId", matchId);
        data.put("items", memos);

        return ResponseEntity.ok(ApiResponse.success(data));
    }
}

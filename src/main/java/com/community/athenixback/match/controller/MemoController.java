package com.community.athenixback.match.controller;

import com.community.athenixback.auth.entity.User;
import com.community.athenixback.common.response.ApiResponse;
import com.community.athenixback.match.dto.MemoResponse;
import com.community.athenixback.match.dto.MemoUpdateRequest;
import com.community.athenixback.match.service.MemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/memos")
@RequiredArgsConstructor
public class MemoController {

    private final MemoService memoService;

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getAllMemos(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "20") int size,
        @RequestParam(value = "sort", defaultValue = "latest") String sort) {

        log.info("전체 메모 조회: page={}, size={}", page, size);

        // page는 1부터 시작하므로 0 기반 인덱스로 변환
        int pageIndex = page - 1;
        if (pageIndex < 0) {
            pageIndex = 0;
        }

        Sort.Direction direction = "oldest".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by(direction, "createdAt"));

        Page<MemoResponse> memos = memoService.getAllMemos(pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("items", memos.getContent());
        data.put("page", page);
        data.put("size", size);
        data.put("total", memos.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success(data));

    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<Object>> getRecentMemos(
        @RequestParam(value = "limit", defaultValue = "2") int limit) {

        log.info("최근 메모 조회: limit={}", limit);
        List<MemoResponse> memos = memoService.getRecentMemos(limit);

        Map<String, Object> data = new HashMap<>();
        data.put("items", memos);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PatchMapping("/{memoId}")
    public ResponseEntity<ApiResponse<MemoResponse>> updateMemo(
        @PathVariable Long memoId,
        @RequestBody MemoUpdateRequest request) {

        log.info("메모 수정: memoId={}", memoId);
        User user = getCurrentUser();
        MemoResponse response = memoService.updateMemo(memoId, user, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{memoId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteMemo(@PathVariable Long memoId) {
        log.info("메모 삭제: memoId={}", memoId);
        User user = getCurrentUser();
        memoService.deleteMemo(memoId, user);

        Map<String, String> data = new HashMap<>();
        data.put("message", "메모가 삭제되었습니다.");
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}

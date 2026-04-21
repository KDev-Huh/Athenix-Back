package com.community.athenixback.match.controller;

import com.community.athenixback.auth.entity.User;
import com.community.athenixback.common.exception.ResourceNotFoundException;
import com.community.athenixback.common.response.ApiResponse;
import com.community.athenixback.match.dto.VideoMetaResponse;
import com.community.athenixback.match.entity.Match;
import com.community.athenixback.match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
@RestController
@RequestMapping("/matches/{matchId}/video")
@RequiredArgsConstructor
public class VideoController {

    private final MatchRepository matchRepository;

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
    }

    @GetMapping
    public ResponseEntity<Resource> streamVideo(@PathVariable Long matchId,
                                                @RequestHeader(value = "Range", required = false) String range) {
        log.info("영상 스트리밍 요청: matchId={}", matchId);
        User user = getCurrentUser();

        Match match = matchRepository.findByIdAndUser(matchId, user)
            .orElseThrow(() -> new ResourceNotFoundException("MATCH_NOT_FOUND", "경기를 찾을 수 없습니다."));

        if (match.getVideoFilePath() == null || match.getVideoFilePath().isEmpty()) {
            throw new ResourceNotFoundException("VIDEO_NOT_FOUND", "영상을 찾을 수 없습니다.");
        }

        File videoFile = new File(match.getVideoFilePath());
        if (!videoFile.exists()) {
            throw new ResourceNotFoundException("VIDEO_NOT_FOUND", "영상 파일이 없습니다.");
        }

        try {
            long fileSize = videoFile.length();
            Resource resource = new FileSystemResource(videoFile);

            if (range != null && range.startsWith("bytes=")) {
                // Range 요청 처리
                String[] rangeParts = range.substring(6).split("-");
                long start = Long.parseLong(rangeParts[0]);
                long end = rangeParts.length > 1 && !rangeParts[1].isEmpty()
                    ? Long.parseLong(rangeParts[1])
                    : fileSize - 1;

                long contentLength = end - start + 1;

                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);
            } else {
                // 전체 파일 요청
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);
            }
        } catch (Exception e) {
            log.error("영상 스트리밍 오류", e);
            throw new IllegalArgumentException("영상 스트리밍 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/meta")
    public ResponseEntity<ApiResponse<VideoMetaResponse>> getVideoMeta(@PathVariable Long matchId) {
        log.info("영상 메타 조회: matchId={}", matchId);
        User user = getCurrentUser();

        Match match = matchRepository.findByIdAndUser(matchId, user)
            .orElseThrow(() -> new ResourceNotFoundException("MATCH_NOT_FOUND", "경기를 찾을 수 없습니다."));

        if (match.getVideoFilePath() == null || match.getVideoFilePath().isEmpty()) {
            throw new ResourceNotFoundException("VIDEO_NOT_FOUND", "영상을 찾을 수 없습니다.");
        }

        File videoFile = new File(match.getVideoFilePath());
        if (!videoFile.exists()) {
            throw new ResourceNotFoundException("VIDEO_NOT_FOUND", "영상 파일이 없습니다.");
        }

        try {
            VideoMetaResponse response = VideoMetaResponse.builder()
                .durationSec(match.getVideoDurationSec() != null ? match.getVideoDurationSec() : 0L)
                .codec("h264")
                .sizeBytes(match.getVideoFileSize() != null ? match.getVideoFileSize() : 0L)
                .thumbnailUrl(null)
                .build();

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("영상 메타 조회 오류", e);
            throw new IllegalArgumentException("영상 메타 조회 중 오류가 발생했습니다.");
        }
    }
}

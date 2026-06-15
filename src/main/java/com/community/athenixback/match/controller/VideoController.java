package com.community.athenixback.match.controller;

import com.community.athenixback.auth.entity.User;
import com.community.athenixback.common.exception.ResourceNotFoundException;
import com.community.athenixback.common.response.ApiResponse;
import com.community.athenixback.match.dto.VideoMetaResponse;
import com.community.athenixback.match.entity.Match;
import com.community.athenixback.match.repository.MatchRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

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
    public void streamVideo(
            @PathVariable("matchId") Long matchId,
            @RequestHeader(value = "Range", required = false) String range,
            HttpServletResponse response) throws IOException {

        log.info("영상 스트리밍 요청: matchId={}, range={}", matchId, range);

        try {
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

            long fileSize = videoFile.length();
            log.info("영상 스트리밍 승인됨: matchId={}, userId={}, fileSize={}", matchId, user.getId(), fileSize);

            response.setContentType("video/mp4");
            response.setHeader("Accept-Ranges", "bytes");

            if (range != null && range.startsWith("bytes=")) {
                serveRange(range, fileSize, videoFile, response);
            } else {
                serveFullFile(fileSize, videoFile, response);
            }

        } catch (IllegalArgumentException e) {
            log.warn("인증되지 않은 요청");
            if (!response.isCommitted()) response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증이 필요합니다.");
        } catch (ResourceNotFoundException e) {
            log.warn("영상 조회 실패: {}", e.getMessage());
            if (!response.isCommitted()) response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (IOException e) {
            log.debug("영상 스트리밍 클라이언트 연결 끊김: {}", e.getMessage());
        } catch (Exception e) {
            log.error("영상 스트리밍 오류", e);
            if (!response.isCommitted()) response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "영상 스트리밍 중 오류가 발생했습니다.");
        }
    }

    private void serveRange(String range, long fileSize, File videoFile, HttpServletResponse response) throws IOException {
        try {
            String[] rangeParts = range.substring(6).split("-");
            long start = Long.parseLong(rangeParts[0]);
            long end = rangeParts.length > 1 && !rangeParts[1].isEmpty()
                ? Long.parseLong(rangeParts[1])
                : fileSize - 1;

            if (start > end || start < 0 || end >= fileSize) {
                log.warn("잘못된 Range: {}", range);
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + fileSize);
                return;
            }

            long contentLength = end - start + 1;
            log.debug("Range 요청 처리: {}-{}/{}", start, end, fileSize);

            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", String.format("bytes %d-%d/%d", start, end, fileSize));
            response.setHeader("Content-Length", String.valueOf(contentLength));

            try (RandomAccessFile raf = new RandomAccessFile(videoFile, "r");
                 OutputStream out = response.getOutputStream()) {
                raf.seek(start);
                byte[] buf = new byte[8192];
                long remaining = contentLength;
                int read;
                while (remaining > 0 && (read = raf.read(buf, 0, (int) Math.min(buf.length, remaining))) != -1) {
                    out.write(buf, 0, read);
                    remaining -= read;
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Range 파싱 실패: {}", range);
            if (!response.isCommitted()) response.sendError(HttpServletResponse.SC_BAD_REQUEST, "잘못된 Range 헤더입니다.");
        }
    }

    private void serveFullFile(long fileSize, File videoFile, HttpServletResponse response) throws IOException {
        log.debug("전체 파일 요청 처리: {} bytes", fileSize);
        response.setHeader("Content-Length", String.valueOf(fileSize));

        try (RandomAccessFile raf = new RandomAccessFile(videoFile, "r");
             OutputStream out = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = raf.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        }
    }

    @GetMapping("/meta")
    public ResponseEntity<ApiResponse<VideoMetaResponse>> getVideoMeta(@PathVariable("matchId") Long matchId) {
        log.info("영상 메타 조회: matchId={}", matchId);

        try {
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

            VideoMetaResponse metaResponse = VideoMetaResponse.builder()
                .durationSec(match.getVideoDurationSec() != null ? match.getVideoDurationSec() : 0L)
                .sizeBytes(videoFile.length())
                .build();

            return ResponseEntity.ok(ApiResponse.success(metaResponse));

        } catch (ResourceNotFoundException ex) {
            log.warn("영상 메타 조회 실패: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getCode(), ex.getMessage()));
        }
    }
}
package com.community.athenixback.common.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
@RestController
@RequestMapping("/thumbnails")
@RequiredArgsConstructor
public class ThumbnailController {

    @Value("${app.storage.path:/tmp/athenix-uploads}")
    private String storagePath;

    /**
     * 썸네일 이미지 서빙
     * GET /api/v1/thumbnails/{filename}
     */
    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable String filename) {
        try {
            // 파일 경로 생성
            File thumbnailFile = new File(storagePath, "thumbnails/" + filename);

            // 파일 존재 여부 확인
            if (!thumbnailFile.exists()) {
                log.warn("썸네일 파일 없음: {}", thumbnailFile.getAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            // 보안: 디렉토리 트래버설 공격 방지
            if (!thumbnailFile.getCanonicalPath().startsWith(new File(storagePath, "thumbnails").getCanonicalPath())) {
                log.warn("보안: 허용되지 않은 경로 접근 시도: {}", thumbnailFile.getAbsolutePath());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 파일 읽기
            byte[] imageBytes = Files.readAllBytes(thumbnailFile.toPath());

            // 응답 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentLength(imageBytes.length);
            headers.setCacheControl("public, max-age=3600"); // 1시간 캐시

            log.info("썸네일 제공: {}", filename);
            return ResponseEntity.ok()
                .headers(headers)
                .body(imageBytes);

        } catch (IOException e) {
            log.error("썸네일 조회 중 오류: filename={}, {}", filename, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

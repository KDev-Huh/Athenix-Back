package com.community.athenixback.match.service;

import com.community.athenixback.common.util.VideoThumbnailUtil;
import com.community.athenixback.match.entity.Match;
import com.community.athenixback.match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoProcessingService {

    private final MatchRepository matchRepository;
    private final VideoThumbnailUtil videoThumbnailUtil;

    @Async
    public void processVideo(Long matchId, String originalFilePath) {
        log.info("영상 처리 시작 (백그라운드): matchId={}", matchId);

        Match match = matchRepository.findById(matchId).orElse(null);
        if (match == null) {
            log.error("영상 처리 실패: matchId={} 를 찾을 수 없음", matchId);
            return;
        }

        try {
            // H.264 트랜스코딩
            String transcodedPath = videoThumbnailUtil.transcodeToH264(originalFilePath);
            match.setVideoFilePath(transcodedPath);

            // 썸네일 생성
            try {
                String thumbnailUrl = videoThumbnailUtil.generateThumbnail(transcodedPath, 0);
                match.setThumbnailUrl(thumbnailUrl);
                log.info("썸네일 생성 완료: matchId={}", matchId);
            } catch (Exception e) {
                log.warn("썸네일 생성 실패 (무시): matchId={}, error={}", matchId, e.getMessage());
            }

            match.setStatus("임시 저장");
            matchRepository.save(match);
            log.info("영상 처리 완료: matchId={}", matchId);

        } catch (Exception e) {
            log.error("영상 처리 중 오류: matchId={}", matchId, e);
            match.setStatus("처리 실패");
            matchRepository.save(match);
        }
    }
}
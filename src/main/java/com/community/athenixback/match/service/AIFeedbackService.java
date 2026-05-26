package com.community.athenixback.match.service;

import com.community.athenixback.common.util.TimeUtil;
import com.community.athenixback.common.util.VideoFrameExtractor;
import com.community.athenixback.match.client.AIServerClient;
import com.community.athenixback.match.dto.AIFeedbackResponse;
import com.community.athenixback.match.dto.AIServerResponse;
import com.community.athenixback.match.dto.PlayGuideDto;
import com.community.athenixback.match.entity.AIFeedback;
import com.community.athenixback.match.entity.Match;
import com.community.athenixback.match.repository.AIFeedbackRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AIFeedbackService {

    private final AIFeedbackRepository aiFeedbackRepository;
    private final AIServerClient aiServerClient;
    private final VideoFrameExtractor videoFrameExtractor;
    private final ObjectMapper objectMapper;

    /**
     * 7.1 AI 피드백 생성
     * 1. 영상의 특정 시간에서 스크린샷 추출
     * 2. AI 서버에 이미지 전송
     * 3. 응답받아 DB에 저장
     * 4. feedbackId, timeMs, timeLabel, createdAt 포함해서 반환
     */
    public AIFeedbackResponse createFeedback(Match match, Long timeMs, Boolean isRtl) {
        try {
            // 1. 영상에서 해당 시간의 프레임 추출
            log.info("프레임 추출 시작: matchId={}, timeMs={}, isRtl={}", match.getId(), timeMs, isRtl);
            byte[] imageBytes = videoFrameExtractor.extractFrameAtTime(
                match.getVideoFilePath(),
                timeMs
            );

            // 2. AI 서버에 이미지 전송하여 플레이 추천 받기
            String fileName = "frame_" + match.getId() + "_" + timeMs + ".jpg";
            AIServerResponse aiResponse = aiServerClient.requestPlayRecommendation(imageBytes, fileName, isRtl);

            if (aiResponse == null) {
                throw new RuntimeException("AI 서버 응답이 null입니다");
            }

            // situation 필드 최종 검증
            String situation = aiResponse.getSituation();
            if (situation == null || situation.trim().isEmpty()) {
                log.warn("AI 서버 응답의 situation이 비어있습니다. 기본값 사용: matchId={}, timeMs={}",
                    match.getId(), timeMs);
                situation = "분석 결과를 획득할 수 없습니다.";
            }

            // 3. playGuideJson으로 저장하기 위해 JSON 문자열로 변환
            String playGuideJson = objectMapper.writeValueAsString(aiResponse.getPlayGuide());

            // 4. AI 피드백 엔티티 생성 및 저장
            AIFeedback feedback = AIFeedback.builder()
                .match(match)
                .timeMs(timeMs)
                .situation(situation)
                .playGuideJson(playGuideJson)
                .build();

            AIFeedback savedFeedback = aiFeedbackRepository.save(feedback);
            log.info("AI 피드백 저장 완료: feedbackId={}", savedFeedback.getId());

            // 5. 응답 객체 생성
            return toResponse(savedFeedback, aiResponse);

        } catch (RuntimeException e) {
            log.error("피드백 생성 실패", e);
            throw e;
        } catch (Exception e) {
            log.error("피드백 생성 실패", e);
            throw new RuntimeException("피드백 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 7.2 AI 피드백 이력 조회
     */
    @Transactional(readOnly = true)
    public Page<AIFeedbackResponse> getFeedbacksByMatch(Long matchId, Pageable pageable) {
        Page<AIFeedback> feedbacks = aiFeedbackRepository.findByMatchId(matchId, pageable);

        List<AIFeedbackResponse> responses = feedbacks.getContent().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, feedbacks.getTotalElements());
    }

    /**
     * 피드백 단건 조회
     */
    @Transactional(readOnly = true)
    public Optional<AIFeedback> getFeedbackById(Long feedbackId) {
        return aiFeedbackRepository.findById(feedbackId);
    }

    /**
     * 경기별 모든 피드백 삭제 (cascade 삭제용)
     */
    public void deleteByMatchId(Long matchId) {
        Page<AIFeedback> feedbacks = aiFeedbackRepository.findByMatchId(matchId, Pageable.unpaged());
        aiFeedbackRepository.deleteAll(feedbacks);
    }

    /**
     * AIFeedback Entity를 AIFeedbackResponse DTO로 변환
     * AI 서버 응답은 필요 없음 (이미 저장된 데이터 조회)
     */
    private AIFeedbackResponse toResponse(AIFeedback feedback) {
        try {
            PlayGuideDto playGuide = objectMapper.readValue(feedback.getPlayGuideJson(), PlayGuideDto.class);

            return AIFeedbackResponse.builder()
                .feedbackId(feedback.getId())
                .matchId(feedback.getMatch().getId())
                .timeMs(feedback.getTimeMs())
                .timeLabel(TimeUtil.msToTimeLabel(feedback.getTimeMs()))
                .situation(feedback.getSituation())
                .playGuide(playGuide)
                .createdAt(feedback.getCreatedAt())
                .build();
        } catch (Exception e) {
            log.error("피드백 변환 실패", e);
            throw new RuntimeException("피드백 변환 실패: " + e.getMessage(), e);
        }
    }

    /**
     * AIFeedback Entity + AIServerResponse를 AIFeedbackResponse DTO로 변환
     * 새로 생성된 피드백을 반환할 때 사용
     */
    private AIFeedbackResponse toResponse(AIFeedback feedback, AIServerResponse aiResponse) {
        return AIFeedbackResponse.builder()
            .feedbackId(feedback.getId())
            .matchId(feedback.getMatch().getId())
            .timeMs(feedback.getTimeMs())
            .timeLabel(TimeUtil.msToTimeLabel(feedback.getTimeMs()))
            .situation(aiResponse.getSituation())
            .playGuide(aiResponse.getPlayGuide())
            .createdAt(feedback.getCreatedAt())
            .build();
    }
}

package com.community.athenixback.match.client;

import com.community.athenixback.match.dto.AIServerResponse;
import com.community.athenixback.match.dto.AIServerRawResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIServerClient {

    private final RestTemplate restTemplate;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    @Value("${ai.server.endpoint.recommend}")
    private String recommendEndpoint;

    /**
     * 이미지를 AI 서버에 전송하여 플레이 추천을 받음
     * AI 서버 명세: POST /ai/recommend/image
     *
     * @param imageBytes 분석할 경기 장면 이미지 바이트 (jpg, png, webp)
     * @param fileName 이미지 파일명
     * @return AIServerResponse (situation, playGuide)
     */
    public AIServerResponse requestPlayRecommendation(byte[] imageBytes, String fileName, Boolean isRtl) {
        try {
            String baseUrl = aiServerUrl + recommendEndpoint;
            String url = (isRtl != null) ? baseUrl + "?is_rtl=" + isRtl : baseUrl;

            // multipart/form-data 요청 생성
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // ByteArrayResource로 이미지 데이터 전송
            ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };

            body.add("image", imageResource);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            log.info("AI 서버에 이미지 분석 요청: {}", url);

            // AI 서버는 { "success": true, "data": {...}, "error": null } 구조로 응답
            AIServerRawResponse rawResponse = restTemplate.postForObject(url, entity, AIServerRawResponse.class);

            if (rawResponse == null || !Boolean.TRUE.equals(rawResponse.getSuccess())) {
                log.error("AI 서버 응답 실패 또는 null");
                throw new RuntimeException("AI 서버 응답이 실패했습니다.");
            }

            AIServerResponse response = rawResponse.getData();
            if (response == null) {
                log.error("AI 서버 응답의 data 필드가 null");
                throw new RuntimeException("AI 서버 응답의 data 필드가 null입니다.");
            }

            // situation이 null인 경우 기본값 설정
            if (response.getSituation() == null || response.getSituation().trim().isEmpty()) {
                log.warn("AI 서버 응답의 situation이 비어있습니다. 기본값 설정");
                response.setSituation("분석 결과를 획득할 수 없습니다.");
            }

            log.info("AI 서버 응답 수신: situation={}, playGuide.type={}",
                response.getSituation(),
                response.getPlayGuide() != null ? response.getPlayGuide().getType() : "null");

            return response;
        } catch (Exception e) {
            log.error("AI 서버 이미지 분석 요청 실패", e);
            throw new RuntimeException("AI 서버 이미지 분석 실패: " + e.getMessage(), e);
        }
    }
}



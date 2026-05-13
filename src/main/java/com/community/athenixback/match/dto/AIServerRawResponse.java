package com.community.athenixback.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 서버의 최상위 응답 구조
 * {
 *   "success": true,
 *   "data": { situation, playGuide },
 *   "error": null
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIServerRawResponse {
    private Boolean success;
    private AIServerResponse data;
    private Object error;
}

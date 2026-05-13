package com.community.athenixback.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIFeedbackRequest {
    private Long timeMs;
    private Boolean isRtl;
}

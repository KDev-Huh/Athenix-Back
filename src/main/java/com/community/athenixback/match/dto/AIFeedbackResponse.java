package com.community.athenixback.match.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIFeedbackResponse {
    private Long feedbackId;
    private Long matchId;
    private Long timeMs;
    private String timeLabel;
    private String situation;
    private PlayGuideDto playGuide;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
}

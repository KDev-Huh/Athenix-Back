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
public class MemoResponse {
    private Long id;
    private Long matchId;
    private Long timeMs;
    private String timeLabel;
    private String label;
    private String text;
    private String thumbnailUrl;
    private Double arrowStartX;
    private Double arrowStartY;
    private Double arrowEndX;
    private Double arrowEndY;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}

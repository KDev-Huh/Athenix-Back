package com.community.athenixback.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoCreateRequest {
    private String text;
    private Long timeMs;
    private String label;
    private Double arrowStartX;
    private Double arrowStartY;
    private Double arrowEndX;
    private Double arrowEndY;
}

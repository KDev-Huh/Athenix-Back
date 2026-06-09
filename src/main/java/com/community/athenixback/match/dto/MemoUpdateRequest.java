package com.community.athenixback.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoUpdateRequest {
    private String text;
    private Double arrowStartX;
    private Double arrowStartY;
    private Double arrowEndX;
    private Double arrowEndY;
}

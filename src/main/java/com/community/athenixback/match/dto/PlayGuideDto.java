package com.community.athenixback.match.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayGuideDto {
    private String type;

    @JsonProperty("start_x")
    private Double startX;

    @JsonProperty("start_y")
    private Double startY;

    @JsonProperty("end_x")
    private Double endX;  // nullable

    @JsonProperty("end_y")
    private Double endY;  // nullable

    private String message;
}

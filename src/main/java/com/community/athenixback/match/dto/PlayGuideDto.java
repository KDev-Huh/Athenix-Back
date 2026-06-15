package com.community.athenixback.match.dto;

import com.community.athenixback.match.entity.ArrowStyle;
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

    @JsonProperty("arrow_style")
    private ArrowStyle arrowStyle;

    private CoordinateDto start;
    private CoordinateDto end;

    @JsonProperty("start_pixel")
    private CoordinateDto startPixel;

    @JsonProperty("end_pixel")
    private CoordinateDto endPixel;

    private String message;

    @JsonProperty("frame_width")
    private Double frameWidth;

    @JsonProperty("frame_height")
    private Double frameHeight;

    /**
     * 좌표 정보 (x, y)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CoordinateDto {
        private Double x;
        private Double y;
    }
}

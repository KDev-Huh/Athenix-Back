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
public class VideoMetaResponse {
    @JsonProperty("durationSec")
    private Long durationSec;
    private String codec;
    @JsonProperty("sizeBytes")
    private Long sizeBytes;
    private String thumbnailUrl;
}

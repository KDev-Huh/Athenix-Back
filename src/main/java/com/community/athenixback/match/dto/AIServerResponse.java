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
public class AIServerResponse {
    private String situation;

    @JsonProperty("playGuide")
    private PlayGuideDto playGuide;
}

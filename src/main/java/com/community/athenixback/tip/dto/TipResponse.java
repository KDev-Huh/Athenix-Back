package com.community.athenixback.tip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipResponse {
    private Long id;
    private String title;
    private String content;

    @JsonProperty("createdAt")
    private String createdAt;
}

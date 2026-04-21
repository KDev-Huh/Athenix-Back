package com.community.athenixback.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchUploadRequest {
    private String title;
    private String date;
    private String description;
    private String opponentName;
    private String teamName;
    private String position;
    private Integer jerseyNumber;
}

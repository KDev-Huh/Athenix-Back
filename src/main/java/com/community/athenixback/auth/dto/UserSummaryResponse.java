package com.community.athenixback.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryResponse {
    private int totalMatches;
    private int totalMemos;
    private int aiAnalysisCount;
}

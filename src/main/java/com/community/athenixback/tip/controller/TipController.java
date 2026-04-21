package com.community.athenixback.tip.controller;

import com.community.athenixback.common.response.ApiResponse;
import com.community.athenixback.tip.dto.TipResponse;
import com.community.athenixback.tip.service.TipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/tips")
@RequiredArgsConstructor
public class TipController {

    private final TipService tipService;

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<TipResponse>> getTodayTip() {
        log.info("오늘의 팁 조회");
        TipResponse response = tipService.getTodayTip();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

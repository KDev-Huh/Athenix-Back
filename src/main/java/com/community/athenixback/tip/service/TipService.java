package com.community.athenixback.tip.service;

import com.community.athenixback.common.exception.ResourceNotFoundException;
import com.community.athenixback.tip.dto.TipResponse;
import com.community.athenixback.tip.entity.Tip;
import com.community.athenixback.tip.repository.TipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class TipService {

    private final TipRepository tipRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Transactional(readOnly = true)
    public TipResponse getTodayTip() {
        Tip tip = tipRepository.findRandomTip()
            .orElseThrow(() -> new ResourceNotFoundException("TIP_NOT_FOUND", "팁을 찾을 수 없습니다."));

        return mapToResponse(tip);
    }

    private TipResponse mapToResponse(Tip tip) {
        return TipResponse.builder()
            .id(tip.getId())
            .title(tip.getTitle())
            .content(tip.getContent())
            .createdAt(tip.getCreatedAt().format(DATE_FORMATTER))
            .build();
    }
}

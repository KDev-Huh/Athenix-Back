package com.community.athenixback.match.service;

import com.community.athenixback.auth.entity.User;
import com.community.athenixback.common.exception.ResourceNotFoundException;
import com.community.athenixback.common.util.VideoThumbnailUtil;
import com.community.athenixback.match.dto.MemoCreateRequest;
import com.community.athenixback.match.dto.MemoResponse;
import com.community.athenixback.match.dto.MemoUpdateRequest;
import com.community.athenixback.match.dto.PlayGuideDto;
import com.community.athenixback.match.entity.AIFeedback;
import com.community.athenixback.match.entity.Match;
import com.community.athenixback.match.entity.Memo;
import com.community.athenixback.match.repository.MatchRepository;
import com.community.athenixback.match.repository.MemoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoService {

    private final MemoRepository memoRepository;
    private final MatchRepository matchRepository;
    private final VideoThumbnailUtil videoThumbnailUtil;
    private final ObjectMapper objectMapper;

    @Transactional
    public MemoResponse createMemo(Long matchId, User user, MemoCreateRequest request) {
        Match match = matchRepository.findByIdAndUser(matchId, user)
            .orElseThrow(() -> new ResourceNotFoundException("MATCH_NOT_FOUND", "경기를 찾을 수 없습니다."));

        // 썸네일 생성
        String thumbnailUrl = null;
        if (match.getVideoFilePath() != null) {
            thumbnailUrl = videoThumbnailUtil.generateThumbnail(match.getVideoFilePath(), request.getTimeMs());
        }

        Memo memo = Memo.builder()
            .match(match)
            .timeMs(request.getTimeMs())
            .text(request.getText())
            .label(request.getLabel() != null ? request.getLabel() : "메모")
            .thumbnailUrl(thumbnailUrl)
            .arrowStartX(request.getArrowStartX())
            .arrowStartY(request.getArrowStartY())
            .arrowEndX(request.getArrowEndX())
            .arrowEndY(request.getArrowEndY())
            .build();

        memoRepository.save(memo);
        return mapToResponse(memo);
    }

    @Transactional(readOnly = true)
    public Page<MemoResponse> getAllMemos(User user, Pageable pageable) {
        Page<Memo> memos = memoRepository.findByMatchUser(user, pageable);
        return memos.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<MemoResponse> getRecentMemos(int limit) {
        // 최근 메모를 전체에서 가져오기 (정렬은 DB에서 처리)
        Page<Memo> memos = memoRepository.findAll(
            org.springframework.data.domain.PageRequest.of(0, limit,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
        );
        return memos.getContent().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MemoResponse> getMemosByMatch(Long matchId, User user, String sort) {
        Match match = matchRepository.findByIdAndUser(matchId, user)
            .orElseThrow(() -> new ResourceNotFoundException("MATCH_NOT_FOUND", "경기를 찾을 수 없습니다."));

        List<Memo> memos;
        if ("oldest".equalsIgnoreCase(sort)) {
            memos = memoRepository.findByMatchOrderByCreatedAtAsc(match);
        } else {
            memos = memoRepository.findByMatchOrderByCreatedAtDesc(match);
        }

        return memos.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public MemoResponse updateMemo(Long memoId, User user, MemoUpdateRequest request) {
        Memo memo = memoRepository.findById(memoId)
            .orElseThrow(() -> new ResourceNotFoundException("MEMO_NOT_FOUND", "메모를 찾을 수 없습니다."));

        // 사용자 권한 확인
        if (!memo.getMatch().getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("MEMO_NOT_FOUND", "메모를 찾을 수 없습니다.");
        }

        memo.setText(request.getText());
        memo.setArrowStartX(request.getArrowStartX());
        memo.setArrowStartY(request.getArrowStartY());
        memo.setArrowEndX(request.getArrowEndX());
        memo.setArrowEndY(request.getArrowEndY());
        memoRepository.save(memo);
        return mapToResponse(memo);
    }

    @Transactional
    public void deleteMemo(Long memoId, User user) {
        Memo memo = memoRepository.findById(memoId)
            .orElseThrow(() -> new ResourceNotFoundException("MEMO_NOT_FOUND", "메모를 찾을 수 없습니다."));

        // 사용자 권한 확인
        if (!memo.getMatch().getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("MEMO_NOT_FOUND", "메모를 찾을 수 없습니다.");
        }

        // 썸네일 파일 삭제
        if (memo.getThumbnailUrl() != null) {
            videoThumbnailUtil.deleteThumbnail(memo.getThumbnailUrl());
        }

        memoRepository.delete(memo);
    }

    /**
     * 경기 삭제 시 모든 관련 메모와 썸네일 삭제
     */
    @Transactional
    public void deleteAllMemosByMatch(Match match) {
        List<Memo> memos = memoRepository.findByMatchOrderByCreatedAtDesc(match);

        for (Memo memo : memos) {
            // 각 메모의 썸네일 파일 삭제
            if (memo.getThumbnailUrl() != null) {
                videoThumbnailUtil.deleteThumbnail(memo.getThumbnailUrl());
            }
            // 메모 삭제
            memoRepository.delete(memo);
        }
    }

    /**
     * AI 피드백을 메모로 추가 (7.3 API)
     */
    @Transactional
    public MemoResponse addFeedbackAsMemo(Match match, AIFeedback feedback, String label) {
        String memoText = feedback.getSituation();
        PlayGuideDto playGuide = null;

        try {
            playGuide = objectMapper.readValue(feedback.getPlayGuideJson(), PlayGuideDto.class);
            memoText = playGuide.getMessage() != null ? playGuide.getMessage() : feedback.getSituation();
        } catch (Exception e) {
            log.warn("playGuideJson 파싱 실패, situation으로 대체: feedbackId={}", feedback.getId());
        }

        PlayGuideDto.CoordinateDto startCoord = playGuide != null
            ? (playGuide.getStartPixel() != null ? playGuide.getStartPixel() : playGuide.getStart())
            : null;
        PlayGuideDto.CoordinateDto endCoord = playGuide != null
            ? (playGuide.getEndPixel() != null ? playGuide.getEndPixel() : playGuide.getEnd())
            : null;

        Memo memo = Memo.builder()
            .match(match)
            .timeMs(feedback.getTimeMs())
            .text(memoText)
            .label(label)
            .arrowStartX(startCoord != null ? startCoord.getX() : null)
            .arrowStartY(startCoord != null ? startCoord.getY() : null)
            .arrowEndX(endCoord != null ? endCoord.getX() : null)
            .arrowEndY(endCoord != null ? endCoord.getY() : null)
            .build();

        memoRepository.save(memo);
        log.info("AI 피드백을 메모로 저장: memoId={}, feedbackId={}", memo.getId(), feedback.getId());

        return mapToResponse(memo);
    }

    private MemoResponse mapToResponse(Memo memo) {
        return MemoResponse.builder()
            .id(memo.getId())
            .matchId(memo.getMatch().getId())
            .timeMs(memo.getTimeMs())
            .timeLabel(memo.getTimeLabel())
            .label(memo.getLabel())
            .text(memo.getText())
            .thumbnailUrl(memo.getThumbnailUrl())
            .arrowStartX(memo.getArrowStartX())
            .arrowStartY(memo.getArrowStartY())
            .arrowEndX(memo.getArrowEndX())
            .arrowEndY(memo.getArrowEndY())
            .createdAt(memo.getCreatedAt())
            .updatedAt(memo.getUpdatedAt())
            .build();
    }
}

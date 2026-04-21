package com.community.athenixback.match.service;

import com.community.athenixback.auth.entity.User;
import com.community.athenixback.common.exception.ResourceNotFoundException;
import com.community.athenixback.common.util.FileStorageUtil;
import com.community.athenixback.common.util.VideoThumbnailUtil;
import com.community.athenixback.match.dto.MatchResponse;
import com.community.athenixback.match.dto.MatchUploadRequest;
import com.community.athenixback.match.entity.Match;
import com.community.athenixback.match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final FileStorageUtil fileStorageUtil;
    private final VideoThumbnailUtil videoThumbnailUtil;
    private final AIFeedbackService aiFeedbackService;
    private final MemoService memoService;

    @Transactional
    public MatchResponse uploadMatch(User user, MultipartFile file, MatchUploadRequest request) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일을 선택해주세요.");
        }

        // 파일 저장
        String filePath = fileStorageUtil.saveVideoFile(file);
        long fileSize = fileStorageUtil.getFileSize(filePath);

        // 썸네일 생성 (첫 번째 프레임에서 0초 시점)
        String thumbnailUrl = null;
        try {
            thumbnailUrl = videoThumbnailUtil.generateThumbnail(filePath, 0);
            log.info("썸네일 생성 완료: {}", thumbnailUrl);
        } catch (Exception e) {
            log.warn("썸네일 생성 실패: {}", e.getMessage());
        }

        // Match 엔티티 생성
        Match match = Match.builder()
            .user(user)
            .title(request.getTitle() != null ? request.getTitle() : file.getOriginalFilename())
            .date(request.getDate())
            .description(request.getDescription())
            .opponentName(request.getOpponentName())
            .teamName(request.getTeamName())
            .position(request.getPosition())
            .jerseyNumber(request.getJerseyNumber())
            .videoFilePath(filePath)
            .videoFileName(file.getOriginalFilename())
            .videoFileSize(fileSize)
            .thumbnailUrl(thumbnailUrl)
            .status("임시 저장")
            .build();

        matchRepository.save(match);
        return mapToResponse(match);
    }

    @Transactional(readOnly = true)
    public Page<MatchResponse> getMatches(User user, String status, Pageable pageable) {
        Page<Match> matches;
        if (status != null && !status.isEmpty()) {
            matches = matchRepository.findByUserAndStatus(user, status, pageable);
        } else {
            matches = matchRepository.findByUser(user, pageable);
        }
        return matches.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> getRecentMatches(User user, int limit) {
        List<Match> matches = matchRepository.findByUserOrderByCreatedAtDesc(user);
        return matches.stream()
            .limit(limit)
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MatchResponse getMatchById(Long matchId, User user) {
        Match match = matchRepository.findByIdAndUser(matchId, user)
            .orElseThrow(() -> new ResourceNotFoundException("MATCH_NOT_FOUND", "경기를 찾을 수 없습니다."));
        return mapToResponse(match);
    }

    @Transactional(readOnly = true)
    public Optional<Match> getMatchById(Long matchId) {
        return matchRepository.findById(matchId);
    }

    @Transactional
    public MatchResponse updateMatchStatus(Long matchId, User user, String status) {
        Match match = matchRepository.findByIdAndUser(matchId, user)
            .orElseThrow(() -> new ResourceNotFoundException("MATCH_NOT_FOUND", "경기를 찾을 수 없습니다."));

        match.setStatus(status);
        matchRepository.save(match);
        return mapToResponse(match);
    }

    @Transactional
    public void deleteMatch(Long matchId, User user) {
        Match match = matchRepository.findByIdAndUser(matchId, user)
            .orElseThrow(() -> new ResourceNotFoundException("MATCH_NOT_FOUND", "경기를 찾을 수 없습니다."));

        log.info("경기 삭제 시작: matchId={}, title={}", matchId, match.getTitle());

        // 1. 메모 삭제 (썸네일 포함)
        memoService.deleteAllMemosByMatch(match);
        log.info("메모 삭제 완료: matchId={}", matchId);

        // 2. AI 피드백 삭제
        aiFeedbackService.deleteByMatchId(matchId);
        log.info("AI 피드백 삭제 완료: matchId={}", matchId);

        // 3. 썸네일 파일 삭제
        if (match.getThumbnailUrl() != null) {
            videoThumbnailUtil.deleteThumbnail(match.getThumbnailUrl());
            log.info("썸네일 파일 삭제 완료: {}", match.getThumbnailUrl());
        }

        // 4. 영상 파일 삭제
        if (match.getVideoFilePath() != null) {
            fileStorageUtil.deleteVideoFile(match.getVideoFilePath());
            log.info("영상 파일 삭제 완료: {}", match.getVideoFilePath());
        }

        // 5. 경기 삭제
        matchRepository.delete(match);
        log.info("경기 삭제 완료: matchId={}", matchId);
    }

    private MatchResponse mapToResponse(Match match) {
        return MatchResponse.builder()
            .id(match.getId())
            .title(match.getTitle())
            .date(match.getDate())
            .description(match.getDescription())
            .status(match.getStatus())
            .opponentName(match.getOpponentName())
            .teamName(match.getTeamName())
            .position(match.getPosition())
            .jerseyNumber(match.getJerseyNumber())
            .thumbnailUrl(match.getThumbnailUrl())
            .createdAt(match.getCreatedAt())
            .updatedAt(match.getUpdatedAt())
            .build();
    }
}

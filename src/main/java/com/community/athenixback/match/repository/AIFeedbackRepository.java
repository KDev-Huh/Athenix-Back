package com.community.athenixback.match.repository;

import com.community.athenixback.match.entity.AIFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AIFeedbackRepository extends JpaRepository<AIFeedback, Long> {
    Page<AIFeedback> findByMatchId(Long matchId, Pageable pageable);
}

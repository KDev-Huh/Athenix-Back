package com.community.athenixback.match.repository;

import com.community.athenixback.match.entity.Match;
import com.community.athenixback.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    Page<Match> findByUser(User user, Pageable pageable);
    Page<Match> findByUserAndStatus(User user, String status, Pageable pageable);
    List<Match> findByUserOrderByCreatedAtDesc(User user);
    Optional<Match> findByIdAndUser(Long id, User user);
}

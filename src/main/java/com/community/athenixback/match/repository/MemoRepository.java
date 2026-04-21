package com.community.athenixback.match.repository;

import com.community.athenixback.match.entity.Memo;
import com.community.athenixback.match.entity.Match;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemoRepository extends JpaRepository<Memo, Long> {
    Page<Memo> findAll(Pageable pageable);
    List<Memo> findByMatchOrderByCreatedAtDesc(Match match);
    List<Memo> findByMatchOrderByCreatedAtAsc(Match match);
    Optional<Memo> findByIdAndMatch(Long id, Match match);
}

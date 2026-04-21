package com.community.athenixback.tip.repository;

import com.community.athenixback.tip.entity.Tip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipRepository extends JpaRepository<Tip, Long> {
    @Query(value = "SELECT * FROM tips ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<Tip> findRandomTip();
}

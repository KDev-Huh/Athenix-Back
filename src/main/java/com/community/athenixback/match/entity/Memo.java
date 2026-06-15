package com.community.athenixback.match.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "memos", indexes = {
    @Index(name = "idx_match_id", columnList = "match_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Memo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(nullable = false)
    private Long timeMs;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false)
    private String label;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "arrow_start_x")
    private Double arrowStartX;

    @Column(name = "arrow_start_y")
    private Double arrowStartY;

    @Column(name = "arrow_end_x")
    private Double arrowEndX;

    @Column(name = "arrow_end_y")
    private Double arrowEndY;

    @Enumerated(EnumType.STRING)
    @Column(name = "arrow_style", length = 10)
    private ArrowStyle arrowStyle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getTimeLabel() {
        long totalSeconds = timeMs / 1000;
        long milliseconds = timeMs % 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d.%03d", minutes, seconds, milliseconds);
    }
}

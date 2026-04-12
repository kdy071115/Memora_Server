package com.kit.memora_server.domain.analysis.entity;

import com.kit.memora_server.domain.lecture.entity.Lecture;
import com.kit.memora_server.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "learning_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LearningLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @Column(nullable = false, length = 30)
    private String activityType;

    private Integer duration;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

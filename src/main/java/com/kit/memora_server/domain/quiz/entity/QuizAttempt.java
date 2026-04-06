package com.kit.memora_server.domain.quiz.entity;

import com.kit.memora_server.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String userAnswer;

    private Boolean isCorrect;

    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String aiFeedback;

    private Integer timeSpent;

    @Builder.Default
    private LocalDateTime attemptedAt = LocalDateTime.now();
}

package com.kit.memora_server.domain.quiz.entity;

import com.kit.memora_server.domain.lecture.entity.Lecture;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "quizzes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(nullable = false, length = 20)
    private String quizType;

    @Column(columnDefinition = "TEXT")
    private String options;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(length = 10)
    @Builder.Default
    private String difficulty = "MEDIUM";

    private String conceptTag;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void update(String question, String quizType, String options,
                       String correctAnswer, String explanation,
                       String difficulty, String conceptTag) {
        if (question != null) this.question = question;
        if (quizType != null) this.quizType = quizType;
        this.options = options;
        if (correctAnswer != null) this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        if (difficulty != null) this.difficulty = difficulty;
        this.conceptTag = conceptTag;
    }
}

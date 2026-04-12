package com.kit.memora_server.domain.assignment.entity;

import com.kit.memora_server.domain.course.entity.Course;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "assignments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Assignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    private LocalDateTime dueDate;

    /** 팀 단위 제출을 허용할지 — false 면 개인 제출만 가능 */
    @Column(nullable = false)
    @Builder.Default
    private boolean allowTeamSubmission = false;

    /**
     * 강사가 dueDate 도래 전에 수동으로 조기 마감했는지.
     * true 가 되면 dueDate 와 무관하게 학생 제출/수정이 막힌다.
     * 강사가 다시 false 로 되돌릴 수 있다.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean closedEarly = false;

    public void update(String title, String description, LocalDateTime dueDate, Boolean allowTeamSubmission) {
        if (title != null && !title.isBlank()) this.title = title;
        if (description != null) this.description = description;
        this.dueDate = dueDate;
        if (allowTeamSubmission != null) this.allowTeamSubmission = allowTeamSubmission;
    }

    public void closeEarly() {
        this.closedEarly = true;
    }

    public void reopen() {
        this.closedEarly = false;
    }
}

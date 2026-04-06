package com.kit.memora_server.domain.course.entity;

import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Course extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @Column(length = 500)
    private String thumbnailUrl;

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(length = 10, unique = true)
    private String inviteCode;

    public void update(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public void regenerateInviteCode(String code) {
        this.inviteCode = code;
    }
}

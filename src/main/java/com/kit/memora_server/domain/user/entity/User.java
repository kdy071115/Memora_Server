package com.kit.memora_server.domain.user.entity;

import com.kit.memora_server.domain.user.enums.UserRole;
import com.kit.memora_server.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(length = 500)
    private String profileImage;

    /** 학생이 마지막으로 피드백 알림을 확인한 시각. 이 이후의 댓글이 "미확인"으로 카운트됨. */
    private LocalDateTime feedbackSeenAt;

    public void updateName(String name) {
        this.name = name;
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void markFeedbackSeen() {
        this.feedbackSeenAt = LocalDateTime.now();
    }
}

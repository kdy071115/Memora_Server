package com.kit.memora_server.domain.team.entity;

import com.kit.memora_server.domain.course.entity.Course;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teams")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Team extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", nullable = false)
    private User leader;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    public void rename(String name, String description) {
        if (name != null && !name.isBlank()) this.name = name;
        if (description != null) this.description = description;
    }
}

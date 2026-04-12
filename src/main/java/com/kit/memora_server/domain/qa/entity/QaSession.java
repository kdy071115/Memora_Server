package com.kit.memora_server.domain.qa.entity;

import com.kit.memora_server.domain.lecture.entity.Lecture;
import com.kit.memora_server.domain.user.entity.User;
import com.kit.memora_server.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "qa_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class QaSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    private String title;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QaMessage> messages = new ArrayList<>();

    public void updateTitle(String title) {
        this.title = title;
    }
}

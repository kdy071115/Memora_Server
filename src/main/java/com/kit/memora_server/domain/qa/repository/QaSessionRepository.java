package com.kit.memora_server.domain.qa.repository;

import com.kit.memora_server.domain.qa.entity.QaSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QaSessionRepository extends JpaRepository<QaSession, Long> {

    List<QaSession> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<QaSession> findByUserIdAndLectureIdOrderByUpdatedAtDesc(Long userId, Long lectureId);
}

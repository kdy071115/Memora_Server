package com.kit.memora_server.domain.notice.repository;

import com.kit.memora_server.domain.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findByCourseIdOrderByPinnedDescCreatedAtDesc(Long courseId);
}

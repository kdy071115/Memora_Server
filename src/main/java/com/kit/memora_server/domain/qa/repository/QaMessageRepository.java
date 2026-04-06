package com.kit.memora_server.domain.qa.repository;

import com.kit.memora_server.domain.qa.entity.QaMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QaMessageRepository extends JpaRepository<QaMessage, Long> {

    List<QaMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    long countBySessionId(Long sessionId);
}

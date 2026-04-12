package com.kit.memora_server.domain.qa.repository;

import com.kit.memora_server.domain.qa.entity.QaMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QaMessageRepository extends JpaRepository<QaMessage, Long> {

    List<QaMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    long countBySessionId(Long sessionId);

    @Modifying
    @Query("DELETE FROM QaMessage m WHERE m.session.id = :sessionId")
    void deleteBySessionId(@Param("sessionId") Long sessionId);
}

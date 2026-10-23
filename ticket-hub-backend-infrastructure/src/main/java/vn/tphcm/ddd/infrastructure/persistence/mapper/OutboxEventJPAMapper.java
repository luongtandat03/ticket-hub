/*
 * @ (#) OutboxEventJPAMapper.java       1.0     9/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.tphcm.ddd.domain.model.OutboxEvent;

import java.time.LocalDateTime;
import java.util.List;

/*
 * @author: Luong Tan Dat
 * @date: 9/13/2026
 */

public interface OutboxEventJPAMapper extends JpaRepository<OutboxEvent, String> {

    @Query(value = """
            SELECT * FROM tbl_outbox_event WHERE status = 0 AND event_type = :eventType ORDER BY created_at ASC LIMIT :limit FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findPending(@Param("eventType") String eventType, @Param("limit") int limit);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 1, " +
            "e.publishedAt = :publishedAt " +
            "WHERE e.id IN :ids")
    void markPublishedByIds(@Param("ids") List<String> ids, @Param("publishedAt") LocalDateTime publishedAt);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 1, " +
            "e.publishedAt = :publishedAt " +
            "WHERE e.id = :id AND e.status = 0")
    void markPublishedById(@Param("id") String id, @Param("publishedAt") LocalDateTime publishedAt);

}

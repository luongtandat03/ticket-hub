/*
 * @ (#) OutboxEventJPAMapper.java       1.0     9/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.mapper;

import org.springframework.data.domain.Pageable;
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

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 0 ORDER BY e.createdAt ASC ")
    List<OutboxEvent> findPending(Pageable pageable);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 1, " +
            "e.publishedAt = :publishedAt " +
            "WHERE e.id IN :ids")
    void markPublishedByIds(@Param("ids") List<String> ids, @Param("publishedAt") LocalDateTime publishedAt);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 1, " +
            "e.publishedAt = :publishedAt " +
            "WHERE e.id = :id")
    void markPublishedById(@Param("id") String id, @Param("publishedAt") LocalDateTime publishedAt);

}

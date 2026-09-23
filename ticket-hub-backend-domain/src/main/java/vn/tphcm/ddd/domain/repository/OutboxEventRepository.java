/*
 * @ (#) OutboxRepository.java       1.0     9/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.repository;
/*
 * @author: Luong Tan Dat
 * @date: 9/13/2026
 */

import vn.tphcm.ddd.domain.model.OutboxEvent;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository {
    void save(OutboxEvent outboxEvent);

    List<OutboxEvent> findPendingBatch(int limit);

    void markPublished(String id, LocalDateTime publishedAt);

    void markPublishedBatch(List<String> ids, LocalDateTime publishedAt);
}

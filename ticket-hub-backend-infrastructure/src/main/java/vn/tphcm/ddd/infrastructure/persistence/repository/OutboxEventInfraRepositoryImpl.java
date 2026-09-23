/*
 * @ (#) OutboxRepositoryImpl.java       1.0     9/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.repository;
/*
 * @author: Luong Tan Dat
 * @date: 9/13/2026
 */

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.domain.model.OutboxEvent;
import vn.tphcm.ddd.domain.repository.OutboxEventRepository;
import vn.tphcm.ddd.infrastructure.persistence.mapper.OutboxEventJPAMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "OUTBOX-REPOSITORY")
public class OutboxEventInfraRepositoryImpl implements OutboxEventRepository {
    private final OutboxEventJPAMapper outboxEventJPAMapper;

    @Override
    public void save(OutboxEvent outboxEvent) {
        outboxEventJPAMapper.save(outboxEvent);
    }

    @Override
    public List<OutboxEvent> findPendingBatch(int limit) {
        return outboxEventJPAMapper.findPending(PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public void markPublished(String id, LocalDateTime publishedAt) {
        outboxEventJPAMapper.markPublishedById(id, publishedAt);
    }

    @Override
    @Transactional
    public void markPublishedBatch(List<String> ids, LocalDateTime publishedAt) {
        outboxEventJPAMapper.markPublishedByIds(ids, publishedAt);
    }
}

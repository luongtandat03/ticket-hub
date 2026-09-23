/*
 * @ (#) OrderQueueRepository.java       1.0     9/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.repository;
/*
 * @author: Luong Tan Dat
 * @date: 9/13/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.domain.model.OrderQueue;
import vn.tphcm.ddd.domain.repository.OrderQueueRepository;
import vn.tphcm.ddd.infrastructure.persistence.mapper.OrderQueueJPAMapper;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderQueueInfraRepositoryImpl implements OrderQueueRepository {
    private final OrderQueueJPAMapper orderQueueJPAMapper;

    @Override
    public OrderQueue save(OrderQueue orderQueue) {
        return orderQueueJPAMapper.save(orderQueue);
    }

    @Override
    public Optional<OrderQueue> findByToken(String token) {
        return orderQueueJPAMapper.findByToken(token);
    }

    @Override
    public boolean updateStatus(String token, int status, String orderNumber, String message) {
        return orderQueueJPAMapper.updateStatusByToken(token, status, orderNumber, message) > 0;
    }
}

/*
 * @ (#) OrderQueueRepository.java       1.0     9/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.repository;

/*
 * @author: Luong Tan Dat
 * @date: 9/13/2026
 */

import vn.tphcm.ddd.domain.model.OrderQueue;

import java.util.Optional;

public interface OrderQueueRepository {
    OrderQueue save(OrderQueue orderQueue);

    Optional<OrderQueue> findByToken(String token);

    boolean updateStatus(String token, int status, String orderNumber, String message);
}

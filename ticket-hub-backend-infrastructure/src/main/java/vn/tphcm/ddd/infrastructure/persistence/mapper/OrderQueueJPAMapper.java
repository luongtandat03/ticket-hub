/*
 * @ (#) OrderQueueJPAMapper.java       1.0     9/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.mapper;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.tphcm.ddd.domain.model.OrderQueue;

import java.util.Optional;

/*
 * @author: Luong Tan Dat
 * @date: 9/13/2026
 */

public interface OrderQueueJPAMapper extends JpaRepository<OrderQueue, String> {
    Optional<OrderQueue> findByToken(String token);

    @Modifying
    @Query("UPDATE OrderQueue q SET q.status = :status, q.orderNumber = :orderNumber, q.message = :message " +
            " WHERE q.token = :token")
    int updateStatusByToken(@Param("token") String token,
                            @Param("status") int status,
                            @Param("orderNumber") String orderNumber,
                            @Param("message") String message);
}

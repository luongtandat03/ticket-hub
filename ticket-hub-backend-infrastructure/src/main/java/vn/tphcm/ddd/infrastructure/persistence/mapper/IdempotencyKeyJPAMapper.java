/*
 * @ (#) IdempotencyKeyJPAMapper.java       1.0     9/15/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.mapper;

/*
 * @author: Luong Tan Dat
 * @date: 9/15/2026
 */


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.tphcm.ddd.domain.model.IdempotencyKey;

import java.time.LocalDateTime;

public interface IdempotencyKeyJPAMapper extends JpaRepository<IdempotencyKey, String> {

    @Modifying
    @Query(value = "INSERT IGNORE INTO tbl_idempotency_key (token, created_at, expired_at) VALUES (:token, :createdAt, :expiredAt)",
            nativeQuery = true)
    int insertIgnore(
            @Param("token") String token,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("expiredAt") LocalDateTime expiresAt
    );
}

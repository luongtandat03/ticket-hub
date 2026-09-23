/*
 * @ (#) IdempotencyKeyRepository.java       1.0     9/15/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.repository;

import java.time.LocalDateTime;

/*
 * @author: Luong Tan Dat
 * @date: 9/15/2026
 */
public interface IdempotencyKeyRepository {
    boolean tryInsert(String token, LocalDateTime expiresAt);
}

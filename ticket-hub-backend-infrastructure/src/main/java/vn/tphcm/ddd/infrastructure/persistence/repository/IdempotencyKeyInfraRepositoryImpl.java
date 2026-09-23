/*
 * @ (#) IdempotencyKeyRepositoryImpl.java       1.0     9/15/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.repository;
/*
 * @author: Luong Tan Dat
 * @date: 9/15/2026
 */

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.tphcm.ddd.domain.repository.IdempotencyKeyRepository;
import vn.tphcm.ddd.infrastructure.persistence.mapper.IdempotencyKeyJPAMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Repository
@RequiredArgsConstructor
public class IdempotencyKeyInfraRepositoryImpl implements IdempotencyKeyRepository {
    private final IdempotencyKeyJPAMapper idempotencyKeyJPAMapper;

    @Override
    public boolean tryInsert(String token, LocalDateTime expiresAt) {
        int affected = idempotencyKeyJPAMapper.insertIgnore(token, LocalDateTime.now(ZoneId.systemDefault()), expiresAt);
        return affected == 1;
    }
}

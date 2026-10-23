/*
 * @ (#) RoleInfraRepositoryImpl.java       1.0     9/25/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.repository;
/*
 * @author: Luong Tan Dat
 * @date: 9/25/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import vn.tphcm.ddd.domain.model.Role;
import vn.tphcm.ddd.domain.repository.RoleRepository;
import vn.tphcm.ddd.infrastructure.persistence.mapper.RoleJPAMapper;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j(topic = "ROLE-INFRASTRUCTURE")
public class RoleInfraRepositoryImpl implements RoleRepository {
    private final RoleJPAMapper roleJPAMapper;

    @Override
    public Role findByKey(String key) {
        log.info("Selecting role by key {}", key);

        return roleJPAMapper.findByKey(key).orElse(null);
    }

    @Override
    public List<Role> selectRolesByUserId(String userId) {
        log.info("selectRolesByUserId");

        return roleJPAMapper.selectRolesByUserId(userId);
    }
}

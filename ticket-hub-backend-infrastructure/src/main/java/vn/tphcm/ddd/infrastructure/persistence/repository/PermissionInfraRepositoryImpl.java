/*
 * @ (#) PermissionInfraRepositoryImpl.java       1.0     9/25/2026
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
import vn.tphcm.ddd.domain.model.Permission;
import vn.tphcm.ddd.domain.repository.PermissionRepository;
import vn.tphcm.ddd.infrastructure.persistence.mapper.PermissionJPAMapper;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j(topic = "PERMISSION-INFRASTRUCRE")
public class PermissionInfraRepositoryImpl implements PermissionRepository {
    private final PermissionJPAMapper permissionJPAMapper;

    @Override
    public List<Permission> selectPermissionsByRoleId(String roleId) {
        log.info("selectPermissionsByUserId()");

        return permissionJPAMapper.selectPermissionsByRoleId(roleId);
    }
}

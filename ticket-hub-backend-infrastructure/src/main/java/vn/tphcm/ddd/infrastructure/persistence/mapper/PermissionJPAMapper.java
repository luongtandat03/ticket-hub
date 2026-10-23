/*
 * @ (#) PermissionJPAMapper.java       1.0     9/25/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.mapper;
/*
 * @author: Luong Tan Dat
 * @date: 9/25/2026
 */

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.tphcm.ddd.domain.model.Permission;

import java.util.List;

public interface PermissionJPAMapper extends JpaRepository<Permission, String> {

    @Query(value = "SELECT p.* FROM tbl_permission p " +
            "JOIN tbl_role_has_permission rm ON p.id = rm.permission_id " +
            "WHERE rm.role_id = :roleId", nativeQuery = true)
    List<Permission> selectPermissionsByRoleId(@Param("roleId") String roleId);
}

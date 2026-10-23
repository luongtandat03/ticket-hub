/*
 * @ (#) PermissionRepository.java       1.0     9/25/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.repository;

/*
 * @author: Luong Tan Dat
 * @date: 9/25/2026
 */


import vn.tphcm.ddd.domain.model.Permission;

import java.util.List;

public interface PermissionRepository {
    List<Permission> selectPermissionsByRoleId(String roleId);
}

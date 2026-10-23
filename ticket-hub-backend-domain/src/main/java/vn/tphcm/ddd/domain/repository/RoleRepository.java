/*
 * @ (#) RoleRepository.java       1.0     9/25/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.repository;

import org.springframework.stereotype.Repository;
import vn.tphcm.ddd.domain.model.Role;

import java.util.List;

/*
 * @author: Luong Tan Dat
 * @date: 9/25/2026
 */

@Repository
public interface RoleRepository {
    Role findByKey(String key);

    List<Role> selectRolesByUserId(String userId);
}

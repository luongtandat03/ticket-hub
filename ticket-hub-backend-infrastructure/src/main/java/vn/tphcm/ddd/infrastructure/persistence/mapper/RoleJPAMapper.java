/*
 * @ (#) RoleJPAMapper.java       1.0     9/25/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.tphcm.ddd.domain.model.Role;

import java.util.List;
import java.util.Optional;

/*
 * @author: Luong Tan Dat
 * @date: 9/25/2026
 */
public interface RoleJPAMapper extends JpaRepository<Role, String> {

    @Query("SELECT r FROM Role r WHERE r.key = :key")
    Optional<Role> findByKey(@Param("key") String key);

    @Query(value = "SELECT r.* FROM tbl_role r " +
            "JOIN tbl_user_has_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = :userId", nativeQuery = true)
    List<Role> selectRolesByUserId(@Param("userId") String userId);
}

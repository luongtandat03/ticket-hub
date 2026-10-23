/*
 * @ (#) UserJPAMapper.java       1.0     9/21/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.tphcm.ddd.domain.model.User;

import java.util.Optional;

/*
 * @author: Luong Tan Dat
 * @date: 9/21/2026
 */

public interface UserJPAMapper extends JpaRepository<User, String> {
    Optional<User> findByUsernameOrEmailOrPhoneNumber(String username, String email, String phonenumber);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);
}

/*
 * @ (#) UserRepository.java       1.0     9/21/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.repository;

import org.springframework.stereotype.Repository;
import vn.tphcm.ddd.domain.model.User;

/*
 * @author: Luong Tan Dat
 * @date: 9/21/2026
 */

@Repository
public interface UserRepository {
    User findByUsernameOrEmailOrPhone(String username, String email, String phonenumber);

    User save(User user);

    User findByEmail(String email);

    User findById(String id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhonenumber(String phonenumber);
}

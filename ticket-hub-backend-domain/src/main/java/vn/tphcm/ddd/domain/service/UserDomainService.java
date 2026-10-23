/*
 * @ (#) UserService.java       1.0     9/21/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.service;

import vn.tphcm.ddd.domain.model.User;

/*
 * @author: Luong Tan Dat
 * @date: 9/21/2026
 */
public interface UserDomainService {
    User save(User user);

    User findById(String id);

    User findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);
}

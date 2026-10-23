/*
 * @ (#) UserRepostioryImpl.java       1.0     9/21/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.tphcm.ddd.domain.model.User;
import vn.tphcm.ddd.domain.repository.UserRepository;
import vn.tphcm.ddd.infrastructure.persistence.mapper.UserJPAMapper;

/*
 * @author: Luong Tan Dat
 * @date: 9/21/2026
 */

@Repository
@RequiredArgsConstructor
public class UserInfraRepostioryImpl implements UserRepository {
    private final UserJPAMapper userJPAMapper;

    @Override
    public User findByUsernameOrEmailOrPhone(String username, String email, String phonenumber) {
        return userJPAMapper.findByUsernameOrEmailOrPhoneNumber(username, email, phonenumber).orElse(null);
    }

    @Override
    public User save(User user) {
        return userJPAMapper.save(user);
    }

    @Override
    public User findByEmail(String email) {
        return userJPAMapper.findByEmail(email).orElse(null);
    }

    @Override
    public User findById(String id) {
        return userJPAMapper.findById(id).orElse(null);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userJPAMapper.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJPAMapper.existsByEmail(email);
    }

    @Override
    public boolean existsByPhonenumber(String phonenumber) {
        return userJPAMapper.existsByPhoneNumber(phonenumber);
    }
}

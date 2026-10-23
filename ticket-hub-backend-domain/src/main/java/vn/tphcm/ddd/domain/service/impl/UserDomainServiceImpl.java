/*
 * @ (#) UserDomainService.java       1.0     9/21/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.service.impl;
/*
 * @author: Luong Tan Dat
 * @date: 9/21/2026
 */

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.domain.model.User;
import vn.tphcm.ddd.domain.repository.UserRepository;
import vn.tphcm.ddd.domain.service.UserDomainService;

@Service
@RequiredArgsConstructor
public class UserDomainServiceImpl implements UserDomainService {
    private final UserRepository userRepository;

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public User findById(String id) {
        return userRepository.findById(id);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhonenumber(phoneNumber);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}

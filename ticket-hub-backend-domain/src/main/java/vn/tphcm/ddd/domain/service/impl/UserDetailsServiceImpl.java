/*
 * @ (#) UserDetailServiceImpl.java       1.0     9/25/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.service.impl;
/*
 * @author: Luong Tan Dat
 * @date: 9/25/2026
 */

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.domain.model.Role;
import vn.tphcm.ddd.domain.model.User;
import vn.tphcm.ddd.domain.repository.PermissionRepository;
import vn.tphcm.ddd.domain.repository.RoleRepository;
import vn.tphcm.ddd.domain.repository.UserRepository;
import vn.tphcm.ddd.domain.service.CustomUserDetailsService;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements CustomUserDetailsService {
    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PermissionRepository permissionRepository;

    private final Cache<String, User> userCache = CacheBuilder.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmailOrPhone(username, username, username);

        if (user == null) {
            throw new UsernameNotFoundException(username);
        }

        loadRolesAndPermissions(user);

        userCache.put(user.getId(), user);

        return user;
    }

    @Override
    public UserDetails loadUserById(String id) throws UsernameNotFoundException {
        User cached = userCache.getIfPresent(id);
        if (cached != null) {
            return cached;
        }

        User user = userRepository.findById(id);

        if (user == null) {
            throw new UsernameNotFoundException("User not found with id " + id);
        }

        loadRolesAndPermissions(user);

        userCache.put(id, user);

        return user;
    }

    private void loadRolesAndPermissions(User user) {
        List<Role> roles = roleRepository.selectRolesByUserId(user.getId());

        roles.forEach(role ->
                role.setPermissions(permissionRepository.selectPermissionsByRoleId(role.getId())));

        user.setRoles(roles);
    }
}

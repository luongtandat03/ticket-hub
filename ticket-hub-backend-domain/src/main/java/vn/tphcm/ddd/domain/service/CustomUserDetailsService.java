/*
 * @ (#) CustomUserDetailsService.java       1.0     10/4/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/*
 * @author: Luong Tan Dat
 * @date: 10/4/2026
 */
public interface CustomUserDetailsService extends UserDetailsService {
    UserDetails loadUserById(String userId);
}

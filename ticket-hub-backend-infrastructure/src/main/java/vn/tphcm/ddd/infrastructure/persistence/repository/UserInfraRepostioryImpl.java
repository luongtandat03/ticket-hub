/*
 * @ (#) UserRepostioryImpl.java       1.0     9/21/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
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


}

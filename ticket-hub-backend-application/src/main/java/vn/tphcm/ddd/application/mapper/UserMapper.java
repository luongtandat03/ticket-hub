/*
 * @ (#) UserMapper.java       1.0     10/1/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.mapper;

/*
 * @author: Luong Tan Dat
 * @date: 10/1/2026
 */

import org.mapstruct.Mapper;
import vn.tphcm.ddd.application.dto.response.UserResponse;
import vn.tphcm.ddd.domain.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}

/*
 * @ (#) UserResponse.java       1.0     9/21/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.dto;
/*
 * @author: Luong Tan Dat
 * @date: 9/21/2026
 */

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private String phoneNumber;
    private int status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}

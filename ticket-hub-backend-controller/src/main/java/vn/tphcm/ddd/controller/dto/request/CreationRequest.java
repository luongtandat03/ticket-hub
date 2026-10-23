/*
 * @ (#) CreationRequest.java       1.0     9/29/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.controller.dto.request;
/*
 * @author: Luong Tan Dat
 * @date: 9/29/2026
 */

import lombok.Getter;

@Getter
public class CreationRequest {
    private String username;
    private String email;
    private String password;
    private String phoneNumber;
}

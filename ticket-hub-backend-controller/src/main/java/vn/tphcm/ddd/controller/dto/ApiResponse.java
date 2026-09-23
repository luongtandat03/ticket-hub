/*
 * @ (#) ApiResponse.java       1.0     8/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.controller.dto;
/*
 * @author: Luong Tan Dat
 * @date: 8/13/2026
 */

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class ApiResponse<T>{
    private int status;
    private String message;
    private T data;
    private long timestamp;
}

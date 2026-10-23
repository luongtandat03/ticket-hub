/*
 * @ (#) ChatMessageRequest.java       1.0     10/16/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.controller.dto.request;
/*
 * @author: Luong Tan Dat
 * @date: 10/16/2026
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ChatMessageRequest {

    @NotBlank
    @Size(max = 2000)
    private String message;
}

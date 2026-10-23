/*
 * @ (#) AiChatMessageResponse.java       1.0     10/19/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.dto.response;
/*
 * @author: Luong Tan Dat
 * @date: 10/19/2026
 */

import java.time.LocalDateTime;

public record AiChatMessageResponse(String role, String content, LocalDateTime createdAt){
}

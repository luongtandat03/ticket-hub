/*
 * @ (#) EmailMQMessage.java       1.0     9/23/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.mq.email;
/*
 * @author: Luong Tan Dat
 * @date: 9/23/2026
 */

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailMQMessage {
    private String token;
    private String messageId;
    private String eventType;
    private String recipient;
    private String content;
    private Map<String, Object> data;
    private long timestamp;
}

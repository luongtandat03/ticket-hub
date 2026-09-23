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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailMQMessage {
    private String template;
    private String recipient;
}

/*
 * @ (#) TicketResponse.java       1.0     9/15/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.dto;
/*
 * @author: Luong Tan Dat
 * @date: 9/15/2026
 */

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {
    private String id;
    private String name;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

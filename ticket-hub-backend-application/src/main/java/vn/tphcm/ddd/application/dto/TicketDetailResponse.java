/*
 * @ (#) TicketDetailResponse.java       1.0     8/20/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.dto;
/*
 * @author: Luong Tan Dat
 * @date: 8/20/2026
 */

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDetailResponse {
    private String id;
    private String name;
    private String description;
    private int stockInitial;
    private int stockAvailable;
    private boolean stockPrepared;
    private BigDecimal priceOriginal;
    private BigDecimal priceFlash;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;
    private int status;
    private String activityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}

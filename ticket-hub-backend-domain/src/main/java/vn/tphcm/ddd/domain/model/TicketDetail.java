/*
 * @ (#) TicketDetail.java       1.0     8/6/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.model;
/*
 * @author: Luong Tan Dat
 * @date: 8/6/2026
 */

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tbl_ticket_item")
public class TicketDetail extends AbstractEntity<String> {
    private String name;

    private String description;

    private int stockInitial;

    private int stockAvailable;

    private boolean isStockPrepared;

    private BigDecimal priceOriginal;

    private BigDecimal priceFlash;

    private LocalDateTime saleStartTime;

    private LocalDateTime saleEndTime;

    private int status;

    private String activityId;
}

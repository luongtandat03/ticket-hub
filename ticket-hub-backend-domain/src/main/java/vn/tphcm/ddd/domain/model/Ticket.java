/*
 * @ (#) Ticket.java       1.0     8/6/2026
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

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tbl_ticket")
public class Ticket extends AbstractEntity<String>{
    private String name;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private int status;
}

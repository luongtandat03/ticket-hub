/*
 * @ (#) TicketListResponse.java       1.0     10/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.dto.response;
/*
 * @author: Luong Tan Dat
 * @date: 10/13/2026
 */

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketListResponse {
    private List<TicketResponse> ticketList;
    private Long version;
}

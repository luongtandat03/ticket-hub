/*
 * @ (#) TicketMapper.java       1.0     9/15/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.mapper;

/*
 * @author: Luong Tan Dat
 * @date: 9/15/2026
 */

import org.mapstruct.Mapper;
import vn.tphcm.ddd.application.dto.TicketResponse;
import vn.tphcm.ddd.domain.model.Ticket;

@Mapper(componentModel = "spring")
public interface TicketMapper {
    TicketResponse toResponse(Ticket ticket);
}

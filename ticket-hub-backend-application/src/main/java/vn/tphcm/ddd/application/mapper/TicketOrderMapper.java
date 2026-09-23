/*
 * @ (#) TicketOrderMapper.java       1.0     9/7/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.mapper;

/*
 * @author: Luong Tan Dat
 * @date: 9/7/2026
 */

import org.mapstruct.Mapper;
import vn.tphcm.ddd.application.dto.TicketOrderResponse;
import vn.tphcm.ddd.domain.model.TicketOrder;

@Mapper(componentModel = "spring")
public interface TicketOrderMapper {
    TicketOrderResponse toResponse(TicketOrder ticketOrder);
}

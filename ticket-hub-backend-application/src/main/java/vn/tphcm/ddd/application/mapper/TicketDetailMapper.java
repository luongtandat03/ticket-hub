/*
 * @ (#) TicketDetailMapper.java       1.0     8/20/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.mapper;
/*
 * @author: Luong Tan Dat
 * @date: 8/20/2026
 */

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.tphcm.ddd.application.dto.TicketDetailResponse;
import vn.tphcm.ddd.domain.model.TicketDetail;

@Mapper(componentModel = "spring")
public interface TicketDetailMapper {
    @Mapping(target = "version", ignore = true)
    TicketDetailResponse toDto(TicketDetail ticketDetail);
}

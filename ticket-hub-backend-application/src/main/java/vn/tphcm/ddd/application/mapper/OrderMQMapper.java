/*
 * @ (#) OrderMQMapper.java       1.0     9/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.mapper;
/*
 * @author: Luong Tan Dat
 * @date: 9/13/2026
 */

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import vn.tphcm.ddd.application.dto.PlaceOrderResponse;
import vn.tphcm.ddd.domain.model.OrderQueue;

@Mapper(componentModel = "spring")
public interface OrderMQMapper {
}

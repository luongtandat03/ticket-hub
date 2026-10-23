/*
 * @ (#) PaymentMapper.java       1.0     10/12/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.mapper;

/*
 * @author: Luong Tan Dat
 * @date: 10/12/2026
 */

import org.mapstruct.Mapper;
import vn.tphcm.ddd.application.dto.response.PaymentResponse;
import vn.tphcm.ddd.domain.model.PaymentTransaction;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponse toDto(PaymentTransaction paymentTransaction);
}

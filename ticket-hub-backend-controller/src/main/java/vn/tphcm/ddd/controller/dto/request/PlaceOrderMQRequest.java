/*
 * @ (#) PlaceOrderMQRequest.java       1.0     9/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.controller.dto.request;
/*
 * @author: Luong Tan Dat
 * @date: 9/13/2026
 */

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderMQRequest {

    @NotNull
    private String ticketId;

    @Min(1)
    private int quantity;
}

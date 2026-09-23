/*
 * @ (#) TicketDetailDomainService.java       1.0     8/6/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.service;

/*
 * @author: Luong Tan Dat
 * @date: 8/6/2026
 */


import vn.tphcm.ddd.domain.model.TicketDetail;

public interface TicketDetailDomainService {
    TicketDetail getTicketDefaultCacheVip(String id);
}

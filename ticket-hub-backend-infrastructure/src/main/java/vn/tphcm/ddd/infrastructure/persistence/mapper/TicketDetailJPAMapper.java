/*
 * @ (#) TicketDetailJPAMapper.java       1.0     9/4/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.tphcm.ddd.domain.model.TicketDetail;

/*
 * @author: Luong Tan Dat
 * @date: 9/4/2026
 */

public interface TicketDetailJPAMapper extends JpaRepository<TicketDetail, String> {
}

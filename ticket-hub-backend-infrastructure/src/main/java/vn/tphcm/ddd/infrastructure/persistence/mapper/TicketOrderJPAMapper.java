/*
 * @ (#) TicketOrderJPAMapper.java       1.0     9/4/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.mapper;

/*
 * @author: Luong Tan Dat
 * @date: 9/4/2026
 */

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.tphcm.ddd.domain.model.TicketDetail;

public interface TicketOrderJPAMapper extends JpaRepository<TicketDetail, String> {

    @Modifying
    @Transactional
    @Query("UPDATE TicketDetail td SET td.updatedAt = CURRENT_TIMESTAMP, " +
            "td.stockAvailable = td.stockAvailable - :quantity " +
            "WHERE td.id = :ticketId AND td.stockAvailable >= :quantity"
    )
    int decreaseStock(@Param("ticketId") String ticketId, @Param("quantity") int quantity);


    @Modifying
    @Transactional
    @Query("UPDATE TicketDetail td SET td.updatedAt = CURRENT_TIMESTAMP, " +
            "td.stockAvailable = :oldStockAvailable - :quantity " +
            "WHERE td.id = :ticketId AND td.stockAvailable = :oldStockAvailable")
    int decreaseStockCAS(@Param("ticketId") String ticketId, @Param("oldStockAvailable") int oldStockAvailable, @Param("quantity") int quantity);

    @Modifying
    @Transactional
    @Query("UPDATE TicketDetail td SET td.updatedAt = CURRENT_TIMESTAMP, " +
            "td.stockAvailable = td.stockAvailable + :quantity " +
            "WHERE td.id = :ticketId")
    int increaseStock(@Param("ticketId") String ticketId, @Param("quantity") int quantity);
}

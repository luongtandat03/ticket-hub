/*
 * @ (#) OrderDeductionInfraRepository.java       1.0     9/4/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.repository;
/*
 * @author: Luong Tan Dat
 * @date: 9/4/2026
 */

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import vn.tphcm.ddd.domain.model.TicketOrder;
import vn.tphcm.ddd.domain.repository.OrderDeductionRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@RequiredArgsConstructor
@Slf4j(topic = "ORDER-DEDUCTION-INFRASTRUCTURE")
public class OrderDeductionInfraRepositoryImpl implements OrderDeductionRepository {
    private static final String TABLE_PREFIX = "tbl_ticket_order_";
    private final EntityManager entityManager;

    @Override
    public void insertOrder(String yearMonth, TicketOrder ticketOrder) {
        ensureTableExists(yearMonth);

        String tableName = genTableName(yearMonth);

        String sql = "INSERT INTO " + tableName + " (id, user_id, ticket_id, order_number, quantity, order_status, total_amount, terminal_id, order_date, order_notes, updated_at, created_at) " +
                "VALUES (:id, :userId, :ticketId, :orderNumber, :quantity, :orderStatus, :totalAmount, :terminalId, :orderDate, :orderNotes, :updatedAt, :createdAt)";

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

        ticketOrder.setId(UuidCreator.getTimeOrderedEpoch().toString());
        ticketOrder.setOrderDate(now);
        ticketOrder.setCreatedAt(now);
        ticketOrder.setUpdatedAt(now);

        entityManager.createNativeQuery(sql)
                .setParameter("id", ticketOrder.getId())
                .setParameter("userId", ticketOrder.getUserId())
                .setParameter("ticketId", ticketOrder.getTicketId())
                .setParameter("orderNumber", ticketOrder.getOrderNumber())
                .setParameter("quantity", ticketOrder.getQuantity())
                .setParameter("orderStatus", ticketOrder.getOrderStatus())
                .setParameter("totalAmount", ticketOrder.getTotalAmount())
                .setParameter("terminalId", ticketOrder.getTerminalId())
                .setParameter("orderDate", ticketOrder.getOrderDate())
                .setParameter("orderNotes", ticketOrder.getOrderNotes())
                .setParameter("updatedAt", ticketOrder.getUpdatedAt())
                .setParameter("createdAt", ticketOrder.getCreatedAt())
                .executeUpdate();
    }

    @Override
    public List<Object[]> findAll(String yearMonth) {
        String tableName = genTableName(yearMonth);
        String sql = "SELECT * FROM " + tableName;
        return entityManager.createNativeQuery(sql).getResultList();
    }

    @Override
    public Object[] findByOrderNumber(String nTable, String orderNumber) {
        String tableName = genTableName(nTable);
        String sql = "SELECT * FROM " + tableName + " WHERE order_number = :orderNumber";
        List<Object[]> result = entityManager.createNativeQuery(sql).setParameter("orderNumber", orderNumber).getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public List<Object[]> findByDateRange(String yearMonth, LocalDateTime startDate, LocalDateTime endDate) {
        String tableName = genTableName(yearMonth);
        String sql = "SELECT * FROM" + tableName + " WHERE order_date = :startDate AND order_date = :endDate";
        return entityManager.createNativeQuery(sql)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();

    }

    @Override
    public List<Object[]> findPage(String yearMonth, String lastId, int limit) {
        String tableName = genTableName(yearMonth);

        if (lastId == null || lastId.isBlank()) {
            String sql = "SELECT * FROM " + tableName + " ORDER BY id DESC LIMIT :limit";
            return entityManager.createNativeQuery(sql)
                    .setParameter("limit", limit)
                    .getResultList();
        }

        String sql = "SELECT * FROM " + tableName + " WHERE id < :lastId ORDER BY id DESC LIMIT :limit";

        return entityManager.createNativeQuery(sql)
                .setParameter("lastId", lastId)
                .setParameter("limit", limit)
                .getResultList();
    }

    @Override
    public boolean updateOrderStatus(String yearMonth, String orderNumber, int orderStatus) {
        String tableName = genTableName(yearMonth);

        String sql = "UPDATE " + tableName + " SET order_status = :orderStatus, updated_at = :updatedAt WHERE order_number = :orderNumber";

        int result = entityManager.createNativeQuery(sql)
                .setParameter("orderStatus", orderStatus)
                .setParameter("updatedAt", LocalDateTime.now(ZoneId.systemDefault()))
                .setParameter("orderNumber", orderNumber)
                .executeUpdate();
        return result > 0;
    }

    private String genOrderNumber(String userId) {
        return "OKS-SGN" + "-" + System.currentTimeMillis() + "-" + userId.replace("-", "");
    }

    private String genTableName(String yearMonth) {
        return TABLE_PREFIX + yearMonth;
    }


    private static final String CREATE_TABLE_TEMPLATE =
            "CREATE TABLE IF NOT EXISTS `%s` ( " +
                    "id VARCHAR(36) NOT NULL COMMENT 'Primary Key', " +
                    "user_id VARCHAR(36) NOT NULL COMMENT 'User Id', " +
                    "ticket_id VARCHAR(36) NOT NULL, " +
                    "order_number VARCHAR(54) NOT NULL COMMENT 'Order Number', " +
                    "quantity INT NOT NULL DEFAULT 1, " +
                    "order_status INT NOT NULL DEFAULT 0 COMMENT '0:Order, 1: Pending, 2: Cancel, 3: Complete', " +
                    "total_amount DECIMAL(15, 2) NOT NULL COMMENT 'Total Amount', " +
                    "terminal_id VARCHAR(36) NOT NULL COMMENT 'Terminal Id', " +
                    "order_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Order Date', " +
                    "order_notes VARCHAR(50) NOT NULL DEFAULT 'None' COMMENT 'Notes for order', " +
                    "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Timestamp of the last update', " +
                    "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp', " +
                    "PRIMARY KEY (id), " +
                    "UNIQUE KEY uk_order_number (order_number), " +
                    "KEY idx_user_id (user_id), " +
                    "KEY idx_ticket_id (ticket_id), " +
                    "KEY idx_order_date (order_date) " +
                    " ) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci; ";

    private static final Map<String, Boolean> totalCreatedCache = new ConcurrentHashMap<>();

    private void ensureTableExists(String yearMonth) {
        String tableName = genTableName(yearMonth);

        if (totalCreatedCache.containsKey(tableName)) {
            return;
        }

        synchronized (totalCreatedCache) {
            if (totalCreatedCache.containsKey(tableName)) {
                return;
            }

            log.info("Checking and creating table {}", tableName);

            try {
                String sql = String.format(CREATE_TABLE_TEMPLATE, tableName);
                entityManager.createNativeQuery(sql).executeUpdate();

                totalCreatedCache.put(tableName, true);
            } catch (Exception e) {
                log.error("Error creating table {}", tableName, e);
            }
        }
    }
}

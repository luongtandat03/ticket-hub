package vn.tphcm.ddd.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import vn.tphcm.ddd.infrastructure.cache.redis.impl.RedisInfrastructureServiceImpl;
import vn.tphcm.ddd.infrastructure.persistence.repository.OrderDeductionInfraRepositoryImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Kiểm tra câu SQL và lệnh Redis mà tính năng huỷ đơn tự động phụ thuộc vào,
 * không cần MySQL hay Redis thật.
 */
@SuppressWarnings("unchecked")
class OrderTimeoutInfrastructureTest {

    // ---------------- OrderDeductionInfraRepositoryImpl ----------------

    private EntityManager entityManagerReturning(Query query) {
        EntityManager em = mock(EntityManager.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        return em;
    }

    @Test
    void compareAndSet_buildsValidConditionalUpdate() {
        Query query = mock(Query.class);
        when(query.executeUpdate()).thenReturn(1);
        EntityManager em = entityManagerReturning(query);

        boolean updated = new OrderDeductionInfraRepositoryImpl(em).compareAndSetOrderStatus("202610", "OKS-1", 0, 2);

        assertTrue(updated);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(em).createNativeQuery(sql.capture());
        assertTrue(sql.getValue().startsWith("UPDATE tbl_ticket_order_202610 "),
                "Câu SQL bị dính chữ, MySQL sẽ báo lỗi cú pháp: " + sql.getValue());
        assertTrue(sql.getValue().contains("order_status = :expectedStatus"),
                "Thiếu điều kiện trạng thái thì không còn là compare-and-set");

        verify(query).setParameter("expectedStatus", 0);
        verify(query).setParameter("newStatus", 2);
        verify(query).setParameter("orderNumber", "OKS-1");
    }

    @Test
    void compareAndSet_noRowChanged_returnsFalse() {
        Query query = mock(Query.class);
        when(query.executeUpdate()).thenReturn(0);

        assertFalse(new OrderDeductionInfraRepositoryImpl(entityManagerReturning(query))
                .compareAndSetOrderStatus("202610", "OKS-1", 0, 2));
    }

    /** EntityManager giả: câu kiểm tra information_schema trả tableCount, câu SELECT đơn trả orders. */
    private EntityManager entityManagerForScan(long tableCount, Query ordersQuery) {
        EntityManager em = mock(EntityManager.class);
        Query existsQuery = mock(Query.class);
        when(existsQuery.setParameter(anyString(), any())).thenReturn(existsQuery);
        when(existsQuery.getSingleResult()).thenReturn(tableCount);
        when(ordersQuery.setParameter(anyString(), any())).thenReturn(ordersQuery);
        when(em.createNativeQuery(anyString())).thenAnswer(inv ->
                inv.<String>getArgument(0).contains("information_schema") ? existsQuery : ordersQuery);
        return em;
    }

    private String ordersSql(EntityManager em) {
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(em, atLeastOnce()).createNativeQuery(sql.capture());
        return sql.getAllValues().stream()
                .filter(q -> q.contains("SELECT order_number"))
                .findFirst()
                .orElse(null);
    }

    @Test
    void findExpiredPending_filtersPendingOlderThanCutoffOldestFirst() {
        Query query = mock(Query.class);
        when(query.getResultList()).thenReturn(List.of("OKS-1"));
        EntityManager em = entityManagerForScan(1L, query);
        LocalDateTime cutoff = LocalDateTime.of(2026, 10, 14, 10, 0);

        List<String> result = new OrderDeductionInfraRepositoryImpl(em).findExpiredPendingOrderNumbers("202610", cutoff, 50);

        assertEquals(List.of("OKS-1"), result);

        String s = ordersSql(em);
        assertNotNull(s);
        assertTrue(s.contains("FROM tbl_ticket_order_202610 "));
        assertTrue(s.contains("order_status = 0"));
        assertTrue(s.contains("created_at < :createdBefore"));
        // Đơn chờ lâu nhất phải được xử lý trước, nếu không đơn cũ có thể bị bỏ đói
        assertTrue(s.matches("(?s).*ORDER BY created_at( ASC)? LIMIT.*"),
                "Nên sắp xếp tăng dần theo created_at: " + s);

        verify(query).setParameter("createdBefore", cutoff);
        verify(query).setParameter("limit", 50);
    }

    @Test
    void findExpiredPending_missingMonthTable_returnsEmptyWithoutQueryingIt() {
        // Job dự phòng luôn quét cả bảng tháng trước; bảng chưa có thì không được bắn lỗi SQL 1146
        Query query = mock(Query.class);
        EntityManager em = entityManagerForScan(0L, query);

        List<String> result = new OrderDeductionInfraRepositoryImpl(em)
                .findExpiredPendingOrderNumbers("202609", LocalDateTime.now(), 50);

        assertTrue(result.isEmpty());
        assertNull(ordersSql(em), "Không được chạy SELECT trên bảng không tồn tại");
        verify(query, never()).getResultList();
    }

    @Test
    void findExpiredPending_checksTheRightTableName() {
        Query query = mock(Query.class);
        when(query.getResultList()).thenReturn(List.of());
        EntityManager em = entityManagerForScan(1L, query);

        new OrderDeductionInfraRepositoryImpl(em).findExpiredPendingOrderNumbers("202609", LocalDateTime.now(), 50);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(em, atLeastOnce()).createNativeQuery(sql.capture());
        assertTrue(sql.getAllValues().stream().anyMatch(q -> q.contains("information_schema")),
                "Phải kiểm tra bảng tồn tại trước khi quét");
    }

    // ---------------- RedisInfrastructureServiceImpl (ZSET) ----------------

    private final RedisTemplate<String, Object> objectTemplate = mock(RedisTemplate.class);
    private final StringRedisTemplate stringTemplate = mock(StringRedisTemplate.class);

    private RedisInfrastructureServiceImpl redisWith(ZSetOperations<String, String> zset) {
        when(stringTemplate.opsForZSet()).thenReturn(zset);
        return new RedisInfrastructureServiceImpl(objectTemplate, stringTemplate, new ObjectMapper());
    }

    @Test
    void zAdd_storesMemberAsPlainStringWithExpiryAsScoreAndNoKeyTtl() {
        ZSetOperations<String, String> zset = mock(ZSetOperations.class);
        RedisInfrastructureServiceImpl service = redisWith(zset);

        service.zAdd("order:cancel", "{\"orderNumber\":\"OKS-1\"}", 1_760_000_000_000L);

        verify(zset).add("order:cancel", "{\"orderNumber\":\"OKS-1\"}", 1_760_000_000_000L);
        verify(stringTemplate, never()).expire(anyString(), anyLong(), any());
        verify(objectTemplate, never()).expire(anyString(), anyLong(), any());
        // Không dùng RedisTemplate<String,Object>: nó lưu kèm @class và đọc ra object, làm ZREM không bao giờ khớp
        verify(objectTemplate, never()).opsForZSet();
    }

    @Test
    void zRangeByScore_returnsExactStoredStringsSoTheyCanBeRemoved() {
        ZSetOperations<String, String> zset = mock(ZSetOperations.class);
        String member = "{\"orderNumber\":\"OKS-1\"}";
        when(zset.rangeByScore("order:cancel", 0, 100, 0, 10)).thenReturn(Set.of(member));
        when(zset.remove("order:cancel", member)).thenReturn(1L);
        RedisInfrastructureServiceImpl service = redisWith(zset);

        Set<String> expired = service.zRangeByScore("order:cancel", 0, 100, 10);

        assertEquals(Set.of(member), expired);
        assertTrue(service.zRemove("order:cancel", expired.iterator().next()));
    }

    @Test
    void zRangeByScore_nullOrError_returnsEmpty() {
        ZSetOperations<String, String> zset = mock(ZSetOperations.class);
        when(zset.rangeByScore(anyString(), anyDouble(), anyDouble(), anyLong(), anyLong())).thenReturn(null);
        assertTrue(redisWith(zset).zRangeByScore("order:cancel", 0, 100, 10).isEmpty());

        when(zset.rangeByScore(anyString(), anyDouble(), anyDouble(), anyLong(), anyLong()))
                .thenThrow(new IllegalStateException("redis down"));
        assertTrue(redisWith(zset).zRangeByScore("order:cancel", 0, 100, 10).isEmpty());
    }

    @Test
    void zRemove_removesOnlyThatMemberAndReportsClaim() {
        ZSetOperations<String, String> zset = mock(ZSetOperations.class);
        when(zset.remove("order:cancel", "member")).thenReturn(1L);
        RedisInfrastructureServiceImpl service = redisWith(zset);

        assertTrue(service.zRemove("order:cancel", "member"));

        verify(stringTemplate, never()).delete(anyString());
        verify(objectTemplate, never()).delete(anyString());
    }

    @Test
    void zRemove_memberAlreadyGone_returnsFalse() {
        ZSetOperations<String, String> zset = mock(ZSetOperations.class);
        when(zset.remove("order:cancel", "member")).thenReturn(0L);

        assertFalse(redisWith(zset).zRemove("order:cancel", "member"));
    }

    @Test
    void zRemove_redisError_returnsFalse() {
        ZSetOperations<String, String> zset = mock(ZSetOperations.class);
        when(zset.remove(anyString(), any())).thenThrow(new IllegalStateException("redis down"));

        assertFalse(redisWith(zset).zRemove("order:cancel", "member"));
    }
}

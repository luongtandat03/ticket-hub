/*
 * @ (#) OrderCancelScheduleService.java       1.0     9/19/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.schedule;

/*
 * @author: Luong Tan Dat
 * @date: 9/19/2026
 */


import com.alibaba.fastjson.JSON;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.infrastructure.cache.redis.RedisInfrastructureService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j(topic = "ORDER-CANCEL-SCHEDULE")
@RequiredArgsConstructor
public class OrderCancelSchedule {
    private static final String ZSET_KEY = "order:cancel";

    public static final long PAYMENT_TIMEOUT_MINUTES = 5;

    private final RedisInfrastructureService redisInfrastructureService;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeoutPayload {
        private String orderNumber;
        private String yearMonth;
        private String ticketId;
        private Integer quantity;
    }

    public List<String> pollExpired(int limit) {
        Set<String> member = redisInfrastructureService.zRangeByScore(ZSET_KEY, 0, System.currentTimeMillis(), limit);
        return new ArrayList<>(member);
    }

    public void scheduleTimeout(String orderNumber, String yearMonth, String ticketId, int quantity) {
        TimeoutPayload timeoutPayload = new TimeoutPayload(orderNumber, yearMonth, ticketId, quantity);
        long expiredAt = System.currentTimeMillis() + PAYMENT_TIMEOUT_MINUTES * 60000L;
        redisInfrastructureService.zAdd(ZSET_KEY, timeoutPayload, expiredAt);
        log.info("[CANCEL-SCHEDULE] ZADD order:cancel orderNumber={}, expiredAt={}", orderNumber, expiredAt);
    }

    public TimeoutPayload parse(String rawMember) {
        return JSON.parseObject(rawMember, TimeoutPayload.class);
    }

    public void remove(String rawMember) {
        redisInfrastructureService.zRemove(ZSET_KEY, rawMember);
    }
}

/*
 * @ (#) StockRedisTccService.java       1.0     9/11/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.order.cache;
/*
 * @author: Luong Tan Dat
 * @date: 9/11/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.application.port.TccContext;
import vn.tphcm.ddd.application.port.TccException;
import vn.tphcm.ddd.application.port.TccParticipant;
import vn.tphcm.ddd.infrastructure.cache.redis.RedisInfrastructureService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "STOCK-REDIS")
public class StockRedisTccService implements TccParticipant {
    private final RedisInfrastructureService redisInfrastructureService;
    private static final long RESERVATION_TTL_SECONDS = 300;

    private static final String LUA_TRY =
            "local stock = tonumber(redis.call('GET', KEYS[1]) or '0'); " +
                    "local marker = redis.call('HGET', KEYS[2], 'status'); " +
                    "if (marker) then return 1; end; " +
                    "if (stock < tonumber(ARGV[1])) then return 0; end; " +
                    "redis.call('DECRBY', KEYS[1], ARGV[1]); " +
                    "redis.call('HSET', KEYS[2], 'status', 'RESERVED', 'qty', ARGV[1]); " +
                    "redis.call('EXPIRE', KEYS[2], ARGV[2]); " +
                    "return 1;";

    private static final String LUA_CONFIRM =
            "local status = redis.call('HGET', KEYS[1], 'status'); " +
                    "if (not status) then return 1; end; " +
                    "if (status == 'CONFIRMED') then return 1; end; " +
                    "if (status == 'CANCELLED') then return 0; end; " +
                    "redis.call('DEL', KEYS[1]); " +
                    "return 1;";

    private static final String LUA_CANCEL =
            "local status = redis.call('HGET', KEYS[2], 'status'); " +
                    "if (not status) then return 1; end; " +
                    "if (status == 'CANCELLED') then return 1; end; " +
                    "if (status == 'CONFIRMED') then return 0; end; " +
                    "local qty = tonumber(redis.call('HGET', KEYS[2], 'qty')); " +
                    "redis.call('INCRBY', KEYS[1], qty); " +
                    "redis.call('DEL', KEYS[2]); " +
                    "return 1;";

    private static final DefaultRedisScript<Long> SCRIPT_TRY = new
            DefaultRedisScript<>(LUA_TRY, Long.class);

    private static final DefaultRedisScript<Long> SCRIPT_CONFIRM = new
            DefaultRedisScript<>(LUA_CONFIRM, Long.class);

    private static final DefaultRedisScript<Long> SCRIPT_CANCEL = new
            DefaultRedisScript<>(LUA_CANCEL, Long.class);

    @Override
    public String tryPhase(TccContext context) {
        String ticketId = (String) context.getData().get("ticketId");
        int quantity = (Integer) context.getData().get("quantity");

        Long result = redisInfrastructureService.redisTemplate().execute(
                SCRIPT_TRY,
                List.of(genKeyStockItemCache(ticketId), genKeyReservation(context.getTxId())),
                String.valueOf(quantity),
                String.valueOf(RESERVATION_TTL_SECONDS)
        );

        if (result != 1) {
            throw new TccException("Havent stock redis for ticket: " + ticketId);
        }
        return "RESERVED";
    }

    @Override
    public void confirm(TccContext context) {
        Long result = redisInfrastructureService.redisTemplate().execute(
                SCRIPT_CONFIRM,
                List.of(genKeyReservation(context.getTxId()))
        );

        if (result != 1) {
            log.error("TCC Confirm error for ticket: {}", context.getTxId());
        }
    }

    @Override
    public void cancel(TccContext context) {
        String ticketId = (String) context.getData().get("ticketId");
        Long result = redisInfrastructureService.redisTemplate().execute(
                SCRIPT_CANCEL,
                List.of(genKeyStockItemCache(ticketId), genKeyReservation(context.getTxId()))
        );

        if (result != 1L) {
            log.error("TCC Cancel error tx={}: marker could be CONFIRMED", context.getTxId());
        }
    }

    private String genKeyReservation(String txId) {
        return "TCC:RESERVATION:" + txId;
    }

    private String genKeyStockItemCache(String ticketId) {
        return "TICKET:STOCK:" + ticketId;
    }
}

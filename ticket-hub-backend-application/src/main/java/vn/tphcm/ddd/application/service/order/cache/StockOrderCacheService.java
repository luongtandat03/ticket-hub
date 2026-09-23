/*
 * @ (#) StockOrderCacheService.java       1.0     9/4/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.order.cache;
/*
 * @author: Luong Tan Dat
 * @date: 9/4/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.application.dto.TicketDetailResponse;
import vn.tphcm.ddd.application.service.ticket.cache.TicketDetailCacheService;
import vn.tphcm.ddd.infrastructure.cache.redis.RedisInfrastructureService;

import java.math.BigDecimal;
import java.util.Collections;

@Service
@Slf4j(topic = "STOCK-ORDER-CACHE-SERVICE")
@RequiredArgsConstructor
public class StockOrderCacheService {
    private final TicketDetailCacheService ticketDetailCacheService;

    private final RedisInfrastructureService redisInfrastructureService;

    private static final String LUA_DEDUCT =
            "local stock = tonumber(redis.call('GET', KEYS[1])); " +
                    "if (stock >= tonumber(ARGV[1])) then " +
                    " redis.call('SET', KEYS[1], stock - tonumber(ARGV[1])); " +
                    " return stock; " +
                    "end; " +
                    " return 0; ";
    private static final String LUA_RESTORE =
            "local stock = redis.call('GET', KEYS[1]); " +
                    "if (stock) then " +
                    "   redis.call('SET', KEYS[1], tonumber(stock) + tonumber(ARGV[1])); " +
                    "   return 1; " +
                    "end; " +
                    "return 0;";

    private static final DefaultRedisScript<Long> SCRIPT_DEDUCT =
            new DefaultRedisScript<>(LUA_DEDUCT, Long.class);

    private static final DefaultRedisScript<Long> SCRIPT_RESTORE =
            new DefaultRedisScript<>(LUA_RESTORE, Long.class);

    public boolean addStockAvailableToCache(String ticketId) {
        if (ticketId == null) {
            return false;
        }

        TicketDetailResponse response = ticketDetailCacheService.getTicketDetail(ticketId, null);

        if (response == null) {
            return false;
        }

        String keyStockCache = genKeyStockItemCache(ticketId);

        log.info("get -> getKeyStockCache: {}", keyStockCache);

        redisInfrastructureService.setObject(keyStockCache, response.getStockAvailable());

        return true;
    }

    public int decreaseStockCacheByLUA(String ticketId, Integer quantity) {
        String keyStockLUA = genKeyStockItemCache(ticketId);

        Long result = redisInfrastructureService.redisTemplate().execute(SCRIPT_DEDUCT, Collections.singletonList(keyStockLUA), quantity);
        log.info("result = {}", result.intValue());

        return result.intValue();
    }

    public int decreaseStockCacheByLua(String ticketId, Integer quantity) {
        String keyStockLUA = genKeyStockItemCache(ticketId);

        String resultLua =
                "local stock = redis.call('GET', KEYS[1]); " +
                        "if stock == false then return -1 end; " +
                        "stock = tonumber(stock); " +
                        "if (stock >= tonumber(ARGV[1])) then " +
                        "   redis.call('SET', KEYS[1], stock - tonumber(ARGV[1])); " +
                        "   return 1; " +
                        "end; " +
                        "return 0; ";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(resultLua, Long.class);
        Long result = redisInfrastructureService.redisTemplate().execute(redisScript, Collections.singletonList(keyStockLUA), quantity);
        return result != null ? result.intValue() : -1;

    }

    public boolean increaseStockCacheByLUA(String ticketId, int quantity) {
        String key = genKeyStockItemCache(ticketId);

        Long result = redisInfrastructureService
                .redisTemplate()
                .execute(SCRIPT_RESTORE,
                        Collections.singletonList(key),
                        quantity);
        return result != null && result.intValue() == 1;
    }

    public long getEffectivePrice(String ticketId) {
        TicketDetailResponse response = ticketDetailCacheService.getTicketDetail(ticketId, null);
        if (response == null) {
            return -1L;
        }

        BigDecimal flash = response.getPriceFlash();
        BigDecimal priceOriginal = response.getPriceOriginal();

        if (flash != null && flash.compareTo(BigDecimal.ZERO) > 0) {
            return flash.longValue();
        }

        return priceOriginal != null ? priceOriginal.longValue() : -1L;
    }

    private String genKeyStockItemCache(String ticketId) {
        return "TICKET:" + ticketId + ":STOCK";
    }
}

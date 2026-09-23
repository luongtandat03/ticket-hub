/*
 * @ (#) WarmupDataBeforeEvent.java       1.0     9/7/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.cronjob;
/*
 * @author: Luong Tan Dat
 * @date: 9/7/2026
 */

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.tphcm.ddd.application.service.order.cache.StockOrderCacheService;

@Component
@Slf4j(topic = "WARM-UP")
@RequiredArgsConstructor
public class WarmupDataBeforeEvent {
    private final StockOrderCacheService stockOrderCacheService;

    private static final String TICKET_ID = "3f2504e0-4f89-41d3-9a0c-0305e82c3301";

    @PostConstruct
    public void loadDataTicketOnce() {
        log.info("Load ticket item Once... warmup..| {}", System.currentTimeMillis());
        stockOrderCacheService.addStockAvailableToCache(TICKET_ID);
    }
}

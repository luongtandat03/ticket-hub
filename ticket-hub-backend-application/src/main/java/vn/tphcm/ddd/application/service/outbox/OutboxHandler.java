/*
 * @ (#) OutboxHandler.java       1.0     10/3/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.outbox;

import vn.tphcm.ddd.domain.model.OutboxEvent;

/*
 * @author: Luong Tan Dat
 * @date: 10/3/2026
 */
public interface OutboxHandler {
    String eventType();
    void publish(OutboxEvent event);
}

/*
 * @ (#) OrderTimeoutWorker.java       1.0     9/18/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.cronjob;
/*
 * @author: Luong Tan Dat
 * @date: 9/18/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.tphcm.ddd.application.schedule.OrderCancelSchedule;
import vn.tphcm.ddd.infrastructure.mq.order.KafkaOrderProducer;

@Component
@Slf4j(topic = "ORDER-TIMEOUT")
@RequiredArgsConstructor
public class OrderTimeoutWorker {

    private static final int BATCH_SIZE = 100;

    private final OrderCancelSchedule orderCancelSchedule;

    private final KafkaOrderProducer kafkaOrderProducer;

//    @Scheduled(fixedRate = 3000)
//    public void pollAndDispatch() {
//        List<String> expiredMembers = orderCancelSchedule.pollExpired(BATCH_SIZE);
//
//        if (expiredMembers.isEmpty()) {
//            return;
//        }
//
//        log.debug("[TIMEOUT-WORKER] found {} expired orders", expiredMembers.size());
//
//        for (String raw : expiredMembers) {
//            try {
//                OrderCancelSchedule.TimeoutPayload payload = orderCancelSchedule.parse(raw);
//
//                OrderCancelMQMessage message = new OrderCancelMQMessage(
//                        payload.getOrderNumber(), payload.getYearMonth(),
//                        payload.getTicketId(), payload.getQuantity(), LocalDateTime.now(ZoneId.systemDefault())
//                );
//
//                kafkaOrderProducer.sendCancelMessage(message);
//                orderCancelSchedule.remove(raw);
//
//                log.info("[TIMEOUT-WORKER] dispathed cancel event orderNumber = {}", payload.getOrderNumber());
//            } catch (Exception e) {
//                log.error("[TIMEOUT-WORKER] failed to send cancel message", e);
//            }
//        }
//    }
}

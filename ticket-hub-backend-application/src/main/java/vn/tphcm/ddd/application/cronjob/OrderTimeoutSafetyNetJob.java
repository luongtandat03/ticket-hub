/*
 * @ (#) OrderTimeoutSafetyNetJob.java       1.0     9/20/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.cronjob;
/*
 * @author: Luong Tan Dat
 * @date: 9/20/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "ORDER-TIMEOUT-SAFETY")
public class OrderTimeoutSafetyNetJob {
    private static final int BATCH_SIZE = 50;
}

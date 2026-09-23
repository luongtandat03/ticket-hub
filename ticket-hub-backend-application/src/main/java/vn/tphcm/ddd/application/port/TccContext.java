/*
 * @ (#) TccContext.java       1.0     9/10/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.port;
/*
 * @author: Luong Tan Dat
 * @date: 9/10/2026
 */

import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class TccContext {
    String txId;
    final Map<String, Object> data = new ConcurrentHashMap<>();
    TccContext(String txId) {
        this.txId = txId;
    }
}

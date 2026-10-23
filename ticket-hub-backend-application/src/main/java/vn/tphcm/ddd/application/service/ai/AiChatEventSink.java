/*
 * @ (#) AiChatService.java       1.0     10/19/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.ai;

/*
 * @author: Luong Tan Dat
 * @date: 10/19/2026
 */
public interface AiChatEventSink {
    void text(String delta);

    void tool(String name, String label);

    void done(String reply);

    void error(String code, String message);
}

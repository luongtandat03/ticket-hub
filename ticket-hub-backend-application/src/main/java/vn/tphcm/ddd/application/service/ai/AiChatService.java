/*
 * @ (#) ChatService.java       1.0     10/16/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.ai;

/*
 * @author: Luong Tan Dat
 * @date: 10/16/2026
 */

import vn.tphcm.ddd.application.dto.response.AiChatMessageResponse;

import java.util.List;

public interface AiChatService {
    void handleUserMessage(String conversationId, String username, String content);

    void chat(String userId, String message, AiChatEventSink sink);

    List<AiChatMessageResponse> getHistory(String userId);

    void clearHistory(String userId);
}

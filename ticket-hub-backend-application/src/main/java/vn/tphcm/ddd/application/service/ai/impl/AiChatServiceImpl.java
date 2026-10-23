/*
 * @ (#) AiChatServiceImpl.java       1.0     10/19/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.ai.impl;
/*
 * @author: Luong Tan Dat
 * @date: 10/19/2026
 */

import org.springframework.stereotype.Service;
import vn.tphcm.ddd.application.dto.response.AiChatMessageResponse;
import vn.tphcm.ddd.application.service.ai.AiChatEventSink;
import vn.tphcm.ddd.application.service.ai.AiChatService;

import java.util.List;

@Service
public class AiChatServiceImpl implements AiChatService {
    @Override
    public void handleUserMessage(String conversationId, String username, String content) {

    }

    @Override
    public void chat(String userId, String message, AiChatEventSink sink) {

    }

    @Override
    public List<AiChatMessageResponse> getHistory(String userId) {
        return List.of();
    }

    @Override
    public void clearHistory(String userId) {

    }
}

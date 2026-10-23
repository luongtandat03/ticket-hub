/*
 * @ (#) ChatController.java       1.0     10/16/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.controller.http;
/*
 * @author: Luong Tan Dat
 * @date: 10/16/2026
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.tphcm.ddd.application.dto.response.AiChatMessageResponse;
import vn.tphcm.ddd.application.exception.AiChatClientDisconnectedException;
import vn.tphcm.ddd.application.service.ai.AiChatEventSink;
import vn.tphcm.ddd.application.service.ai.AiChatService;
import vn.tphcm.ddd.controller.dto.ApiResponse;
import vn.tphcm.ddd.controller.dto.request.ChatMessageRequest;
import vn.tphcm.ddd.controller.ratelimit.RateLimit;
import vn.tphcm.ddd.domain.model.User;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/chat")
@Slf4j(topic = "AI-CHAT-CONTROLLER")
public class AiChatController {
    private final AiChatService aiChatService;
    private final ObjectMapper objectMapper;

    @PostMapping("/stream")
    @RateLimit(limit = 10)
    public void stream(@Valid @RequestBody ChatMessageRequest request, HttpServletResponse response) throws IOException {
        String userId = currentUserId();

        log.info("Controller -> AI chat stream | userId: {}", userId);

        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");

        response.setHeader("X-Accel-Buffering", "no");

        try {
            aiChatService.chat(userId, request.getMessage().trim(), new SseSink(response.getWriter(), objectMapper));
        } catch (AiChatClientDisconnectedException e) {
            log.info("AI chat client disconnected, userId={}", userId, e);
        }
    }

    @GetMapping("/history")
    public ApiResponse<List<AiChatMessageResponse>> getHistory() {
        log.info("Controller -> AI chat get history");

        return ApiResponse.<List<AiChatMessageResponse>>builder()
                .status(OK.value())
                .message("Get AI Chat history successfully")
                .data(aiChatService.getHistory(currentUserId()))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @DeleteMapping("/history")
    public ApiResponse<Void> clearHistory() {
        log.info("Controller -> AI chat clear history");

        return ApiResponse.<Void>builder()
                .status(OK.value())
                .message("Clear AI chat history successfully")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }

        throw new AccessDeniedException("Login required");
    }

    private record SseSink(PrintWriter writer, ObjectMapper objectMapper) implements AiChatEventSink {
        @Override
        public void text(String delta) {
            send("delta", new TextEvent(delta));
        }

        @Override
        public void tool(String name, String label) {
            send("tool", new ToolEvent(name, label));
        }

        @Override
        public void done(String reply) {
            send("done", new DoneEvent(reply));
        }

        @Override
        public void error(String code, String message) {
            send("error", new ErrorEvent(code, message));
        }

        private void send(String event, Object data) {
            try {
                writer.write("event: " + event + "\ndata: " + objectMapper.writeValueAsString(data) + "\n\n");
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Cannot serialize SSE event", e);
            }

            writer.flush();

            if (writer.checkError()) {
                throw new AiChatClientDisconnectedException("Ai was disconnected");
            }
        }
    }

    private record TextEvent(String text) {
    }

    private record ToolEvent(String name, String lable) {
    }

    private record DoneEvent(String reply) {
    }

    private record ErrorEvent(String code, String message) {
    }

}

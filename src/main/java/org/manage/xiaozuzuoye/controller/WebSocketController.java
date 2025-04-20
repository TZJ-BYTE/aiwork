package org.manage.xiaozuzuoye.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.manage.xiaozuzuoye.dto.OllamaRequest;
import org.manage.xiaozuzuoye.dto.OllamaResponse;
import org.manage.xiaozuzuoye.service.AIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.web.socket.CloseStatus;
import org.springframework.lang.NonNull;

@Component
public class WebSocketController extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);
    private final AIService aiService;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, ExecutorService> sessionExecutors = new ConcurrentHashMap<>();

    public WebSocketController(AIService aiService, ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        sessionExecutors.put(session.getId(), Executors.newSingleThreadExecutor());
        logger.info("新的WebSocket连接已建立: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        ExecutorService executor = sessionExecutors.get(session.getId());
        if (executor != null) {
            executor.submit(() -> {
                try {
                    // 首先尝试解析为通用JSON对象
                    JsonNode jsonNode = objectMapper.readTree(message.getPayload());
                    
                    // 如果是ping消息，直接返回pong
                    if (jsonNode.has("type") && "ping".equals(jsonNode.get("type").asText())) {
                        session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
                        return;
                    }
                    
                    // 其他消息按OllamaRequest处理
                    OllamaRequest request = objectMapper.readValue(message.getPayload(), OllamaRequest.class);
                    logger.debug("收到消息: {}", message.getPayload());
                    
                    // 确保请求中包含必要的字段
                    if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
                        sendErrorMessage(session, "提示信息不能为空");
                        return;
                    }
                    
                    // 处理AI请求
                    request.setStream(true);  // 确保启用流式响应
                    aiService.streamResponse(session.getId(), request, response -> {
                        try {
                            String jsonResponse = objectMapper.writeValueAsString(response);
                            logger.debug("发送响应: {}", jsonResponse);
                            session.sendMessage(new TextMessage(jsonResponse));
                        } catch (Exception e) {
                            logger.error("发送消息时出错", e);
                            sendErrorMessage(session, "发送消息时出错: " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    logger.error("处理消息时出错", e);
                    sendErrorMessage(session, "处理消息时出错: " + e.getMessage());
                }
            });
        }
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            OllamaResponse errorResponse = new OllamaResponse();
            errorResponse.setResponse(errorMessage);
            errorResponse.setDone(true);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorResponse)));
        } catch (Exception ex) {
            logger.error("发送错误消息时出错", ex);
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        ExecutorService executor = sessionExecutors.remove(session.getId());
        if (executor != null) {
            executor.shutdown();
        }
        // 清理 AI 服务中的会话
        aiService.closeSession(session.getId());
        logger.info("WebSocket连接已关闭: {}", session.getId());
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) {
        logger.error("传输错误 - SessionID: {} | 错误类型: {} | 错误信息: {}", 
            session.getId(),
            exception.getClass().getSimpleName(),
            exception.getMessage());
        
        // 记录详细堆栈
        logger.debug("完整错误堆栈：", exception);
        
        // 关闭问题会话
        if (session.isOpen()) {
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (IOException e) {
                logger.error("关闭会话时发生错误", e);
            }
        }
    }
} 
package org.manage.xiaozuzuoye.config;

import org.manage.xiaozuzuoye.controller.WebSocketController;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);
    @Value("${server.port:4388}")
    private String serverPort;

    @Autowired
    private WebSocketController webSocketController;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册WebSocket处理器
        registry.addHandler(webSocketController, "/chat")
               .setAllowedOriginPatterns("*");
        logger.info("WebSocket服务已启动，监听端口: {}, 路径: /chat", serverPort);
    }
}


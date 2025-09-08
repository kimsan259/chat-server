// src/main/java/com/sankim/chat_server/chat/chat/config/WebSocketHandlerConfig.java
package com.sankim.chat_server.chat.chat.config;

import com.sankim.chat_server.chat.chat.ws.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 순수 WebSocket 핸들러를 /ws-handler 에 등록합니다.
 * Allowed origins에 프런트 포트(5173)를 허용해야 CORS 문제가 없습니다.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketHandlerConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws-handler")
                .addInterceptors(new WebSocketHandshakeInterceptor())
                .setAllowedOriginPatterns("http://localhost:5173", "http://127.0.0.1:5173")
                .withSockJS(); // SockJS를 사용하면 구형 브라우저 지원
    }
}

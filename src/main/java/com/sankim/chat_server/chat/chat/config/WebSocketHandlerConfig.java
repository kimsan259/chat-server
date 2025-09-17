// src/main/java/com/sankim/chat_server/chat/chat/config/WebSocketHandlerConfig.java
package com.sankim.chat_server.chat.chat.config;

import com.sankim.chat_server.chat.chat.ws.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketHandlerConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws-handler")
                .addInterceptors(new WebSocketHandshakeInterceptor()) // userId 전달 인터셉터
                .setAllowedOriginPatterns("http://localhost:5173", "http://127.0.0.1:5173");
        // SockJS가 필요 없다면 .withSockJS()를 제거합니다.
    }
}

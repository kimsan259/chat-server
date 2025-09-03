// src/main/java/com/sankim/chat_server/chat/chat/config/StompWebSocketConfig.java
package com.sankim.chat_server.chat.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라가 구독할 주소 prefix
        config.enableSimpleBroker("/topic");
        // (선택) 클라가 서버로 보낼 때 prefix — 지금 예제에선 convertAndSend만 써도 OK
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // 개발용 프론트 오리진만 허용
                .setAllowedOriginPatterns("http://localhost:5173", "http://127.0.0.1:5173")
                .withSockJS(); // 프론트에서 SockJS("/ws")로 접속
    }
}

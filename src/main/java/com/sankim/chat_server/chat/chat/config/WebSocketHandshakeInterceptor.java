// src/main/java/com/sankim/chat_server/chat/chat/config/WebSocketHandshakeInterceptor.java
package com.sankim.chat_server.chat.chat.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * SockJS 연결 시, 쿼리스트링에서 userId를 읽어 세션 속성에 저장합니다.
 * 예: /ws-handler?userId=1
 */
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String query = request.getURI().getQuery(); // "userId=1"
        if (query != null && query.startsWith("userId=")) {
            attributes.put("userId", query.substring("userId=".length()));
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) { }
}

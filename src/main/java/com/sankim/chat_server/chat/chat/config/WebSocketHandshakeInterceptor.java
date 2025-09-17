// src/main/java/com/sankim/chat_server/chat/chat/config/WebSocketHandshakeInterceptor.java
package com.sankim.chat_server.chat.chat.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

// 쿼리스트링에서 userId를 추출하여 WebSocketSession 속성에 저장
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest req, ServerHttpResponse res,
                                   WebSocketHandler handler, Map<String, Object> attributes) {
        String query = req.getURI().getQuery(); // 예: userId=1
        if (query != null && query.startsWith("userId=")) {
            attributes.put("userId", query.substring("userId=".length()));
        }
        return true;
    }
    @Override
    public void afterHandshake(ServerHttpRequest req, ServerHttpResponse res,
                               WebSocketHandler handler, Exception ex) {}
}

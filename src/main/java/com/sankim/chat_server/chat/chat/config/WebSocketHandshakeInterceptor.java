// src/main/java/com/sankim/chat_server/chat/chat/config/WebSocketHandshakeInterceptor.java
package com.sankim.chat_server.chat.chat.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 소켓 연결 직전의 HTTP 핸드셰이크에서 쿼리스트링 ?userId=... 를 읽어 세션 속성에 저장합니다.
 * 브라우저에서는 WebSocket 헤더를 직접 지정할 수 없으므로 쿼리스트링으로 전달합니다.
 */
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // URI 예: /ws-handler?userId=1
        String query = request.getURI().getQuery();
        if (query != null && query.startsWith("userId=")) {
            attributes.put("userId", query.substring("userId=".length()));
        }
        return true;
    }
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {}
}

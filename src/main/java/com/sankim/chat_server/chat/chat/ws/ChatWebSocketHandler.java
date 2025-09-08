// src/main/java/com/sankim/chat_server/chat/chat/ws/ChatWebSocketHandler.java
package com.sankim.chat_server.chat.chat.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankim.chat_server.chat.chat.api.dto.MessageResponse;
import com.sankim.chat_server.chat.chat.api.dto.SendMessageRequest;
import com.sankim.chat_server.chat.chat.message.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * STOMP 없이 순수 WebSocket을 처리하는 핸들러.
 * - 세션 속성에 있는 userId를 이용해 클라이언트를 식별합니다.
 * - 들어온 메시지를 DB에 저장하고, 같은 방의 다른 세션으로 브로드캐스트합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final MessageService messageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 현재 접속 중인 세션을 userId 기준으로 관리
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // HandshakeInterceptor 에서 넣어준 userId 속성 가져오기
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.put(Long.valueOf(userId), session);
            log.info("웹소켓 연결됨: userId={}", userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 프런트에서 보내온 JSON을 DTO로 변환
        SendMessageRequest req = objectMapper.readValue(message.getPayload(), SendMessageRequest.class);

        // 세션 속성에 저장된 userId를 Long으로 변환
        Long senderId = Long.valueOf((String) session.getAttributes().get("userId"));

        // 메시지를 DB에 저장하고 MessageResponse 응답을 받음
        MessageResponse saved = messageService.sendMessage(senderId, req);

        // 간단한 예: 연결된 모든 세션에 메시지를 전송
        // 실제 서비스에서는 같은 chatId의 세션을 필터링해야 함
        for (WebSocketSession ws : sessions.values()) {
            ws.sendMessage(new TextMessage(objectMapper.writeValueAsString(saved)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.remove(Long.valueOf(userId));
            log.info("웹소켓 연결 종료: userId={}", userId);
        }
    }

    public void broadcastMessage(MessageResponse dto) {
        // 메시지를 모든 접속자에게 전송
        try {
            String json = objectMapper.writeValueAsString(dto);
            TextMessage textMessage = new TextMessage(json);
            for (WebSocketSession ws : sessions.values()) {
                ws.sendMessage(textMessage);
            }
        } catch (Exception ex) {
            log.error("웹소켓 브로드캐스트 실패", ex);
        }
    }
}

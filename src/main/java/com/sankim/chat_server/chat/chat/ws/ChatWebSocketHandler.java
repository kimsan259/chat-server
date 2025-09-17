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
 * - 세션 속성의 userId를 기준으로 사용자 세션을 관리하고,
 * - 들어오는 메시지를 DB에 저장한 후 모든 사용자에게 브로드캐스트합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final MessageService messageService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    // 현재 접속 중인 세션을 userId ↔ WebSocketSession 형태로 저장
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // HandshakeInterceptor에서 userId를 세션 속성에 넣음
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.put(Long.valueOf(userId), session);
            log.info("웹소켓 연결됨: userId={}", userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 브라우저에서 보내온 JSON을 DTO로 역직렬화
        SendMessageRequest req = objectMapper.readValue(message.getPayload(), SendMessageRequest.class);
        // 세션 속성에서 전송자 ID 추출
        Long senderId = Long.valueOf((String) session.getAttributes().get("userId"));
        // 메시지 저장
        MessageResponse saved = messageService.sendMessage(senderId, req);
        // 모든 사용자에게 브로드캐스트
        broadcastMessage(saved);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.remove(Long.valueOf(userId));
            log.info("웹소켓 연결 종료: userId={}", userId);
        }
    }

    /** DB 커밋 후 실시간으로 메시지를 보내기 위한 메서드 */
    public void broadcastMessage(MessageResponse dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            TextMessage tm = new TextMessage(json);
            for (WebSocketSession ws : sessions.values()) {
                ws.sendMessage(tm);
            }
        } catch (Exception e) {
            log.error("브로드캐스트 실패", e);
        }
    }
}

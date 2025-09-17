package com.sankim.chat_server.chat.chat.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sankim.chat_server.chat.chat.api.dto.MessageResponse;
import com.sankim.chat_server.chat.chat.api.dto.SendMessageRequest;
import com.sankim.chat_server.chat.chat.message.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * STOMP를 사용하지 않고 순수 WebSocket/SockJS로 채팅을 처리하는 핸들러.
 * - 세션 속성의 userId를 기준으로 세션을 관리합니다.
 * - 들어온 JSON 메시지를 DB에 저장한 뒤, 커밋 후 브로드캐스트를 이벤트로 처리합니다.
 */
@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    // 현재 접속 중인 세션을 (userId -> WebSocketSession) 형태로 저장
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    public ChatWebSocketHandler(@Lazy MessageService messageService) {
        this.messageService = messageService;
        // Java 8 날짜/시간을 ISO-8601 문자열로 직렬화하기 위한 설정
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // HandshakeInterceptor에서 userId를 속성에 넣어줍니다.
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.put(Long.valueOf(userId), session);
            log.info("웹소켓 연결됨: userId={}", userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 클라이언트에서 전송한 JSON을 SendMessageRequest DTO로 변환
        SendMessageRequest req = objectMapper.readValue(message.getPayload(), SendMessageRequest.class);
        Long senderId = Long.valueOf((String) session.getAttributes().get("userId"));
        // 메시지를 DB에 저장
        messageService.sendMessage(senderId, req);
        // 실제 브로드캐스트는 커밋 후 이벤트 리스너가 처리합니다.
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.remove(Long.valueOf(userId));
            log.info("웹소켓 연결 종료: userId={}", userId);
        }
    }

    /**
     * DB 커밋 후 MessageBroadcastListener가 호출하는 브로드캐스트 메서드.
     */
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

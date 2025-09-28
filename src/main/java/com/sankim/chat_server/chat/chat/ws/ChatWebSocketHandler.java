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
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    public ChatWebSocketHandler(@Lazy MessageService messageService) {
        this.messageService = messageService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) sessions.put(Long.valueOf(userId), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SendMessageRequest req = objectMapper.readValue(message.getPayload(), SendMessageRequest.class);
        Long senderId = Long.valueOf((String) session.getAttributes().get("userId"));
        messageService.sendMessage(senderId, req);  // 저장 후 Kafka 발행 + 이벤트
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) sessions.remove(Long.valueOf(userId));
    }

    public void broadcastMessage(MessageResponse dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            TextMessage tm = new TextMessage(json);
            for (WebSocketSession ws : sessions.values()) ws.sendMessage(tm);
        } catch (Exception e) {
            log.error("브로드캐스트 실패", e);
        }
    }
}



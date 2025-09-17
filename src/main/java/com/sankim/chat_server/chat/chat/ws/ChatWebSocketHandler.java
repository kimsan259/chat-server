package com.sankim.chat_server.chat.chat.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
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
 * STOMP 없이 순수 WebSocket을 처리하는 핸들러.
 * - 세션 속성의 userId를 기준으로 사용자 세션을 관리하고,
 * - 들어오는 메시지를 DB에 저장한 후 커밋 후 이벤트를 통해 브로드캐스트합니다.
 */
@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    // 현재 접속 중인 세션을 userId ↔ WebSocketSession 형태로 저장
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 생성자에서 MessageService를 @Lazy 주입하고
     * JavaTimeModule을 등록한 ObjectMapper를 초기화합니다.
     */
    @Autowired
    public ChatWebSocketHandler(@Lazy MessageService messageService) {
        this.messageService = messageService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /** 웹소켓 연결 수립 시: 세션 속성에서 userId를 꺼내 저장 */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.put(Long.valueOf(userId), session);
            log.info("웹소켓 연결됨: userId={}", userId);
        }
    }

    /** 클라이언트로부터 메시지를 수신 */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 프런트에서 보내온 JSON을 DTO로 변환
        SendMessageRequest req = objectMapper.readValue(message.getPayload(), SendMessageRequest.class);
        Long senderId = Long.valueOf((String) session.getAttributes().get("userId"));
        // 메시지 저장. 저장 후 브로드캐스트는 이벤트 리스너가 처리합니다.
        messageService.sendMessage(senderId, req);
    }

    /** 웹소켓 연결 종료 시 세션 제거 */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.remove(Long.valueOf(userId));
            log.info("웹소켓 연결 종료: userId={}", userId);
        }
    }

    /** DB 커밋 후 호출되는 브로드캐스트 메서드 */
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

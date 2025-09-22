package com.sankim.chat_server.chat.chat.message;


import com.sankim.chat_server.chat.chat.api.dto.MessageResponse;
import com.sankim.chat_server.chat.chat.ws.ChatWebSocketHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaMessageListener {
    private final ChatWebSocketHandler chatWebSocketHandler;

    public KafkaMessageListener(ChatWebSocketHandler chatWebSocketHandler) {
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @KafkaListener(topics = "chat-messages", groupId = "chat-consumer")
    public void listen(MessageResponse dto) {
        // 수신한 메시지를 연결된 모든 WebSocket 클라이언트에 브로드캐스트
        chatWebSocketHandler.broadcastMessage(dto);
    }
}


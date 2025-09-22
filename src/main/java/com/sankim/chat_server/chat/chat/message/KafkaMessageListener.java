package com.sankim.chat_server.chat.chat.message;


import com.sankim.chat_server.chat.chat.api.dto.MessageResponse;
import com.sankim.chat_server.chat.chat.ws.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaMessageListener {
    private final ChatWebSocketHandler chatWebSocketHandler;

    @KafkaListener(topics = "chat-messages", groupId = "chat-consumer")
    public void listen(MessageResponse dto) {
        chatWebSocketHandler.broadcastMessage(dto);
    }
}


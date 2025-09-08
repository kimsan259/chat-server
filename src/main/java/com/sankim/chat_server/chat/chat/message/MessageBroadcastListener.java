// src/main/java/com/sankim/chat_server/chat/chat/message/MessageBroadcastListener.java
package com.sankim.chat_server.chat.chat.message;

import com.sankim.chat_server.chat.chat.api.dto.MessageResponse;
import com.sankim.chat_server.chat.chat.ws.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 트랜잭션 커밋 후 MessageCreatedEvent를 받아서 WebSocket으로 브로드캐스트하는 리스너.
 */
@Component
@RequiredArgsConstructor
public class MessageBroadcastListener {

    private final ChatWebSocketHandler chatWebSocketHandler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(MessageCreatedEvent event) {
        MessageResponse dto = event.dto();
        // ChatWebSocketHandler에 정의한 broadcastMessage()를 호출
        chatWebSocketHandler.broadcastMessage(dto);
    }
}

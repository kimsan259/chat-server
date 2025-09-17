// src/main/java/com/sankim/chat_server/chat/chat/message/MessageBroadcastListener.java
package com.sankim.chat_server.chat.chat.message;

import com.sankim.chat_server.chat.chat.ws.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 트랜잭션 커밋 이후에 브로드캐스트를 수행하는 리스너
// src/main/java/com/sankim/chat_server/chat/chat/message/MessageBroadcastListener.java
@Component
@RequiredArgsConstructor
public class MessageBroadcastListener {
    private final ChatWebSocketHandler chatWebSocketHandler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageCreated(MessageCreatedEvent event) {
        chatWebSocketHandler.broadcastMessage(event.dto());
    }
}


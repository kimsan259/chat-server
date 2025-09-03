// src/main/java/com/sankim/chat_server/chat/chat/message/MessageBroadcastListener.java
package com.sankim.chat_server.chat.chat.message;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MessageBroadcastListener {

    private final SimpMessagingTemplate template;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(MessageCreatedEvent event) {
        var dto = event.dto();
        template.convertAndSend("/topic/chats/" + dto.chatId(), dto);
    }
}

// src/main/java/com/sankim/chat_server/chat/chat/message/MessageCreatedEvent.java
package com.sankim.chat_server.chat.chat.message;

import com.sankim.chat_server.chat.chat.api.dto.MessageResponse;

// 메시지 저장 완료 후 브로드캐스트를 알리는 이벤트
public record MessageCreatedEvent(MessageResponse dto) {}

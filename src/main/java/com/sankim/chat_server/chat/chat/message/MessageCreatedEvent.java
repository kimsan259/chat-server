// src/main/java/com/sankim/chat_server/chat/chat/message/MessageCreatedEvent.java
package com.sankim.chat_server.chat.chat.message;

import com.sankim.chat_server.chat.chat.api.dto.MessageResponse;

/** 메시지 저장이 완료된 후 방송할 때 사용하는 이벤트 */
public record MessageCreatedEvent(MessageResponse dto) {}

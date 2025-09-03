// src/main/java/com/sankim/chat_server/chat/chat/message/MessageCreatedEvent.java
package com.sankim.chat_server.chat.chat.message;

import com.sankim.chat_server.chat.chat.api.dto.MessageResponse;

/** 트랜잭션 커밋 이후에 브로드캐스트할 때 사용할 도메인 이벤트 */
public record MessageCreatedEvent(MessageResponse dto) {}

package com.sankim.chat_server.chat.chat.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 읽음 처리 요청 DTO: 사용자가 마지막으로 읽은 메시지 ID를 전달합니다.
 * record를 사용하면 Lombok 없이도 getter 메서드가 자동으로 생성됩니다.
 */
public record ReadUpToRequest(@NotNull Long lastReadMessageId) {
    // record 형태로 정의하면 lastReadMessageId() 메서드가 생성됩니다.
}


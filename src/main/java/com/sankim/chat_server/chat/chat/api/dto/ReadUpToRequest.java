package com.sankim.chat_server.chat.chat.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 읽음 처리 요청 DTO
 * record를 사용하면 lastReadMessageId() 메서드가 자동 생성됩니다.
 */
public record ReadUpToRequest(@NotNull Long lastReadMessageId) { }

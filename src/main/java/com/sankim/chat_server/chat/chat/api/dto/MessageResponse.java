package com.sankim.chat_server.chat.chat.api.dto;

import java.time.Instant;

public record MessageResponse(
        Long id, Long chatId, Long senderId, String contentType, String content,
        Instant createdAt, long seenCount
) {
}

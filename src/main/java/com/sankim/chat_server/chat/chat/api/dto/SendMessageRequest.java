package com.sankim.chat_server.chat.chat.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendMessageRequest(
        @NotNull Long chatId, // 어떤 방에 보낼지
        @NotBlank String content, // 본문
        String contentType // null 이면 TEXT로 처리
        
) {
}

package com.sankim.chat_server.chat.chat.api.dto;

import java.time.Instant;

/** 내 채팅방 목록 카드에 필요한 최소정보
 */
public record ChatSummary(
        Long chatId, String type, String title,
        String lastMessagePreview, Instant lastMessageAt,
        int memberCount, long unreadCount
) {
}

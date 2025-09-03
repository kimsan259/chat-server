package com.sankim.chat_server.chat.chat.api;

import com.sankim.chat_server.chat.chat.api.dto.MessageResponse;
import com.sankim.chat_server.chat.chat.api.dto.ReadUpToRequest;
import com.sankim.chat_server.chat.chat.api.dto.SendMessageRequest;
import com.sankim.chat_server.chat.chat.message.MessageService;
import com.sankim.chat_server.chat.chat.read.ReadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api") // 공통 prefix를 붙이면 URL 관리가 쉽습니다. (선택)
public class MessageController {

    private final MessageService messageService;
    private final ReadService readService;

    /**
     * 메시지 목록 조회
     * - 헤더 X-USER-ID: 현재 로그인 유저를 간단히 흉내
     * - chatId: 어느 채팅방의 메시지를 볼지
     * - page/size: 페이징 파라미터
     * - 정렬: 최신 먼저 (자바 필드명인 createAt DESC)
     *
     * ⚠️ 정렬에 쓰는 문자열은 "자바 필드명" 입니다.
     *    BaseTimeEntity에 있는 필드명이 createAt 이므로 "createAt"을 써야 합니다.
     *    (createdAt 이라고 쓰면 에러가 납니다)
     */
    @GetMapping("/messages")
    public Page<MessageResponse> getMessages(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam Long chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        // 안전장치: 말도 안 되는 값이 들어오면 적당한 값으로 보정
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 50;

        // ✅ 정렬 필드는 자바 필드명 createAt
        return messageService.getMessages(
                userId,
                chatId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createAt"))
        );
    }

    /** 메시지 전송 */
    @PostMapping("/messages")
    public MessageResponse sendMessage(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody @Valid SendMessageRequest req
    ) {
        return messageService.sendMessage(userId, req);
    }

    /** 읽음 처리 (해당 채팅방에서 특정 메시지까지 읽었다) */
    @PostMapping("/chats/{chatId}/read")
    public void readUpTo(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long chatId,
            @RequestBody @Valid ReadUpToRequest req
    ) {
        readService.readUpTo(userId, chatId, req);
    }
}

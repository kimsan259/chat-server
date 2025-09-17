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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api") // 공통 prefix
public class MessageController {

    private final MessageService messageService;
    private final ReadService readService;

    /** 메시지 목록 조회 */
    @GetMapping("/messages")
    public Page<MessageResponse> getMessages(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam Long chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        // 안전장치
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 50;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createAt"));
        return messageService.getMessages(userId, chatId, pageable);
    }

    /** 메시지 전송 */
    @PostMapping("/messages")
    public MessageResponse sendMessage(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody @Valid SendMessageRequest req) {
        return messageService.sendMessage(userId, req);
    }

    /** 읽음 처리 */
    @PostMapping("/chats/{chatId}/read")
    public void readUpTo(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long chatId,
            @RequestBody @Valid ReadUpToRequest req) {
        readService.readUpTo(userId, chatId, req);
    }
}

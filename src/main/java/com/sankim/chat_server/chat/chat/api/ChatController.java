package com.sankim.chat_server.chat.chat.api;


import com.sankim.chat_server.chat.chat.ChatService;
import com.sankim.chat_server.chat.chat.api.dto.ChatSummary;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;

    /** 내 채팅방 목록 : GET /api/chats?page=0&size=20 */
    @GetMapping
    public Page<ChatSummary> getMyChats(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return chatService.getMyChats(userId, page, size);
    }

    /** 특정 채팅방 조회(v1은 간단 응답) */
    @GetMapping("/{id}")
    public Object getChat(@PathVariable Long id) {
        return java.util.Map.of("chatId", id, "message", "상세는 v2에서 강화");
    }

}

// redis cache 에 안읽음 정보를 저장해놓는게 좋겠다
// 또는 특정 채팅방에 참여중인 유저 id 를 저장해놓는게 좋겠다.
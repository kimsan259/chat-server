package com.sankim.chat_server.chat.chat.read;

import com.sankim.chat_server.chat.chat.UserChat;
import com.sankim.chat_server.chat.chat.api.dto.ReadUpToRequest;
import com.sankim.chat_server.chat.chat.repository.UserChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // ✅ 스프링 @Transactional 사용 권장
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ReadService {
    private final UserChatRepository userChatRepo;

    @Transactional
    public void readUpTo(Long currentUserId, Long chatId, ReadUpToRequest req) {
        // 유저-채팅방 엔티티 조회
        UserChat uc = userChatRepo.findByUser_IdAndChat_Id(currentUserId, chatId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 멤버가 아님"));
        Long lastReadId = req.lastReadMessageId();
        // 기존 읽음 ID보다 큰 경우에만 업데이트
        if (lastReadId != null && (uc.getLastReadMessageId() == null || uc.getLastReadMessageId() < lastReadId)) {
            uc.setLastReadMessageId(lastReadId);
            uc.setLastReadAt(Instant.now());
        }
    }
}

package com.sankim.chat_server.chat.chat.message;

import com.sankim.chat_server.chat.chat.*;
import com.sankim.chat_server.chat.chat.api.dto.*;
import com.sankim.chat_server.chat.chat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메시지를 저장하고 조회하는 서비스입니다.
 * DB 저장 후 이벤트를 발행하여 커밋 후 브로드캐스트를 수행합니다.
 */
@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepo;
    private final ChatRepository chatRepo;
    private final UserRepository userRepo;
    private final UserChatRepository userChatRepo;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public MessageResponse sendMessage(Long currentUserId, SendMessageRequest req) {
        Chat chat = chatRepo.findById(req.chatId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));
        if (!userChatRepo.existsByUser_IdAndChat_Id(currentUserId, chat.getId())) {
            throw new IllegalArgumentException("채팅방 멤버가 아님");
        }
        User sender = userRepo.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        Message msg = Message.builder()
                .chat(chat).sender(sender)
                .contentType(req.contentType() == null ? "TEXT" : req.contentType())
                .content(req.content())
                .build();
        messageRepo.save(msg);

        MessageResponse dto = new MessageResponse(
                msg.getId(), chat.getId(), sender.getId(),
                msg.getContentType(), msg.getContent(),
                msg.getCreateAt(), 1L);
        // 커밋 이후 브로드캐스트 이벤트 발행
        eventPublisher.publishEvent(new MessageCreatedEvent(dto));
        return dto;
    }
}

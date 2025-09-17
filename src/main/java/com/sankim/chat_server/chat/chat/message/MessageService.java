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

    /** 메시지를 저장하고 커밋 후 이벤트를 발행 */
    @Transactional
    public MessageResponse sendMessage(Long currentUserId, SendMessageRequest req) {
        // 1) 방 존재 확인
        Chat chat = chatRepo.findById(req.chatId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));
        // 2) 사용자가 멤버인지 확인
        if (!userChatRepo.existsByUser_IdAndChat_Id(currentUserId, chat.getId())) {
            throw new IllegalArgumentException("채팅방 멤버가 아님");
        }
        // 3) 발신자 조회
        User sender = userRepo.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        // 4) 메시지 저장
        Message msg = Message.builder()
                .chat(chat)
                .sender(sender)
                .contentType(req.contentType() == null ? "TEXT" : req.contentType())
                .content(req.content())
                .build();
        messageRepo.save(msg);

        // 5) 응답 DTO 생성
        MessageResponse dto = new MessageResponse(
                msg.getId(), chat.getId(), sender.getId(),
                msg.getContentType(), msg.getContent(),
                msg.getCreateAt(), 1L
        );

        // 6) 커밋 이후 브로드캐스트 이벤트 발행
        eventPublisher.publishEvent(new MessageCreatedEvent(dto));
        return dto;
    }

    /** 메시지 목록 조회 (읽기 전용) */
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(Long userId, Long chatId, Pageable pageable) {
        if (!userChatRepo.existsByUser_IdAndChat_Id(userId, chatId)) {
            throw new IllegalArgumentException("채팅방 멤버가 아님");
        }
        Pageable effective = pageable;
        if (pageable == null || pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            effective = PageRequest.of(
                    pageable == null ? 0 : pageable.getPageNumber(),
                    pageable == null ? 50 : pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createAt")
            );
        }
        return messageRepo.findByChatId(chatId, effective)
                .map(m -> new MessageResponse(
                        m.getId(),
                        m.getChat().getId(),
                        m.getSender().getId(),
                        m.getContentType(),
                        m.getContent(),
                        m.getCreateAt(),
                        0L
                ));
    }
}

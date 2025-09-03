// src/main/java/com/sankim/chat_server/chat/chat/message/MessageService.java
package com.sankim.chat_server.chat.chat.message;

import com.sankim.chat_server.chat.chat.Chat;
import com.sankim.chat_server.chat.chat.Message;
import com.sankim.chat_server.chat.chat.User;
import com.sankim.chat_server.chat.chat.api.dto.MessageResponse;
import com.sankim.chat_server.chat.chat.api.dto.SendMessageRequest;
import com.sankim.chat_server.chat.chat.repository.ChatRepository;
import com.sankim.chat_server.chat.chat.repository.MessageRepository;
import com.sankim.chat_server.chat.chat.repository.UserChatRepository;
import com.sankim.chat_server.chat.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepo;
    private final ChatRepository chatRepo;
    private final UserRepository userRepo;
    private final UserChatRepository userChatRepo;

    // ✅ 브로드캐스트는 “커밋 이후”에만 일어나도록 이벤트로 발행
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 메시지 전송 (쓰기 트랜잭션)
     * 1) 채팅방 존재 확인
     * 2) 현재 유저가 그 방 멤버인지 확인
     * 3) 메시지 저장
     * 4) DTO 만들기
     * 5) (중요) 트랜잭션 커밋 후 브로드캐스트 되도록 이벤트 발행
     */
    @Transactional
    public MessageResponse sendMessage(Long currentUserId, SendMessageRequest req) {
        // 1) 방 존재 확인
        Chat chat = chatRepo.findById(req.chatId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));

        // 2) 멤버 검증 (연관관계 경로: user.id / chat.id)
        if (!userChatRepo.existsByUser_IdAndChat_Id(currentUserId, chat.getId())) {
            throw new IllegalArgumentException("채팅방 멤버가 아님");
        }

        // 3) 발신자 조회
        User sender = userRepo.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        // 4) 메시지 엔티티 생성/저장
        Message msg = Message.builder()
                .chat(chat)
                .sender(sender)
                .contentType(req.contentType() == null ? "TEXT" : req.contentType())
                .content(req.content())
                .build();
        messageRepo.save(msg);

        // 5) 응답 DTO (createdAt은 엔티티의 createAt 값을 그대로 전달)
        MessageResponse dto = new MessageResponse(
                msg.getId(),
                chat.getId(),
                sender.getId(),
                msg.getContentType(),
                msg.getContent(),
                msg.getCreateAt(),
                1L // 정책상: 보낸 본인 1명은 이미 읽음 처리로 간주
        );

        // 6) 커밋 이후에만 브로드캐스트 되도록 이벤트 발행
        eventPublisher.publishEvent(new MessageCreatedEvent(dto));

        return dto;
    }

    /**
     * 방별 메시지 페이징 조회 (읽기 전용)
     * - 정렬이 비었으면 createAt DESC로 강제
     * - (chat_id, create_at) 인덱스가 성능 핵심
     */
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(Long userId, Long chatId, Pageable pageable) {
        // 1) 멤버 검증
        if (!userChatRepo.existsByUser_IdAndChat_Id(userId, chatId)) {
            throw new IllegalArgumentException("채팅방 멤버가 아님");
        }

        // 2) 정렬/페이지 기본값 보정 (자바 필드명: createAt)
        Pageable effective;
        if (pageable == null) {
            effective = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createAt"));
        } else if (pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            effective = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createAt"));
        } else {
            effective = pageable;
        }

        // 3) 페이지 조회 → DTO 변환
        return messageRepo.findByChatId(chatId, effective)
                .map(m -> new MessageResponse(
                        m.getId(),
                        m.getChat().getId(),
                        m.getSender().getId(),
                        m.getContentType(),
                        m.getContent(),
                        m.getCreateAt(),
                        0L // 목록에서는 seen 계산 생략 (v1)
                ));
    }
}

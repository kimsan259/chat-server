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
import com.sankim.chat_server.chat.chat.ws.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메시지 관련 비즈니스 로직을 담당하는 서비스.
 * - 메시지 저장(쓰기)은 @Transactional로 묶어 DB에 안전하게 저장.
 * - 메시지 조회(읽기)는 readOnly = true 로 더 빠르고 안전하게 수행.
 * - DB에 저장된 후 WebSocketHandler를 통해 실시간으로 브로드캐스트.
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepo;
    private final ChatRepository chatRepo;
    private final UserRepository userRepo;
    private final UserChatRepository userChatRepo;
    private final ChatWebSocketHandler chatWebSocketHandler; // WebSocket 브로드캐스트용

    /**
     * 메시지 전송 (저장 + 브로드캐스트)
     *
     * @param currentUserId 현재 메시지를 보내는 사용자 ID
     * @param req           채팅방 ID와 내용이 들어있는 요청 DTO
     * @return 저장된 메시지를 표현하는 응답 DTO
     */
    @Transactional
    public MessageResponse sendMessage(Long currentUserId, SendMessageRequest req) {
        // 1) 채팅방 존재 확인
        Chat chat = chatRepo.findById(req.chatId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));

        // 2) 사용자 멤버십 검증: userChatRepo에서 user.id 와 chat.id 경로를 기준으로 존재 여부 확인
        if (!userChatRepo.existsByUser_IdAndChat_Id(currentUserId, chat.getId())) {
            throw new IllegalArgumentException("채팅방 멤버가 아님");
        }

        // 3) 메시지 보낸 사람 조회
        User sender = userRepo.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        // 4) 메시지 엔티티 생성 및 저장
        Message msg = Message.builder()
                .chat(chat)
                .sender(sender)
                // contentType이 null이면 기본값 TEXT 지정
                .contentType(req.contentType() == null ? "TEXT" : req.contentType())
                .content(req.content())
                .build();
        messageRepo.save(msg); // JPA가 INSERT 실행

        // 5) 응답 DTO 구성: 서버는 DB 엔티티를 직접 반환하지 않고 DTO로 전달
        MessageResponse dto = new MessageResponse(
                msg.getId(),
                chat.getId(),
                sender.getId(),
                msg.getContentType(),
                msg.getContent(),
                msg.getCreateAt(),
                1L // 보낸 본인 1명은 이미 읽음으로 간주
        );

        // 6) 저장이 끝나면 WebSocketHandler를 통해 실시간 브로드캐스트
        // chatWebSocketHandler의 broadcastMessage 메서드가 현재 접속 중인 세션에 전송
        chatWebSocketHandler.broadcastMessage(dto);

        return dto;
    }

    /**
     * 방별 메시지 목록 조회 (읽기 전용)
     *
     * @param userId  현재 사용자의 ID (멤버십 검증 용)
     * @param chatId  조회하려는 채팅방 ID
     * @param pageable 페이지 정보와 정렬 옵션 (createAt DESC 권장)
     * @return DTO 형태의 메시지 페이지
     */
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(Long userId, Long chatId, Pageable pageable) {
        // 1) 채팅방 멤버인지 확인
        if (!userChatRepo.existsByUser_IdAndChat_Id(userId, chatId)) {
            throw new IllegalArgumentException("채팅방 멤버가 아님");
        }

        // 2) 정렬 조건이 없으면 createAt DESC 로 기본값 설정
        Pageable effective = pageable;
        if (pageable == null || pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            effective = PageRequest.of(
                    pageable == null ? 0 : pageable.getPageNumber(),
                    pageable == null ? 50 : pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createAt")
            );
        }

        // 3) 메시지를 조회하고 DTO로 매핑
        return messageRepo.findByChatId(chatId, effective)
                .map(m -> new MessageResponse(
                        m.getId(),
                        m.getChat().getId(),
                        m.getSender().getId(),
                        m.getContentType(),
                        m.getContent(),
                        m.getCreateAt(),
                        0L // 목록에서는 미읽음 수를 계산하지 않음
                ));
    }
}

// src/main/java/com/sankim/chat_server/chat/chat/message/MessageService.java
package com.sankim.chat_server.chat.chat.message;

import com.sankim.chat_server.chat.chat.*;
import com.sankim.chat_server.chat.chat.api.dto.*;
import com.sankim.chat_server.chat.chat.repository.*;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepo;
    private final ChatRepository chatRepo;
    private final UserRepository userRepo;
    private final UserChatRepository userChatRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final RedissonClient redissonClient;
    private final KafkaTemplate<String, MessageResponse> kafkaTemplate;

    @Transactional
    public MessageResponse sendMessage(Long userId, SendMessageRequest req) {
        Chat chat = chatRepo.findById(req.chatId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));
        if (!userChatRepo.existsByUser_IdAndChat_Id(userId, chat.getId())) {
            throw new IllegalArgumentException("채팅방 멤버가 아님");
        }
        User sender = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        // Message 엔티티 생성
        Message msg = Message.builder()
                .chat(chat)
                .sender(sender)
                .contentType(req.contentType() == null ? "TEXT" : req.contentType())
                .content(req.content())
                .build();
        messageRepo.save(msg);  // ID가 채워짐
        MessageResponse dto = new MessageResponse(
                msg.getId(), chat.getId(), sender.getId(),
                msg.getContentType(), msg.getContent(), msg.getCreateAt(), 1L);


        // Kafka 발행
        kafkaTemplate.send("chat-messages", dto.chatId().toString(), dto);
        // 커밋 후 WebSocket 브로드캐스트
        eventPublisher.publishEvent(new MessageCreatedEvent(dto));
        return dto;
    }



    @Transactional(readOnly = true)
    @Cacheable(value = "chatMessages", key = "#chatId + ':' + #pageable.pageNumber")
    public Page<MessageResponse> getMessages(Long userId, Long chatId, Pageable pageable) {
        if (!userChatRepo.existsByUser_IdAndChat_Id(userId, chatId)) {
            throw new IllegalArgumentException("채팅방 멤버가 아님");
        }
        Pageable effective = pageable;
        if (pageable == null || pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            effective = PageRequest.of(
                    pageable == null ? 0 : pageable.getPageNumber(),
                    pageable == null ? 50 : pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createAt"));
        }
        return messageRepo.findByChatId(chatId, effective)
                .map(m -> new MessageResponse(
                        m.getId(), m.getChat().getId(), m.getSender().getId(),
                        m.getContentType(), m.getContent(), m.getCreateAt(), 0L));
    }
}

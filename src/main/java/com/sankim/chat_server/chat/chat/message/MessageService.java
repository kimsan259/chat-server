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

    @Transactional
    @CacheEvict(value = "chatMessages", key = "#req.chatId() + ':' + '*'", allEntries = true)
    public MessageResponse sendMessage(Long userId, SendMessageRequest req) {
        Chat chat = chatRepo.findById(req.chatId()).orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));
        if (!userChatRepo.existsByUser_IdAndChat_Id(userId, chat.getId())) {
            throw new IllegalArgumentException("채팅방 멤버가 아님");
        }
        User sender = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        // 채팅방별 락
        String lockName = "chatLock:" + chat.getId();
        RLock lock = redissonClient.getLock(lockName);
        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                Message msg = Message.builder()
                        .chat(chat).sender(sender)
                        .contentType(req.contentType() == null ? "TEXT" : req.contentType())
                        .content(req.content())
                        .build();
                messageRepo.save(msg);

                MessageResponse dto = new MessageResponse(
                        msg.getId(), chat.getId(), sender.getId(),
                        msg.getContentType(), msg.getContent(), msg.getCreateAt(), 1L);
                eventPublisher.publishEvent(new MessageCreatedEvent(dto));
                return dto;
            }
            throw new IllegalStateException("메시지 전송 중 다른 작업과 충돌했습니다.");
        } catch (InterruptedException e) {
            throw new RuntimeException("락 획득 중 오류", e);
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
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

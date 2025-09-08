// MessageService.java
package com.sankim.chat_server.chat.chat.message;
import com.sankim.chat_server.chat.chat.*;
import com.sankim.chat_server.chat.chat.api.dto.*;
import com.sankim.chat_server.chat.chat.repository.*;
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
                .chat(chat)
                .sender(sender)
                .contentType(req.contentType() == null ? "TEXT" : req.contentType())
                .content(req.content())
                .build();
        messageRepo.save(msg);

        MessageResponse dto = new MessageResponse(
                msg.getId(), chat.getId(), sender.getId(),
                msg.getContentType(), msg.getContent(),
                msg.getCreateAt(), 1L);

        // 유저들에게 메시지를 전달해줄때, ex)1000명방 -> 루프 천회
        // 스레드풀이나 카프카를 이용해서 비동기로 전달해주자.
        /*
        List<Long> userIds = userChatRepository.findByUserIdsByChatId(chatId);
        for(Long userId: userIds){
            Ws ws = webSocketHandler.getSessionByUserId(userId);
            if(ws == null){
                // 미접속중(소켓이 연결되어있지 않다.)
                fcmClient.push(new MessageResponse));
            } else {
                // 접속중
                ws.send(new TextMessage(new MessageResponse)));
            }
        }

        */


        // 커밋 후 브로드캐스트
        eventPublisher.publishEvent(new MessageCreatedEvent(dto));
        return dto;
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(Long userId, Long chatId, Pageable pageable) {
        if (!userChatRepo.existsByUser_IdAndChat_Id(userId, chatId)) {
            throw new IllegalArgumentException("채팅방 멤버가 아님");
        }
        Pageable effective = pageable;
        if (pageable == null || pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            effective = PageRequest.of(pageable == null ? 0 : pageable.getPageNumber(),
                    pageable == null ? 50 : pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createAt"));
        }
        return messageRepo.findByChatId(chatId, effective)
                .map(m -> new MessageResponse(
                        m.getId(),
                        m.getChat().getId(),
                        m.getSender().getId(),
                        m.getContentType(),
                        m.getContent(),
                        m.getCreateAt(),
                        0L));
    }
}

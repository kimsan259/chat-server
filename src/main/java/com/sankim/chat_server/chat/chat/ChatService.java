package com.sankim.chat_server.chat.chat;

import com.sankim.chat_server.chat.chat.api.dto.ChatSummary;
import com.sankim.chat_server.chat.chat.repository.MessageRepository;
import com.sankim.chat_server.chat.chat.repository.UserChatRepository;
import com.sankim.chat_server.chat.chat.Message;   // ← Message 엔티티 패키지에 맞게 import
import com.sankim.chat_server.chat.chat.UserChat;  // ← UserChat 엔티티 패키지에 맞게 import
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final UserChatRepository userChatRepo;
    private final MessageRepository messageRepo;

    /**
     * 내 채팅방 목록 조회
     *
     * - OSIV(false) 환경에서는 컨트롤러까지 엔티티를 들고 나가면 세션이 닫혀서 LAZY 접근 시 에러가 납니다.
     *   그래서 서비스(트랜잭션) 안에서 LAZY를 전부 접근/처리하고 DTO로 바꿔서 반환합니다.
     *
     * - userChatRepo.findPageWithChatByUserId(...) 는 fetch join 으로 UserChat의 chat을 미리 로딩합니다.
     *   (N+1과 LazyInitializationException 예방)
     *
     * - 정렬은 UserChat의 자바 필드명 "updateAt" 기준입니다. (자주 바뀌는 시간)
     */
    @Transactional(readOnly = true)
    public Page<ChatSummary> getMyChats(Long userId, int page, int size) {

        // PageRequest.of(페이지, 크기, 정렬) — 정렬 필드명은 "자바 필드명"을 사용합니다.
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updateAt"));

        // fetch join으로 chat을 미리 로딩하는 전용 쿼리 사용
        Page<UserChat> p = userChatRepo.findPageWithChatByUserId(userId, pageable);

        // Page<UserChat> -> Page<ChatSummary> 로 변환 (서비스 안에서 DTO로 변환)
        return p.map(uc -> {
            Long chatId = uc.getChat().getId();

            // "가장 최신 메시지 1개" — 자바 필드명 createAt 기준으로 정렬된 메서드 사용
            Message last = messageRepo
                    .findTop1ByChatIdOrderByCreateAtDesc(chatId) // ← 여기 이름이 포인트!
                    .orElse(null); // 없으면 null

            return new ChatSummary(
                    chatId,
                    uc.getChat().getType(),                     // 방 타입
                    uc.getChat().getTitle(),                    // 방 제목
                    last != null ? last.getContent() : "",      // 최근 메시지 내용
                    last != null ? last.getCreateAt() : null,   // 최근 메시지 시간 (자바 필드명 createAt)
                    0, // v1: 멤버 수는 추후 개선
                    0  // v1: 미읽음 수는 추후 개선
            );
        });
    }
}

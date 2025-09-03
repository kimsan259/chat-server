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

    /**
     * [읽음 밀어올리기]
     * - 같은 유저가 여러 기기에서 동시에 "읽음"을 올릴 수 있음 → 마지막(가장 큰 messageId)만 남아야 함
     * - UserChat 엔티티에는 @Version 이 있다고 가정(로그에 version 컬럼 생성됨)
     *   → 동시에 업데이트가 겹치면 낙관적 락으로 한쪽이 실패 → 재시도 정책 적용 가능
     *
     * - 포인트
     *   1) 레포지토리 메서드는 연관관계 경로로: findByUser_IdAndChat_Id(...)
     *   2) 현재 lastReadMessageId 보다 "큰 값"일 때만 갱신
     *   3) JPA 더티체킹으로 UPDATE 됨 (트랜잭션 커밋 시점 반영)
     */
    @Transactional
    public void readUpTo(Long currentUserId, Long chatId, ReadUpToRequest req) {
        if (req == null || req.lastReadMessageId() == null) {
            throw new IllegalArgumentException("lastReadMessageId가 필요합니다.");
        }

        // ✅ 올바른 메서드명: user.id / chat.id 경로를 이름에 반영
        UserChat uc = userChatRepo.findByUser_IdAndChat_Id(currentUserId, chatId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 멤버가 아님"));

        // 이미 더 높은 메시지까지 읽었으면 스킵, 낮으면 올림 (단조증가 보장)
        Long incoming = req.lastReadMessageId();
        Long current = uc.getLastReadMessageId();

        if (current == null || current < incoming) {
            uc.setLastReadMessageId(incoming);
            uc.setLastReadAt(Instant.now()); // 시간 기록
            // @Version 이 있으면 동시에 갱신 충돌 시 OptimisticLockingFailureException 발생 → 상위에서 재시도/무시 정책 가능
        }
        // 트랜잭션 종료 시점에 변경감지(더티체킹)로 UPDATE 수행
    }
}

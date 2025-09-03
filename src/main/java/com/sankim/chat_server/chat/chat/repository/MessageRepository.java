package com.sankim.chat_server.chat.chat.repository;

import com.sankim.chat_server.chat.chat.Message; // 패키지 맞게
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // 가장 최신 메시지 1건 — "자바 필드명" createAt 기준으로 내림차순
    Optional<Message> findTop1ByChatIdOrderByCreateAtDesc(Long chatId);

    // 메시지 목록 페이징 조회 (정렬은 서비스에서 Pageable로 넘겨주세요)
    Page<Message> findByChatId(Long chatId, Pageable pageable);

    long countByChatId(Long chatId);

    long countByChatIdAndIdGreaterThan(Long chatId, Long lastReadMessageId);

    // ---- [선택] 기존 코드와 호환용 (당장 서비스 못 바꾸면 임시로 사용) ----
    default Optional<Message> findTopByChatIdOrderByIdDesc(Long chatId) {
        return findTop1ByChatIdOrderByCreateAtDesc(chatId);
    }
}

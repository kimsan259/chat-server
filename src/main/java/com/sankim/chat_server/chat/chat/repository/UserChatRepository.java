package com.sankim.chat_server.chat.chat.repository;

import com.sankim.chat_server.chat.chat.UserChat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * UserChat 레포지토리 (실서비스용)
 *
 * - 메서드 이름은 "DB 컬럼명"이 아니라 "엔티티 자바 필드명" 기준입니다.
 *   ex) userId(X)  -> user.id(✓)  => existsByUser_IdAndChat_Id
 *
 * - OSIV(false) 환경/지연로딩(LAZY) 문제를 줄이기 위해
 *   페이지 목록 조회 시 fetch join 쿼리 메서드를 제공합니다.
 */
public interface UserChatRepository extends JpaRepository<UserChat, Long> {

    // ------------------------------------------------------------
    // [권장X] 옛날 호환용 이름: 엔티티에 userId라는 필드는 없으므로 잘못된 이름입니다.
    // 기존 서비스 코드가 이 메서드를 호출 중이면 당장은 깨지지 않도록 브리징합니다.
    // 장기적으로는 호출부를 findByUser_Id(...) 또는 findPageWithChatByUserId(...)로 변경하세요.
    // ------------------------------------------------------------
    @Deprecated
    default Page<UserChat> findByUserId(Long userId, Pageable pageable) {
        return findByUser_Id(userId, pageable);
    }

    // 올바른 파생 메서드 (연관관계의 id 경로 표기)
    Page<UserChat> findByUser_Id(Long userId, Pageable pageable);

    // 채팅방 멤버 여부 체크 (서비스에서 보안/검증에 자주 사용)
    boolean existsByUser_IdAndChat_Id(Long userId, Long chatId);

    // 필요 시 단건 조회
    Optional<UserChat> findByUser_IdAndChat_Id(Long userId, Long chatId);

    // 채팅방 참여자 수 (미래에 "인원수" 표시에 사용 가능)
    long countByChat_Id(Long chatId);

    // ------------------------------------------------------------
    // 페이지네이션 + fetch join (ManyToOne 은 페이징과 함께 안전)
    // - uc.chat 을 미리 로딩해서 OSIV(false) + LAZY 로 인한 예외 및 N+1을 예방
    // - 정렬은 Pageable의 Sort를 사용 (메서드에 별도 ORDER BY를 넣지 않음)
    // - countQuery 는 fetch 없이 가볍게
    // ------------------------------------------------------------
    @Query(value = """
            select uc
            from UserChat uc
            join fetch uc.chat c
            where uc.user.id = :userId
            """,
            countQuery = """
            select count(uc)
            from UserChat uc
            where uc.user.id = :userId
            """)
    Page<UserChat> findPageWithChatByUserId(@Param("userId") Long userId, Pageable pageable);
}

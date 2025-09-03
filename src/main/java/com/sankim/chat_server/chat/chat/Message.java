package com.sankim.chat_server.chat.chat;

import com.sankim.chat_server.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 메시지 엔티티
 *
 * - 시간 필드(createAt, updateAt)는 BaseTimeEntity(자바 필드명)에서 상속받습니다.
 *   DB에는 create_at / update_at 컬럼으로 저장됩니다. (스네이크 케이스)
 *
 * - 정렬/파생쿼리에서 쓰는 이름은 "자바 필드명" (createAt)
 * - 인덱스/DDL에서 쓰는 이름은 "DB 컬럼명" (create_at)
 */
@Entity
@Table(
        name = "message",
        indexes = {
                // ✅ 왜 이 인덱스? "채팅방별 최신 메시지 순 페이징"이 가장 많이 호출됨
                // ✅ 여기서는 DB 컬럼명 사용 (chat_id, create_at)
                @Index(name = "idx_msg_chat_created", columnList = "chat_id, create_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 방의 메시지인가?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    // 누가 보냈나?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // TEXT/IMAGE/FILE 등(간단히 TEXT만 써도 됨)
    @Column(length = 20)
    private String contentType;

    // 본문은 TEXT 컬럼
    @Column(nullable = false, columnDefinition = "text")
    private String content;
}

package com.sankim.chat_server.chat.chat;


import com.sankim.chat_server.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_chat", uniqueConstraints = @UniqueConstraint(name = "uk_user_chat", columnNames = {"user_id", "chat_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserChat extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /** LAZY : 필요할 때만 DB에서 가져온다 (요즘 JPA 기본 룰) */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    /**
     * 읽음 처리 v1 : 이 유저가 이 방에서 어디까지 읽었는지 (메시지 ID기준)
     * 요즘 카톡류 서비스에서 "읽음 수"를 빠르게 계산하려면 per-message 로그 대신
     * 유저별 "마지막 읽음"을 올려두고 >= 비교로 집계한다(쓰기/저장 비용 다운, 조회 계산 업
     */
    private Long lastReadMessageId;

    private Instant lastReadAt;

    /*
    낙관적 락 Optimistic : 동시 업데이트 충돌을 검출할 때 사용
    운영에서 읽음 올리기 같은 카운터성 갱신은 충돌 가능성이 있어 유용하다.
     */
    @Version
    private Long version;
}

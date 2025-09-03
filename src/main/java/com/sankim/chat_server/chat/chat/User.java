package com.sankim.chat_server.chat.chat;


import com.sankim.chat_server.common.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "users") // user는 예약어 충돌 피하려고 복수형 권장
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User extends BaseTimeEntity {

    /**
     * MySQL auto_increment 전략과 궁합이 좋다 => 왜 IDENTITY?
     * 대형 서비스는 스노우플레이크 같은 분산 ID를 쓰기도 한다.
     */

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인/고우 ID 용, 유니크 인덱스 필수 */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** 프로필 표시 이름*/
    @Column(length = 100)
    private String displayname;
}

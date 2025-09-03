package com.sankim.chat_server.common;


import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * 모든 엔티티가 공통으로 물려받는 "생성/수정 시간" 베이스 클래스.
 *
 * 핵심 개념
 * - @MappedSuperclass: 이 클래스는 테이블로 만들지 않고,
 *   여기에 정의한 필드를 "자식 엔티티의 컬럼"으로 포함시킵니다.
 * - 정렬/인덱스를 안정적으로 쓰기 위해 DB 컬럼명을 스네이크 표기(create_at/update_at)로 "고정"합니다.
 * - 시간값은 JPA 라이프사이클 훅(@PrePersist/@PreUpdate)에서 채웁니다.
 *
 * 필드명 vs 컬럼명
 * - 자바 필드명: createAt / updateAt (컨트롤러/서비스에서 정렬·접근할 때 사용)
 * - DB 컬럼명  : create_at / update_at (인덱스/SQL에서 사용)
 *   → 이름 충돌/혼동 방지를 위해 명시적으로 분리해 둡니다.
 *
 * 시간 타입
 * - Instant = UTC 기준의 절대시간. 운영에서 타임존 이슈가 줄어 안정적입니다.
 */
@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {

    /** 최초 생성 시각 (이후 수정 불가) */
    @Column(name = "create_at", nullable = false, updatable = false)
    private Instant createAt;

    /** 마지막 수정 시각 */
    @Column(name = "update_at", nullable = false)
    private Instant updateAt;

    /** INSERT 직전에 한 번만 호출 → 생성·수정 시간을 동일하게 now 로 채움 */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createAt = now;
        this.updateAt = now;
    }

    /** UPDATE 직전에 호출 → 수정 시간만 갱신 */
    @PreUpdate
    protected void onUpdate() {
        this.updateAt = Instant.now();
    }
}

// 우리 엔티티(Message, Chat, User, UserChat)는 모두 BaseTimeEntity를 상속받는다.
// 그래서 자동으로 create_at, update_at 컬럼이 만들어지고
// 새 레코드 INSERT 시 -> 두 시간 모두 NOW
// UPDATE 시 -> update_at 만 NOW
// 컨트롤러에서 최신순 정렬이 필요할 때, Sort.by("createAt").descending() 하면 된다.
// JPA는 이것을 DB컬럼 create_at 로 바꿔서 정렬 SQL을 생성한다.

/*
Controller : HTTP 요청받아 파라미터/헤더를 읽고 -> Service 호출
Service : 비즈니스 규칙(권한,검증,트랜잭션) -> Repository 로 데이터 읽기/쓰기
Repository(JPA) : 메소드 이름만으로 SELECT/INSERT/UPDATE SQL을 자동 생성
Entity : 테이블과 1:1매핑되는 "자바 클래스"(필드=컬럼)
BaseTimeEntity : 모든 엔티티의 시간 필드를 공통화(중복제거 + 정렬/인덱스 일관성)
 */


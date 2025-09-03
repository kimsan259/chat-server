package com.sankim.chat_server.chat.chat.repository;

import com.sankim.chat_server.chat.chat.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 왜 요즘 JPA/스프링 데이터
 * 반복적인 CRUD, 페이지 쿼리를 인터페이스 메소드 이름만으로 자동구현 -> 생산성업, 실수 다운
 */

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}

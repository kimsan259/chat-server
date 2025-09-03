package com.sankim.chat_server.chat.chat.repository;

import com.sankim.chat_server.chat.chat.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Chat, Long> {
}

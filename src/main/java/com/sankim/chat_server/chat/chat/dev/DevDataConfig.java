package com.sankim.chat_server.chat.chat.dev;


import com.sankim.chat_server.chat.chat.Chat;
import com.sankim.chat_server.chat.chat.User;
import com.sankim.chat_server.chat.chat.UserChat;
import com.sankim.chat_server.chat.chat.repository.ChatRepository;
import com.sankim.chat_server.chat.chat.repository.UserChatRepository;
import com.sankim.chat_server.chat.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [왜 필요?]
 * - 테스트 데이터가 없으면 API가 빈 리스트만 준다.
 * - 서버 시작 시 한 번만 “필요 최소” 데이터를 넣어 빠르게 테스트 가능.
 * - CommandLineRunner 는 스프링이 모든 빈을 준비한 뒤 "마지막에" 실행해 준다.
 */
@Configuration
@RequiredArgsConstructor
public class DevDataConfig {

    @Bean
    CommandLineRunner initData(UserRepository users,
                               ChatRepository chats,
                               UserChatRepository userChats) {
        return args -> {
            if (users.count() == 0) {
                // 1) 사용자 두 명 생성
                User u1 = users.save(User.builder().username("alice").displayname("앨리스").build());
                User u2 = users.save(User.builder().username("bob").displayname("밥").build());

                // 2) 1:1 채팅방 하나 생성
                Chat c1 = chats.save(Chat.builder().type("DIRECT").title(null).build());

                // 3) 두 사용자를 그 방의 멤버로 연결
                userChats.save(UserChat.builder().user(u1).chat(c1).build());
                userChats.save(UserChat.builder().user(u2).chat(c1).build());

                System.out.println("[seed] users=" + users.count() + ", chats=" + chats.count());
                System.out.println("[seed] alice(id= " + u1.getId() + "), bob(id= " + u2.getId() + "), chat(id= " + c1.getId() + ")");
            }
        };
    }
}
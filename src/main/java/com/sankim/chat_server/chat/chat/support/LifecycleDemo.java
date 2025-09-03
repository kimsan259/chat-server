package com.sankim.chat_server.chat.chat.support;


import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LifecycleDemo {

    @PostConstruct
    void init() {
        log.info("[Lifecycle] 서버 기동 - 필요한 초기화 작업 수행");
    }

    @PreDestroy
    void close() {
        log.info("[Lifecycle] 서버 종료 - 자원 정리/플러시 수행");
    }
}

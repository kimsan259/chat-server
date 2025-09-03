package com.sankim.chat_server;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [IOC/Bean/DI 기초설명]
 * - @RestController: 스프링이 이 클래스를 빈으로 등록한다.
 * = 스프링 컨테이너가 객체 생명주기를 관리. 이것이 Inversion of Control
 * 등록된 빈은 의존성 주입으로 다른곳에 자동으로 꽂아쓸수 있다.
 * 지금은 외부 의존이 없어서 주입은 안하지만, 컨테이너가 대신 관리한다. 감만 잡자
 */
@RestController
public class PingController {

    /** 단순 핑 API. 브라우저에서 http://localhost:8083/ping 호출 */
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}


// src/main/java/com/sankim/chat_server/config/RedissonConfig.java
package com.sankim.chat_server.chat.chat.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 클라이언트 설정.
 */
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        // 단일 노드 Redis 클러스터를 사용 (환경에 맞게 변경)
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379");
        return Redisson.create(config);
    }
}


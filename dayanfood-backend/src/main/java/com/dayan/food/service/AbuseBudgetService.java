package com.dayan.food.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

@Service
public class AbuseBudgetService {
    private static final DefaultRedisScript<Long> TAKE = new DefaultRedisScript<>("""
            local n = redis.call('INCR', KEYS[1])
            if n == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
            return n
            """, Long.class);
    private final StringRedisTemplate redis;

    public AbuseBudgetService(StringRedisTemplate redis) { this.redis = redis; }

    public void login(String remoteAddress, String username) {
        require("login:ip:" + digest(remoteAddress), 20, Duration.ofMinutes(5));
        require("login:account:" + digest(username.toLowerCase()), 10, Duration.ofMinutes(5));
    }

    public void mail(String remoteAddress, String target) {
        require("mail:ip:" + digest(remoteAddress), 10, Duration.ofHours(1));
        require("mail:target:" + digest(target.toLowerCase()), 5, Duration.ofHours(1));
    }

    private void require(String suffix, long limit, Duration window) {
        Long used = redis.execute(TAKE, List.of("dayan-food:budget:" + suffix), Long.toString(window.toMillis()));
        if (used != null && used > limit) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁，请稍后重试");
        }
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)), 0, 12);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}

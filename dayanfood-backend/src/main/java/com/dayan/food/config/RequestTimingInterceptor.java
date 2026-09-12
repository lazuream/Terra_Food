package com.dayan.food.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import java.util.UUID;

@Component
public class RequestTimingInterceptor implements HandlerInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTimingInterceptor.class);
    private static final String START = RequestTimingInterceptor.class.getName() + ".start";

    @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START, System.nanoTime());
        String incoming = request.getHeader("X-Request-ID");
        String id = incoming != null && incoming.matches("[A-Za-z0-9._-]{1,64}") ? incoming : UUID.randomUUID().toString();
        response.setHeader("X-Request-ID", id);
        return true;
    }

    @Override public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception exception) {
        Object started = request.getAttribute(START);
        if (!(started instanceof Long value)) return;
        long millis = (System.nanoTime() - value) / 1_000_000;
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        LOGGER.info("http_request method={} route={} status={} durationMs={}", request.getMethod(),
                pattern == null ? "unmatched" : pattern, response.getStatus(), millis);
    }
}

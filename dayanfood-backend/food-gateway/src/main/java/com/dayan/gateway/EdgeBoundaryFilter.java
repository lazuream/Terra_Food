package com.dayan.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** This gateway is the public edge; upstream identity/forwarding headers are never trusted. */
@Component
public class EdgeBoundaryFilter implements GlobalFilter, Ordered {
    @Override public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        // Reject ambiguous path encodings instead of letting routing and servlet decoding disagree.
        String raw = exchange.getRequest().getURI().getRawPath();
        if (path.startsWith("/api/internal") || raw.contains("%") || raw.contains(";")
                || raw.contains("\\") || raw.contains("//") || raw.contains("/../") || raw.contains("/./")) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }
        var request = exchange.getRequest().mutate().headers(headers -> {
            headers.remove("Forwarded");
            headers.remove("X-Forwarded-For");
            headers.remove("X-Forwarded-Host");
            headers.remove("X-Forwarded-Proto");
            headers.remove("X-Forwarded-Port");
            headers.remove("X-Forwarded-Prefix");
            headers.remove("X-User-Id");
            headers.remove("X-User-Role");
            headers.remove("X-MCP-Backend-Token");
            if (exchange.getRequest().getRemoteAddress() != null)
                headers.set("X-Forwarded-For", exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
        }).build();
        return chain.filter(exchange.mutate().request(request).build());
    }
}

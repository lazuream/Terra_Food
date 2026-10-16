package com.dayan.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutes {
    @Bean
    RouteLocator routes(RouteLocatorBuilder builder,
            @Value("${app.routes.core}") String core, @Value("${app.routes.media}") String media) {
        return builder.routes()
                .route("media", route -> route.path("/api/images", "/api/images/**", "/uploads/**").uri(media))
                .route("core", route -> route.path("/api/**").uri(core))
                .build();
    }
}

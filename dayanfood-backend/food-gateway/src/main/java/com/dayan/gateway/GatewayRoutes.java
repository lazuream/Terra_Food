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
            @Value("${app.routes.core}") String core, @Value("${app.routes.media}") String media,
            @Value("${app.routes.user}") String user, @Value("${app.routes.community}") String community) {
        return builder.routes()
                .route("user", route -> route.path("/api/auth/**", "/api/admin/users", "/api/admin/users/**",
                        "/api/profile/avatar", "/api/profile/signature", "/api/profile/display-name",
                        "/api/achievements", "/api/achievements/**", "/api/etchings", "/api/etchings/**").uri(user))
                .route("community", route -> route.path(
                        "/api/profile/favorites", "/api/profile/favorites/**",
                        "/api/profile/wishlist", "/api/profile/wishlist/**",
                        "/api/profile/check-ins", "/api/profile/check-ins/**", "/api/profile/stats",
                        "/api/foods/{foodId}/comments", "/api/foods/{foodId}/comments/**",
                        "/api/foods/{foodId}/like", "/api/foods/{foodId}/like/**",
                        "/api/foods/{foodId}/check-ins", "/api/users/{id}").uri(community))
                .route("media", route -> route.path("/api/images", "/api/images/**", "/uploads/**").uri(media))
                .route("core", route -> route.path("/api/**").uri(core))
                .build();
    }
}

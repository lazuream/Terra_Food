package com.dayan.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTests {
    static final HttpServer core = server("core");
    static final HttpServer media = server("media");
    static final HttpServer user = server("user");
    static final HttpServer community = server("community");
    @Autowired WebTestClient client;
    @org.springframework.boot.test.web.server.LocalServerPort int port;

    static HttpServer server(String name) {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                String body = name + "|" + exchange.getRequestURI() + "|"
                        + exchange.getRequestHeaders().getFirst("Cookie") + "|"
                        + exchange.getRequestHeaders().getFirst("X-User-Id") + "|"
                        + exchange.getRequestHeaders().getFirst("X-Forwarded-For") + "|"
                        + exchange.getRequestHeaders().getFirst("X-Expected-User-Id") + "|"
                        + exchange.getRequestHeaders().getFirst("X-CSRF-TOKEN") + "|"
                        + exchange.getRequestHeaders().getFirst("X-User-Service-Token");
                byte[] data = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Set-Cookie", "SESSION=rotated; Path=/; HttpOnly; SameSite=Lax");
                exchange.sendResponseHeaders(200, data.length);
                exchange.getResponseBody().write(data); exchange.close();
            });
            server.start(); return server;
        } catch (Exception error) { throw new ExceptionInInitializerError(error); }
    }
    @DynamicPropertySource static void routes(DynamicPropertyRegistry registry) {
        registry.add("app.routes.community", () -> "http://127.0.0.1:" + community.getAddress().getPort());
        registry.add("app.routes.core", () -> "http://127.0.0.1:" + core.getAddress().getPort());
        registry.add("app.routes.user", () -> "http://127.0.0.1:" + user.getAddress().getPort());
        registry.add("app.routes.media", () -> "http://127.0.0.1:" + media.getAddress().getPort());
    }
    @AfterAll static void stop() { core.stop(0); media.stop(0); user.stop(0); community.stop(0); }

    @Test void routesMediaAndLeavesApiPathsAndQueriesIntact() {
        for (String path : new String[]{"/api/images", "/api/images/foods/1/export", "/uploads/one.png"})
            client.get().uri(path).exchange().expectStatus().isOk().expectBody(String.class)
                    .value(body -> assertTrue(body.startsWith("media|" + path + "|"), body));
        client.get().uri("/api/foods/catalog?page=2&keyword=rice").exchange().expectStatus().isOk()
                .expectBody(String.class).value(body -> assertTrue(body.startsWith("core|/api/foods/catalog?page=2&keyword=rice|"), body));
    }
    @Test void carriesSessionCsrfAndExpectedIdentityButRemovesForgedIdentity() {
        client.post().uri("/api/profile/favorites/3").header("Cookie", "SESSION=original")
                .header("X-User-Service-Token", "forged-token").header("X-CSRF-TOKEN", "csrf").header("X-Expected-User-Id", "3")
                .header("X-User-Id", "admin").header("X-Forwarded-For", "8.8.8.8")
                .exchange().expectStatus().isOk().expectCookie().valueEquals("SESSION", "rotated")
                .expectBody(String.class).value(body -> {
                    assertTrue(body.contains("|SESSION=original|null|"), body);
                    assertFalse(body.contains("8.8.8.8"), body);
                    assertTrue(body.endsWith("|3|csrf|null"), body);
                });
    }
    @Test void routesUserOperationsWithoutCapturingCatalogOrCommunity() {
        for (String path : new String[]{"/api/auth/login", "/api/auth/csrf", "/api/admin/users", "/api/admin/users/3/role",
                "/api/profile/avatar", "/api/profile/signature", "/api/profile/display-name", "/api/achievements/me", "/api/etchings"})
            client.get().uri(path).exchange().expectStatus().isOk().expectBody(String.class)
                    .value(body -> assertTrue(body.startsWith("user|" + path + "|"), body));
        for (String path : new String[]{"/api/profile/foods", "/api/profile/footprints", "/api/profile/foods/3",
                "/api/admin/foods", "/api/foods/3", "/api/foods/catalog", "/api/food-tags"})
            client.get().uri(path).exchange().expectStatus().isOk().expectBody(String.class)
                    .value(body -> assertTrue(body.startsWith("core|" + path + "|"), body));
    }
    @Test void routesEveryCommunityPathAndMethodWithQueryUnchanged() {
        for (String path : new String[]{"/api/profile/favorites", "/api/profile/favorites/page",
                "/api/profile/favorites/3", "/api/profile/favorites/3/status",
                "/api/profile/wishlist", "/api/profile/wishlist/page", "/api/profile/wishlist/3",
                "/api/profile/wishlist/foods/3/status", "/api/profile/stats",
                "/api/profile/check-ins", "/api/profile/check-ins/3",
                "/api/foods/3/comments", "/api/foods/3/comments/page", "/api/foods/3/check-ins",
                "/api/foods/3/like", "/api/foods/3/like/status", "/api/foods/3/like/unlike", "/api/users/3"}) {
            for (var method : new org.springframework.http.HttpMethod[]{org.springframework.http.HttpMethod.GET,
                    org.springframework.http.HttpMethod.POST, org.springframework.http.HttpMethod.PATCH,
                    org.springframework.http.HttpMethod.DELETE}) {
                client.method(method).uri(path + "?page=2&version=1").exchange().expectStatus().isOk()
                        .expectBody(String.class).value(body -> assertTrue(
                                body.startsWith("community|" + path + "?page=2&version=1|"), body));
            }
        }
    }
    @Test void privateRoutesAndEncodedBypassesAreNotPublic() {
        for (String path : new String[]{"/api/internal/users/1/identity", "/api/internal/agent/recommendations", "/api/internal;param/agent", "/api//internal/agent"})
            client.get().uri(path).exchange().expectStatus().isNotFound();
        client.get().uri(java.net.URI.create("http://localhost:" + port + "/api/%69nternal/agent")).exchange().expectStatus().isNotFound();
    }
}

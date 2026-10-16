package com.dayan.media;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class RemoteFoodImageLookupTests {
    HttpServer server;
    int status;
    AtomicReference<String> cookie = new AtomicReference<>();
    AtomicReference<String> identity = new AtomicReference<>();
    @BeforeEach void start() throws Exception {
        status = 200;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/foods/7", exchange -> {
            cookie.set(exchange.getRequestHeaders().getFirst("Cookie"));
            identity.set(exchange.getRequestHeaders().getFirst("X-User-Id"));
            byte[] body = "{\"imageUrl\":\"/uploads/photo.png\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
    }
    @AfterEach void stop() { server.stop(0); }
    RemoteFoodImageLookup lookup() {
        var request = new MockHttpServletRequest();
        request.addHeader("Cookie", "SESSION=caller");
        request.addHeader("X-User-Id", "forged-admin");
        return new RemoteFoodImageLookup("http://127.0.0.1:" + server.getAddress().getPort(), request);
    }
    @Test void forwardsSessionAndUsesCoreVisibilityRules() {
        assertEquals("/uploads/photo.png", lookup().imageUrl(7L));
        assertEquals("SESSION=caller", cookie.get());
        assertNull(identity.get());
    }
    @Test void preservesForbiddenAndMissingResponses() {
        for (int code : new int[]{401, 403, 404}) {
            status = code;
            assertEquals(code, assertThrows(ResponseStatusException.class, () -> lookup().imageUrl(7L)).getStatusCode().value());
        }
    }
    @Test void upstreamFailureBecomesBadGatewayWithoutInternalErrorDetails() {
        status = 500;
        assertEquals(502, assertThrows(ResponseStatusException.class, () -> lookup().imageUrl(7L)).getStatusCode().value());
    }
}

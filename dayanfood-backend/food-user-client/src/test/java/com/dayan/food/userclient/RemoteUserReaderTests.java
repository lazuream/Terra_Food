package com.dayan.food.userclient;
import com.dayan.food.exception.UserServiceUnavailableException;
import com.dayan.food.entity.enums.UserRole;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
class RemoteUserReaderTests {
    HttpServer server;
    int status=200;
    String body="{\"subjectId\":\"subject-7\",\"authVersion\":2,\"active\":true,\"role\":\"USER\"}";
    static final String TOKEN="test-service-token-01234567890123456789";
    AtomicReference<String> token=new AtomicReference<>(), cookie=new AtomicReference<>();
    @BeforeEach void start() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/internal/users/7", exchange -> {
            token.set(exchange.getRequestHeaders().getFirst("X-User-Service-Token"));
            cookie.set(exchange.getRequestHeaders().getFirst("Cookie"));
            byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type","application/json");
            exchange.getResponseHeaders().add("Location","http://127.0.0.1:1/never-follow");
            exchange.sendResponseHeaders(status,bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
    }
    @AfterEach void stop() { server.stop(0); }
    RemoteUserReader reader() { return new RemoteUserReader("http://127.0.0.1:"+server.getAddress().getPort(),TOKEN); }
    @Test void readsIdentityWithServiceTokenWithoutForwardingUserSession() {
        var account=reader().findStatus(7L);
        assertEquals("subject-7",account.subjectId()); assertEquals(2,account.authVersion());
        assertEquals(UserRole.USER,account.role()); assertTrue(account.active());
        assertEquals(TOKEN,token.get()); assertNull(cookie.get());
    }
    @Test void distinguishesDeletedAccountFromServiceFailure() {
        status=404; assertNull(reader().findStatus(7L));
        assertEquals(404,assertThrows(ResponseStatusException.class,()->reader().getPublicProfile(7L)).getStatusCode().value());
        for(int code:new int[]{401,403,500,302}) {
            status=code;
            assertThrows(UserServiceUnavailableException.class,()->reader().findStatus(7L));
            assertEquals(503,assertThrows(ResponseStatusException.class,()->reader().getPublicProfile(7L)).getStatusCode().value());
        }
    }
    @Test void malformedOrUnavailableUpstreamFailsClosed() {
        body="not json"; assertThrows(UserServiceUnavailableException.class,()->reader().findStatus(7L));
        body="{}"; assertThrows(UserServiceUnavailableException.class,()->reader().findStatus(7L));
        body="null"; assertThrows(UserServiceUnavailableException.class,()->reader().findStatus(7L));
        server.stop(0); assertThrows(UserServiceUnavailableException.class,()->reader().findStatus(7L));
    }
    @Test void rejectsBlankOrShortServiceToken() {
        assertThrows(IllegalArgumentException.class,()->new RemoteUserReader("http://localhost", "short"));
        assertThrows(IllegalArgumentException.class,()->new RemoteUserReader("http://localhost", " ".repeat(32)));
    }
}

package com.dayan.food.userclient;
import com.dayan.food.entity.vo.*;
import com.dayan.food.exception.UserServiceUnavailableException;
import com.dayan.food.service.AccountStatusReader;
import com.dayan.food.service.PublicUserProfileReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import java.net.http.HttpClient;
import java.time.Duration;
@Service
@ConditionalOnProperty(name="app.user-service.remote", havingValue="true")
public class RemoteUserReader implements AccountStatusReader, PublicUserProfileReader {
    private final RestClient client;
    public RemoteUserReader(@Value("${app.user-service.url}") String url, @Value("${app.user-service.token}") String token) {
        if (token == null || token.isBlank() || token.length() < 32) throw new IllegalArgumentException("USER_SERVICE_TOKEN must contain at least 32 characters");
        var transport=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build();
        var factory=new JdkClientHttpRequestFactory(transport);
        factory.setReadTimeout(Duration.ofSeconds(3));
        client=RestClient.builder().baseUrl(url).requestFactory(factory).defaultHeader("X-User-Service-Token", token).build();
    }
    private <T> T read(Long id, String resource, Class<T> type) {
        try {
            T value=client.get().uri("/api/internal/users/{id}/{resource}",id,resource).retrieve()
                    .onStatus(status -> status.is3xxRedirection(), (request,response) -> { throw new IllegalStateException("Unexpected redirect"); })
                    .body(type);
            if (value == null) throw new IllegalStateException("Empty user response");
            if (value instanceof AccountStatusVO account && (account.subjectId() == null
                    || account.subjectId().isBlank() || account.role() == null))
                throw new IllegalStateException("Incomplete account status");
            if (value instanceof PublicUserProfileVO profile && (!id.equals(profile.id()) || profile.username() == null))
                throw new IllegalStateException("Invalid public profile");
            return value;
        } catch (HttpClientErrorException.NotFound missing) { throw missing;
        } catch (Exception unavailable) { throw new UserServiceUnavailableException(unavailable); }
    }
    public AccountStatusVO findStatus(Long id) {
        try { return read(id,"identity",AccountStatusVO.class); }
        catch (HttpClientErrorException.NotFound missing) { return null; }
    }
    public PublicUserProfileVO getPublicProfile(Long id) {
        try { return read(id,"public-profile",PublicUserProfileVO.class); }
        catch (HttpClientErrorException.NotFound missing) { throw new ResponseStatusException(HttpStatus.NOT_FOUND,"用户不存在"); }
        catch (UserServiceUnavailableException unavailable) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"用户服务暂时不可用",unavailable); }
    }
}

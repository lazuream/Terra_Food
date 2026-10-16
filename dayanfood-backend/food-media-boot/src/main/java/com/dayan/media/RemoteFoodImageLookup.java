package com.dayan.media;

import com.dayan.food.entity.vo.FoodVO;
import com.dayan.food.service.FoodImageLookup;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;

/** Core retains visibility rules. Only the caller's session is forwarded, never a claimed user ID. */
@Service
public class RemoteFoodImageLookup implements FoodImageLookup {
    private final RestClient client;
    private final HttpServletRequest request;

    public RemoteFoodImageLookup(@Value("${app.media.core-url}") String coreUrl, HttpServletRequest request) {
        var transport = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        var factory = new JdkClientHttpRequestFactory(transport);
        factory.setReadTimeout(Duration.ofSeconds(8));
        this.client = RestClient.builder().baseUrl(coreUrl).requestFactory(factory).build();
        this.request = request;
    }

    @Override
    public String imageUrl(Long foodId) {
        try {
            FoodVO food = client.get().uri("/api/foods/{id}?recordVisit=false", foodId)
                    .headers(headers -> {
                        String cookie = request.getHeader(HttpHeaders.COOKIE);
                        if (cookie != null) headers.set(HttpHeaders.COOKIE, cookie);
                    }).retrieve().body(FoodVO.class);
            if (food == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "菜品服务响应为空");
            return food.imageUrl();
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().is4xxClientError())
                throw new ResponseStatusException(error.getStatusCode(), "菜品不可访问");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "菜品服务暂不可用");
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "菜品服务暂不可用");
        }
    }
}

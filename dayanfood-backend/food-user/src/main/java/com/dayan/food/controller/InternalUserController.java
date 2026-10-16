package com.dayan.food.controller;
import com.dayan.food.entity.vo.*;
import com.dayan.food.service.AccountStatusReader;
import com.dayan.food.service.PublicUserProfileReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@RestController
@RequestMapping("/api/internal/users")
@ConditionalOnProperty(name="app.user-service.internal-enabled", havingValue="true")
public class InternalUserController {
    private final AccountStatusReader accounts;
    private final PublicUserProfileReader profiles;
    private final byte[] token;
    public InternalUserController(AccountStatusReader accounts, PublicUserProfileReader profiles,
            @Value("${app.user-service.token}") String token) {
        if (token == null || token.isBlank() || token.length() < 32) throw new IllegalArgumentException("USER_SERVICE_TOKEN must contain at least 32 characters");
        this.accounts=accounts; this.profiles=profiles; this.token=token.getBytes(StandardCharsets.UTF_8);
    }
    private void authorize(String supplied) {
        if (supplied == null || !MessageDigest.isEqual(token, supplied.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    @GetMapping("/{id}/identity")
    public AccountStatusVO identity(@PathVariable Long id, @RequestHeader(value="X-User-Service-Token", required=false) String supplied) {
        authorize(supplied);
        var status=accounts.findStatus(id);
        if (status == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return status;
    }
    @GetMapping("/{id}/public-profile")
    public PublicUserProfileVO profile(@PathVariable Long id, @RequestHeader(value="X-User-Service-Token", required=false) String supplied) {
        authorize(supplied); return profiles.getPublicProfile(id);
    }
}

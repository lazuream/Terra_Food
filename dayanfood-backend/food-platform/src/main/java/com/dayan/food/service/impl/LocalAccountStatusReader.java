package com.dayan.food.service.impl;
import com.dayan.food.mapper.AppUserMapper;
import com.dayan.food.service.AccountStatusReader;
import com.dayan.food.entity.vo.AccountStatusVO;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Service
@ConditionalOnProperty(name="app.user-service.remote", havingValue="false", matchIfMissing=true)
public class LocalAccountStatusReader implements AccountStatusReader {
    private final AppUserMapper users;
    public LocalAccountStatusReader(AppUserMapper users) { this.users = users; }
    public AccountStatusVO findStatus(Long id) { return users.findAccountStatus(id); }
}

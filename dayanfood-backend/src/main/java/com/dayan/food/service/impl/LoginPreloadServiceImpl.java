package com.dayan.food.service.impl;

import com.dayan.food.service.AuthService;
import com.dayan.food.service.FoodService;
import com.dayan.food.service.LoginPreloadService;
import com.dayan.food.service.WishlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 通过 FoodService 代理触发全国地图标记的 Redis 缓存。
 * 用户身份由 Spring Session 在同一次登录请求结束时写入 Redis。
 */
@Service
public class LoginPreloadServiceImpl implements LoginPreloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginPreloadServiceImpl.class);

    private final AuthService authService;
    private final FoodService foodService;
    private final WishlistService wishlistService;

    public LoginPreloadServiceImpl(
            AuthService authService,
            FoodService foodService,
            WishlistService wishlistService
    ) {
        this.authService = authService;
        this.foodService = foodService;
        this.wishlistService = wishlistService;
    }

    @Override
    public void preload(String username) {
        try {
            authService.currentUser(username);
            foodService.markers(null, null, null, null, null, null);
            // 想吃清单匹配在登录读条内完成，新审核菜品会在下一次登录时出现。
            wishlistService.list(username);
        } catch (RuntimeException exception) {
            // 初始化是性能优化而不是登录门禁；Redis 或数据库短时故障不能锁住用户。
            LOGGER.warn("登录时预热用户资料、地图菜品或想吃清单匹配失败，用户仍可继续进入系统", exception);
        }
    }
}

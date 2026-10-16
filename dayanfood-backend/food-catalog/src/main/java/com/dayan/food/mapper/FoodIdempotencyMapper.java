package com.dayan.food.mapper;

import com.dayan.food.entity.po.FoodCreateIdempotency;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface FoodIdempotencyMapper {
    FoodCreateIdempotency find(@Param("userId") Long userId, @Param("key") String key);
    int deleteExpired(@Param("userId") Long userId, @Param("key") String key, @Param("now") LocalDateTime now);
    int insertReservation(@Param("userId") Long userId, @Param("key") String key,
                          @Param("requestHash") String requestHash, @Param("expiresAt") LocalDateTime expiresAt);
    int attachFood(@Param("userId") Long userId, @Param("key") String key, @Param("foodId") Long foodId);
}

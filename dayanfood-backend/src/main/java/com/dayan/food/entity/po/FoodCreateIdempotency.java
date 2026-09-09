package com.dayan.food.entity.po;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class FoodCreateIdempotency {
    private Long userId;
    private String key;
    private String requestHash;
    private Long foodId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}

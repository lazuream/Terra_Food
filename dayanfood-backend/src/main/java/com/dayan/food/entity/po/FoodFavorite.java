package com.dayan.food.entity.po;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class FoodFavorite {
    private Long userId;
    private Long foodId;
    private LocalDateTime createdAt;

    public FoodFavorite(Long userId, Long foodId) {
        this.userId = userId;
        this.foodId = foodId;
        this.createdAt = LocalDateTime.now();
    }
}

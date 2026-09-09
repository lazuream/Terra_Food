package com.dayan.food.entity.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FoodCheckinVO(
        Long id,
        Long foodId,
        String foodName,
        LocalDate eatenOn,
        String note,
        String visibility,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

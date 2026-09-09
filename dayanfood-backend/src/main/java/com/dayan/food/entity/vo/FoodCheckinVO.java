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
        String timezone,
        Integer version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FoodCheckinVO from(com.dayan.food.entity.po.FoodCheckin checkin) {
        return new FoodCheckinVO(checkin.getId(), checkin.getFoodId(), checkin.getFoodName(),
                checkin.getEatenOn(), checkin.getNote(), checkin.getVisibility(), checkin.getTimezone(),
                checkin.getVersion(), checkin.getCreatedAt(), checkin.getUpdatedAt());
    }
}

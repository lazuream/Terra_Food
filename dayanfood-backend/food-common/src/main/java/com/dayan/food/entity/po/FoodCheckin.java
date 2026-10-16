package com.dayan.food.entity.po;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class FoodCheckin {
    private Long id;
    private Long foodId;
    private Long userId;
    private String foodName;
    private LocalDate eatenOn;
    private String note;
    private String visibility;
    private Long commentId;
    private String timezone;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FoodCheckin(Long foodId, Long userId, String foodName, LocalDate eatenOn, String note,
                       String visibility, Long commentId, String timezone) {
        this.foodId = foodId;
        this.userId = userId;
        this.foodName = foodName;
        this.eatenOn = eatenOn;
        this.note = note;
        this.visibility = visibility;
        this.commentId = commentId;
        this.timezone = timezone;
        this.version = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
}

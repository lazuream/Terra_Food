package com.dayan.food.entity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record FoodCheckinUpdateDTO(
        @NotNull LocalDate eatenOn,
        @Size(max = 500) String note,
        @NotNull @Pattern(regexp = "PUBLIC|PRIVATE", message = "可见性必须为 PUBLIC 或 PRIVATE") String visibility,
        @Size(max = 64) String timezone,
        @NotNull @Min(0) Integer version
) {
    public String normalizedTimezone() {
        return timezone == null || timezone.isBlank() ? "Asia/Shanghai" : timezone.trim();
    }
}

package com.dayan.food.entity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record FoodCheckinCreateDTO(
        @NotNull LocalDate eatenOn,
        @Size(max = 500) String note,
        @Pattern(regexp = "PUBLIC|PRIVATE", message = "可见性必须为 PUBLIC 或 PRIVATE") String visibility,
        @Size(max = 64) String timezone
) {
    public String normalizedVisibility() {
        return "PRIVATE".equalsIgnoreCase(visibility) ? "PRIVATE" : "PUBLIC";
    }

    public String normalizedTimezone() {
        return timezone == null || timezone.isBlank() ? "Asia/Shanghai" : timezone.trim();
    }
}

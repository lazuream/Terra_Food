package com.dayan.food.entity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record FoodCheckinCreateDTO(
        @NotNull LocalDate eatenOn,
        @Size(max = 500) String note,
        String visibility
) {
    public String normalizedVisibility() {
        return "PRIVATE".equalsIgnoreCase(visibility) ? "PRIVATE" : "PUBLIC";
    }
}

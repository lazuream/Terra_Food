package com.dayan.food.entity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FoodTagMergeDTO(@NotNull Long targetId, @NotNull @Min(0) Integer version) {}

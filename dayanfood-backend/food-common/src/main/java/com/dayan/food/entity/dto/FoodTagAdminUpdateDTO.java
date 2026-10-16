package com.dayan.food.entity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FoodTagAdminUpdateDTO(@NotBlank String type, @NotBlank @Size(max=30) String name,
                                    @NotBlank String status, @Size(max=300) String reason,
                                    @NotNull @Min(0) Integer version) {}

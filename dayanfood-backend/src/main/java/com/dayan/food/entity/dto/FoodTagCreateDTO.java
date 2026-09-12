package com.dayan.food.entity.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record FoodTagCreateDTO(@NotNull String type, @NotBlank @Size(max=30) String name) {}

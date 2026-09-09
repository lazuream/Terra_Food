package com.dayan.food.entity.vo;

import java.time.LocalDateTime;

public record FoodTagVO(Long id, String type, String name, String status, Long mergedIntoId,
                        Integer version, Long createdBy, LocalDateTime createdAt, LocalDateTime reviewedAt) {}

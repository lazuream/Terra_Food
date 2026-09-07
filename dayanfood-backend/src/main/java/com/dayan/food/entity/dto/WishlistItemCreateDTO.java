package com.dayan.food.entity.dto;

import jakarta.validation.constraints.Size;

public record WishlistItemCreateDTO(
        @Size(min = 2, max = 100, message = "想吃内容需为 2 至 100 个字符") String content,
        Long foodId
) {
}

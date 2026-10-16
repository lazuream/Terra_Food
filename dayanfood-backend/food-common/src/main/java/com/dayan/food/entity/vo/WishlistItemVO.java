package com.dayan.food.entity.vo;

import java.time.LocalDateTime;
import java.util.List;

public record WishlistItemVO(
        Long id,
        String content,
        Long sourceFoodId,
        LocalDateTime createdAt,
        List<WishlistMatchVO> matches
) {
}

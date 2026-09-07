package com.dayan.food.entity.po;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class WishlistItem {
    private Long id;
    private Long userId;
    private String content;
    private String normalizedContent;
    private Long sourceFoodId;
    private LocalDateTime createdAt;

    public WishlistItem(Long userId, String content, String normalizedContent, Long sourceFoodId) {
        this.userId = userId;
        this.content = content;
        this.normalizedContent = normalizedContent;
        this.sourceFoodId = sourceFoodId;
        this.createdAt = LocalDateTime.now();
    }
}

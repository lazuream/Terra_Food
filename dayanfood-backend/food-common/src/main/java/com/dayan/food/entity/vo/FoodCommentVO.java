package com.dayan.food.entity.vo;

import com.dayan.food.entity.po.FoodComment;

import java.time.LocalDateTime;
import java.time.LocalDate;

public record FoodCommentVO(
        Long id,
        Long foodId,
        UserSummaryVO author,
        String content,
        LocalDateTime createdAt,
        Long checkinId,
        LocalDate eatenOn
) {

    public static FoodCommentVO from(FoodComment comment) {
        return new FoodCommentVO(
                comment.getId(),
                comment.getFoodId(),
                UserSummaryVO.from(comment.getAuthor()),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getCheckinId(),
                comment.getEatenOn()
        );
    }
}

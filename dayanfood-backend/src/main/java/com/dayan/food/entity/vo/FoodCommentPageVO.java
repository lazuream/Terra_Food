package com.dayan.food.entity.vo;

import java.util.List;

public record FoodCommentPageVO(List<FoodCommentVO> items, int total, int page, int pageSize) {
}

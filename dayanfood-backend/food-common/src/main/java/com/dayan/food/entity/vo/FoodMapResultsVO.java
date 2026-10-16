package com.dayan.food.entity.vo;

import java.util.List;

public record FoodMapResultsVO(List<FoodMarkerVO> items, int total, boolean truncated) {
}

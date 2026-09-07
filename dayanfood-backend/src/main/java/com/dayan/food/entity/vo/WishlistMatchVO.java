package com.dayan.food.entity.vo;

import java.util.List;

public record WishlistMatchVO(FoodVO food, int score, List<String> matchedFields) {
}

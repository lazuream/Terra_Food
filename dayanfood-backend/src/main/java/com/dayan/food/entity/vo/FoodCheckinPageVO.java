package com.dayan.food.entity.vo;

import java.util.List;

public record FoodCheckinPageVO(List<FoodCheckinVO> items, int total, int page, int pageSize) {}

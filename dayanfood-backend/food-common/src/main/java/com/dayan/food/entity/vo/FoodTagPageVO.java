package com.dayan.food.entity.vo;
import java.util.List;
public record FoodTagPageVO(List<FoodTagVO> items, int total, int page, int pageSize) {}

package com.dayan.food.service.impl;
import com.dayan.food.service.FoodImageLookup;
import com.dayan.food.service.FoodService;
import org.springframework.stereotype.Service;
@Service
public class LocalFoodImageLookup implements FoodImageLookup {
 private final FoodService foods;
 public LocalFoodImageLookup(FoodService foods) { this.foods = foods; }
 @Override public String imageUrl(Long foodId) { return foods.detail(foodId).imageUrl(); }
}

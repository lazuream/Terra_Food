package com.dayan.food.service;

import com.dayan.food.entity.dto.FoodCreateDTO;
import com.dayan.food.entity.vo.FoodVO;

public interface FoodCreationService {
    FoodVO create(FoodCreateDTO request, String username, String idempotencyKey);
}

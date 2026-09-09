package com.dayan.food.service;

import com.dayan.food.entity.dto.FoodCheckinCreateDTO;
import com.dayan.food.entity.vo.FoodCheckinVO;
import java.util.List;

public interface FoodCheckinService {
    FoodCheckinVO create(Long foodId, FoodCheckinCreateDTO request, String username);
    List<FoodCheckinVO> listMine(String username, int limit);
    void deleteMine(Long id, String username);
}

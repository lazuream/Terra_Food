package com.dayan.food.service;

import com.dayan.food.entity.dto.FoodCheckinCreateDTO;
import com.dayan.food.entity.vo.FoodCheckinVO;
import com.dayan.food.entity.dto.FoodCheckinUpdateDTO;
import com.dayan.food.entity.vo.FoodCheckinPageVO;
import java.time.LocalDate;

public interface FoodCheckinService {
    FoodCheckinVO create(Long foodId, FoodCheckinCreateDTO request, String username, String idempotencyKey);
    FoodCheckinPageVO listMine(String username, int page, int pageSize, String visibility, LocalDate from, LocalDate to);
    FoodCheckinVO getMine(Long id, String username);
    FoodCheckinVO updateMine(Long id, FoodCheckinUpdateDTO request, String username);
    void deleteMine(Long id, int expectedVersion, String username);
}

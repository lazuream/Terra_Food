package com.dayan.food.service;
import com.dayan.food.entity.dto.FoodTagCreateDTO;
import com.dayan.food.entity.vo.FoodTagVO;
import java.util.List;
public interface FoodTagService { List<FoodTagVO> list(String type, String keyword); FoodTagVO create(FoodTagCreateDTO request, String username); }

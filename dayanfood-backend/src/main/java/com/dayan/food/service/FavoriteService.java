package com.dayan.food.service;

import com.dayan.food.entity.vo.FavoriteStatusVO;
import com.dayan.food.entity.vo.FoodVO;

import java.util.List;

public interface FavoriteService {
    List<FoodVO> list(String username);
    FavoriteStatusVO status(Long foodId, String username);
    FavoriteStatusVO add(Long foodId, String username);
    FavoriteStatusVO remove(Long foodId, String username);
}

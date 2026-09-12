package com.dayan.food.service;

import com.dayan.food.entity.vo.FavoriteStatusVO;
import com.dayan.food.entity.vo.FoodVO;
import com.dayan.food.entity.vo.FoodCatalogVO;

import java.util.List;

public interface FavoriteService {
    List<FoodVO> list(String username);
    FoodCatalogVO page(String username, int page, int pageSize);
    FavoriteStatusVO status(Long foodId, String username);
    FavoriteStatusVO add(Long foodId, String username);
    FavoriteStatusVO remove(Long foodId, String username);
}

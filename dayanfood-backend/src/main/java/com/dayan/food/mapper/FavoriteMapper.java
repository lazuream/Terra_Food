package com.dayan.food.mapper;

import com.dayan.food.entity.po.Food;
import com.dayan.food.entity.po.FoodFavorite;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FavoriteMapper {
    int insertIgnore(FoodFavorite favorite);
    int delete(@Param("userId") Long userId, @Param("foodId") Long foodId);
    int exists(@Param("userId") Long userId, @Param("foodId") Long foodId);
    List<Food> findByUserId(@Param("userId") Long userId);
    List<Food> findPageByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("pageSize") int pageSize);
    int countByUserId(@Param("userId") Long userId);
}

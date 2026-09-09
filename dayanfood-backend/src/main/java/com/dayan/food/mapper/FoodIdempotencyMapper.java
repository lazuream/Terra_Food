package com.dayan.food.mapper;
import org.apache.ibatis.annotations.Param;
public interface FoodIdempotencyMapper {
  Long findFoodId(@Param("userId") Long userId, @Param("key") String key);
  int insert(@Param("userId") Long userId, @Param("key") String key, @Param("foodId") Long foodId);
}

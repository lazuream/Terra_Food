package com.dayan.food.mapper;

import com.dayan.food.entity.po.FoodComment;

import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface FoodCommentMapper {

    List<FoodComment> findByFoodId(Long foodId);
    List<FoodComment> findPageByFoodId(@Param("foodId") Long foodId, @Param("offset") int offset, @Param("pageSize") int pageSize);
    int countByFoodId(@Param("foodId") Long foodId);

    int insert(FoodComment comment);
    int updateOwned(@Param("id") Long id, @Param("userId") Long userId,
                    @Param("content") String content);
    int deleteOwned(@Param("id") Long id, @Param("userId") Long userId);
}

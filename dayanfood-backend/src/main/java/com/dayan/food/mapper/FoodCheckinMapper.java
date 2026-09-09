package com.dayan.food.mapper;

import com.dayan.food.entity.vo.FoodCheckinVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface FoodCheckinMapper {
    int insert(@Param("foodId") Long foodId, @Param("userId") Long userId,
               @Param("foodName") String foodName, @Param("eatenOn") java.time.LocalDate eatenOn,
               @Param("note") String note, @Param("visibility") String visibility);
    List<FoodCheckinVO> findByUser(@Param("userId") Long userId, @Param("limit") int limit);
    FoodCheckinVO findOwned(@Param("id") Long id, @Param("userId") Long userId);
    int deleteOwned(@Param("id") Long id, @Param("userId") Long userId);
}

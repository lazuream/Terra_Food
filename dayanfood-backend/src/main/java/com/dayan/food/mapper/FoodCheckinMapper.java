package com.dayan.food.mapper;

import com.dayan.food.entity.po.FoodCheckin;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface FoodCheckinMapper {
    int insert(FoodCheckin checkin);
    List<FoodCheckin> findByUser(@Param("userId") Long userId, @Param("visibility") String visibility,
                                 @Param("from") java.time.LocalDate from, @Param("to") java.time.LocalDate to,
                                 @Param("offset") int offset, @Param("limit") int limit);
    int countByUser(@Param("userId") Long userId, @Param("visibility") String visibility,
                    @Param("from") java.time.LocalDate from, @Param("to") java.time.LocalDate to);
    FoodCheckin findOwned(@Param("id") Long id, @Param("userId") Long userId);
    int updateOwned(@Param("id") Long id, @Param("userId") Long userId,
                    @Param("eatenOn") java.time.LocalDate eatenOn, @Param("note") String note,
                    @Param("visibility") String visibility, @Param("commentId") Long commentId,
                    @Param("timezone") String timezone, @Param("version") int version);
    int deleteOwned(@Param("id") Long id, @Param("userId") Long userId);
}

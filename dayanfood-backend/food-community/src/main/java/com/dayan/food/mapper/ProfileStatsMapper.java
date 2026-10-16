package com.dayan.food.mapper;
import com.dayan.food.entity.po.ProfileStats;
import org.apache.ibatis.annotations.Param;

public interface ProfileStatsMapper {
    ProfileStats findByUserId(@Param("userId") Long userId);
}

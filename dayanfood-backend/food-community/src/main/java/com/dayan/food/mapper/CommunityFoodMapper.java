package com.dayan.food.mapper;

import com.dayan.food.entity.po.Food;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** Read-only catalog projection in the current shared schema; participates in community transactions. */
public interface CommunityFoodMapper {
    Food findById(@Param("id") Long id);
    List<Food> findApprovedByIds(@Param("ids") List<Long> ids);
    List<Food> findApprovedByCreatedBy(@Param("userId") Long userId, @Param("limit") int limit);
    List<Food> findMatchingCandidates(@Param("tokens") List<String> tokens, @Param("limit") int limit);
    long currentDiscoveryVersion();
}

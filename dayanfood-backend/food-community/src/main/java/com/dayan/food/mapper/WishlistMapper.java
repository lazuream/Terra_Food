package com.dayan.food.mapper;

import com.dayan.food.entity.po.WishlistItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WishlistMapper {
    int insertIgnore(WishlistItem item);
    List<WishlistItem> findByUserId(@Param("userId") Long userId);
    List<WishlistItem> findPageByUserId(@Param("userId") Long userId, @Param("offset") int offset,
                                        @Param("pageSize") int pageSize);
    int countByUserId(@Param("userId") Long userId);
    int existsBySourceFood(@Param("userId") Long userId, @Param("foodId") Long foodId);
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}

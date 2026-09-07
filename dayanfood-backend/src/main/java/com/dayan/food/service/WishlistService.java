package com.dayan.food.service;

import com.dayan.food.entity.vo.WishlistItemVO;
import com.dayan.food.entity.vo.WishlistStatusVO;

import java.util.List;

public interface WishlistService {
    List<WishlistItemVO> list(String username);
    WishlistStatusVO status(Long foodId, String username);
    WishlistItemVO create(String content, Long foodId, String username);
    void delete(Long id, String username);
}

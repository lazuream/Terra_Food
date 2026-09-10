package com.dayan.food.service;

import com.dayan.food.entity.vo.WishlistItemVO;
import com.dayan.food.entity.vo.WishlistStatusVO;
import com.dayan.food.entity.vo.WishlistPageVO;

import java.util.List;

public interface WishlistService {
    List<WishlistItemVO> list(String username);
    WishlistPageVO page(String username, int page, int pageSize);
    WishlistStatusVO status(Long foodId, String username);
    WishlistItemVO create(String content, Long foodId, String username);
    void delete(Long id, String username);
}

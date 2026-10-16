package com.dayan.food.service;

import com.dayan.food.entity.vo.FoodCommentVO;
import com.dayan.food.entity.vo.FoodCommentPageVO;

import java.util.List;

public interface FoodCommentService {

    List<FoodCommentVO> list(Long foodId);
    FoodCommentPageVO page(Long foodId, int page, int pageSize);

    FoodCommentVO create(Long foodId, String content, String username);
}

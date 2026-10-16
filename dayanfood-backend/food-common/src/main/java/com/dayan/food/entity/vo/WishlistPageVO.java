package com.dayan.food.entity.vo;

import java.util.List;

public record WishlistPageVO(List<WishlistItemVO> items, int total, int page, int pageSize) {
}

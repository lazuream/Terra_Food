package com.dayan.food.entity.vo;

import com.dayan.food.entity.po.Food;
import java.io.Serializable;

public record ImageVariantsVO(String small, String medium, String large) implements Serializable {
    public static ImageVariantsVO from(Food food) {
        if (food.getImageSmallUrl() == null && food.getImageMediumUrl() == null && food.getImageLargeUrl() == null) return null;
        return new ImageVariantsVO(food.getImageSmallUrl(), food.getImageMediumUrl(), food.getImageLargeUrl());
    }
}

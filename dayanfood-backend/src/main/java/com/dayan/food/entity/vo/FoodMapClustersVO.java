package com.dayan.food.entity.vo;

import java.util.List;
import java.io.Serializable;

public record FoodMapClustersVO(long dataVersion, int total, int zoom, List<FoodMapClusterItemVO> items) implements Serializable {
}

package com.dayan.food.entity.po;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class FoodMapClusterRow {
    private long clusterX;
    private long clusterY;
    private int itemCount;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal minLatitude;
    private BigDecimal maxLatitude;
    private BigDecimal minLongitude;
    private BigDecimal maxLongitude;
    private Long foodId;
    private String foodName;
}

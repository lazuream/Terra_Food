package com.dayan.food.entity.vo;

import com.dayan.food.entity.po.FoodMapClusterRow;
import java.math.BigDecimal;
import java.io.Serializable;

public record FoodMapClusterItemVO(String id, String kind, int count, Long foodId, String name,
        BigDecimal latitude, BigDecimal longitude, BigDecimal minLatitude, BigDecimal maxLatitude,
        BigDecimal minLongitude, BigDecimal maxLongitude) implements Serializable {
    public static FoodMapClusterItemVO from(FoodMapClusterRow row, int zoom) {
        boolean point = row.getItemCount() == 1 && row.getFoodId() != null;
        return new FoodMapClusterItemVO(zoom + ":" + row.getClusterX() + ":" + row.getClusterY(),
                point ? "POINT" : "CLUSTER", row.getItemCount(), row.getFoodId(), row.getFoodName(),
                row.getLatitude(), row.getLongitude(), row.getMinLatitude(), row.getMaxLatitude(),
                row.getMinLongitude(), row.getMaxLongitude());
    }
}

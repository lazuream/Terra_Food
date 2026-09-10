package com.dayan.food.service;

import com.dayan.food.mapper.FoodMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class DiscoveryCountService {
    private final FoodMapper mapper;
    public DiscoveryCountService(FoodMapper mapper) { this.mapper = mapper; }

    @Cacheable(cacheNames = "foodDiscoveryCounts")
    public int count(List<String> tokens, Long regionId, List<Long> tasteIds,
            List<Long> ingredientIds, List<Long> cuisineIds, BigDecimal minLatitude,
            BigDecimal maxLatitude, BigDecimal minLongitude, BigDecimal maxLongitude) {
        return mapper.countFilteredCatalog(tokens, regionId, tasteIds, ingredientIds, cuisineIds,
                minLatitude, maxLatitude, minLongitude, maxLongitude);
    }
}

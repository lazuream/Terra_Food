package com.dayan.food.cache;

import com.dayan.food.mapper.CommunityFoodMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Fresh committed catalog version keeps wishlist matches coherent across service processes. */
@Service
public class CommunityCatalogVersion {
    private final CommunityFoodMapper mapper;
    public CommunityCatalogVersion(CommunityFoodMapper mapper) { this.mapper = mapper; }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, isolation = Isolation.READ_COMMITTED)
    public long current() { return mapper.currentDiscoveryVersion(); }
}

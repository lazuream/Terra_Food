package com.dayan.food.service.impl;

import com.dayan.food.cache.CacheInvalidator;
import com.dayan.food.entity.dto.FoodUpdateDTO;
import com.dayan.food.entity.enums.FoodReviewStatus;
import com.dayan.food.entity.enums.UserRole;
import com.dayan.food.entity.po.AppUser;
import com.dayan.food.entity.po.Food;
import com.dayan.food.entity.po.Region;
import com.dayan.food.mapper.AppUserMapper;
import com.dayan.food.mapper.FoodMapper;
import com.dayan.food.mapper.RegionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FoodEditReviewTests {
    private final FoodMapper foods = mock(FoodMapper.class);
    private final RegionMapper regions = mock(RegionMapper.class);
    private final AppUserMapper users = mock(AppUserMapper.class);
    private final ConcurrentMapCacheManager caches = new ConcurrentMapCacheManager();
    private final FoodServiceImpl service = new FoodServiceImpl(foods, regions, users, caches, new CacheInvalidator());

    private FoodUpdateDTO request(Long regionId) {
        return new FoodUpdateDTO("Corrected", regionId, BigDecimal.ONE, BigDecimal.TEN,
                "Address", "Summary", "Story", "Ingredients", null, null);
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void everyRoleMustResubmitAndInvalidatePublicCaches(UserRole role) {
        when(users.findByUsername("owner")).thenReturn(new AppUser("owner", "unused", "Owner", role));
        Food original = new Food("Original", null, BigDecimal.ONE, BigDecimal.TEN,
                "Address", "Summary", "Story", "Ingredients", null, null, "owner", FoodReviewStatus.APPROVED);
        Food pending = new Food("Corrected", null, BigDecimal.ONE, BigDecimal.TEN,
                "Address", "Summary", "Story", "Ingredients", null, null, "owner", FoodReviewStatus.PENDING);
        when(foods.findOwnedById(7L, "owner")).thenReturn(original, pending);
        when(foods.updateOwnedDetails(7L, "owner", "Corrected", null, BigDecimal.ONE, BigDecimal.TEN,
                "Address", "Summary", "Story", "Ingredients", null, null, FoodReviewStatus.PENDING, null)).thenReturn(1);
        for (String cache : new String[]{"foodDetails", "foodLists", "foodCatalogs", "foodMarkers", "wishlistMatchCatalog"}) {
            caches.getCache(cache).put(7L, original);
        }
        assertEquals(FoodReviewStatus.PENDING, service.updateMine(7L, request(null), "owner").reviewStatus());
        for (String cache : new String[]{"foodDetails", "foodLists", "foodCatalogs", "foodMarkers", "wishlistMatchCatalog"}) {
            assertNull(caches.getCache(cache).get(7L), cache);
        }
        verify(foods, never()).updateLocationLabels(anyLong(), any(), any());
    }

    @Test
    void cannotEditAnotherUsersDish() {
        when(users.findByUsername("owner")).thenReturn(new AppUser("owner", "unused", "Owner", UserRole.ADMIN));
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.updateMine(7L, request(null), "owner")).getStatusCode().value());
        verify(foods).findOwnedById(7L, "owner");
        verifyNoMoreInteractions(foods);
    }

    @Test
    void changedRegionClearsImportedLabels() {
        when(users.findByUsername("owner")).thenReturn(new AppUser("owner", "unused", "Owner", UserRole.USER));
        Region region = new Region("New city", "New province", "");
        ReflectionTestUtils.setField(region, "id", 2L);
        when(regions.findById(2L)).thenReturn(region);
        Food original = new Food("Original", null, BigDecimal.ONE, BigDecimal.TEN,
                "Address", "Summary", "Story", "Ingredients", null, null, "owner", FoodReviewStatus.PENDING);
        when(foods.findOwnedById(7L, "owner")).thenReturn(original);
        when(foods.updateOwnedDetails(7L, "owner", "Corrected", 2L, BigDecimal.ONE, BigDecimal.TEN,
                "Address", "Summary", "Story", "Ingredients", null, null, FoodReviewStatus.PENDING, null)).thenReturn(1);
        service.updateMine(7L, request(2L), "owner");
        verify(foods).updateLocationLabels(7L, null, null);
    }
}

package com.dayan.food.service.impl;

import com.dayan.food.entity.enums.FoodReviewStatus;
import com.dayan.food.entity.po.AppUser;
import com.dayan.food.entity.po.WishlistItem;
import com.dayan.food.entity.vo.FoodVO;
import com.dayan.food.entity.vo.RegionVO;
import com.dayan.food.entity.vo.WishlistItemVO;
import com.dayan.food.mapper.AppUserMapper;
import com.dayan.food.mapper.FoodMapper;
import com.dayan.food.mapper.WishlistMapper;
import com.dayan.food.service.FoodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTests {
    private static final long USER_ID = 7L;
    private static final String USERNAME = "reader";

    @Mock private WishlistMapper wishlistMapper;
    @Mock private AppUserMapper appUserMapper;
    @Mock private FoodMapper foodMapper;
    @Mock private FoodService foodService;

    private WishlistServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WishlistServiceImpl(wishlistMapper, appUserMapper, foodMapper, foodService);
        AppUser user = activeUser();
        when(appUserMapper.findByUsername(USERNAME)).thenReturn(user);
    }

    @Test
    void listFindsSimilarApprovedDishesFromChineseKeywords() {
        WishlistItem item = mock(WishlistItem.class);
        when(item.getId()).thenReturn(12L);
        when(item.getContent()).thenReturn("想吃酸汤鱼");
        when(item.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(wishlistMapper.findByUserId(USER_ID)).thenReturn(List.of(item));
        when(foodService.matchingCatalog()).thenReturn(List.of(
                food(21L, "凯里酸汤鱼", "鱼、番茄、辣椒", "贵州", "凯里"),
                food(22L, "北京烤鸭", "鸭肉", "北京", "北京")
        ));

        List<WishlistItemVO> result = service.list(USERNAME);

        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().matches().size());
        assertEquals(21L, result.getFirst().matches().getFirst().food().id());
        assertTrue(result.getFirst().matches().getFirst().matchedFields().contains("NAME"));
    }

    @Test
    void createRejectsDuplicateNormalizedContent() {
        when(wishlistMapper.insertIgnore(org.mockito.ArgumentMatchers.any(WishlistItem.class))).thenReturn(0);

        assertThrows(ResponseStatusException.class, () -> service.create("  酸汤鱼  ", null, USERNAME));
    }

    @Test
    void deleteIsScopedToCurrentUser() {
        when(wishlistMapper.deleteByIdAndUserId(9L, USER_ID)).thenReturn(1);

        service.delete(9L, USERNAME);

        verify(wishlistMapper).deleteByIdAndUserId(9L, USER_ID);
    }

    private static AppUser activeUser() {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(USER_ID);
        when(user.isActive()).thenReturn(true);
        return user;
    }

    private static FoodVO food(Long id, String name, String ingredients, String province, String region) {
        return new FoodVO(
                id, name, new RegionVO(id, region, province, "", null, null),
                BigDecimal.ZERO, BigDecimal.ZERO, "", name + "简介", "", ingredients,
                null, null, 0, FoodReviewStatus.APPROVED, null, null, "author", null,
                LocalDateTime.now()
        );
    }
}

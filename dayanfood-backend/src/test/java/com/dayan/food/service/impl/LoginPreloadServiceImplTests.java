package com.dayan.food.service.impl;

import com.dayan.food.service.AuthService;
import com.dayan.food.service.FoodService;
import com.dayan.food.service.WishlistService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LoginPreloadServiceImplTests {

    @Test
    void preloadWarmsGlobalMapMarkers() {
        FoodService foodService = mock(FoodService.class);
        AuthService authService = mock(AuthService.class);
        WishlistService wishlistService = mock(WishlistService.class);

        new LoginPreloadServiceImpl(authService, foodService, wishlistService).preload("reader");

        verify(authService).currentUser("reader");
        verify(foodService).markers(null, null, null, null, null, null);
        verify(wishlistService).list("reader");
    }

    @Test
    void preloadFailureDoesNotBlockLogin() {
        FoodService foodService = mock(FoodService.class);
        AuthService authService = mock(AuthService.class);
        WishlistService wishlistService = mock(WishlistService.class);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(foodService).markers(null, null, null, null, null, null);

        assertDoesNotThrow(() ->
                new LoginPreloadServiceImpl(authService, foodService, wishlistService).preload("reader"));
    }
}

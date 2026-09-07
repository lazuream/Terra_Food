package com.dayan.food.controller;

import com.dayan.food.entity.dto.WishlistItemCreateDTO;
import com.dayan.food.entity.vo.FavoriteStatusVO;
import com.dayan.food.entity.vo.FoodVO;
import com.dayan.food.entity.vo.WishlistItemVO;
import com.dayan.food.entity.vo.WishlistStatusVO;
import com.dayan.food.service.FavoriteService;
import com.dayan.food.service.WishlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
public class CollectionController {
    private final FavoriteService favoriteService;
    private final WishlistService wishlistService;

    public CollectionController(FavoriteService favoriteService, WishlistService wishlistService) {
        this.favoriteService = favoriteService;
        this.wishlistService = wishlistService;
    }

    @GetMapping("/favorites")
    public List<FoodVO> favorites(Authentication authentication) {
        return favoriteService.list(authentication.getName());
    }

    @GetMapping("/favorites/{foodId}/status")
    public FavoriteStatusVO favoriteStatus(@PathVariable Long foodId, Authentication authentication) {
        return favoriteService.status(foodId, authentication.getName());
    }

    @PostMapping("/favorites/{foodId}")
    public FavoriteStatusVO addFavorite(@PathVariable Long foodId, Authentication authentication) {
        return favoriteService.add(foodId, authentication.getName());
    }

    @DeleteMapping("/favorites/{foodId}")
    public FavoriteStatusVO removeFavorite(@PathVariable Long foodId, Authentication authentication) {
        return favoriteService.remove(foodId, authentication.getName());
    }

    @GetMapping("/wishlist")
    public List<WishlistItemVO> wishlist(Authentication authentication) {
        return wishlistService.list(authentication.getName());
    }

    @GetMapping("/wishlist/foods/{foodId}/status")
    public WishlistStatusVO wishlistStatus(@PathVariable Long foodId, Authentication authentication) {
        return wishlistService.status(foodId, authentication.getName());
    }

    @PostMapping("/wishlist")
    @ResponseStatus(HttpStatus.CREATED)
    public WishlistItemVO addWishlist(
            @Valid @RequestBody WishlistItemCreateDTO request,
            Authentication authentication
    ) {
        return wishlistService.create(request.content(), request.foodId(), authentication.getName());
    }

    @DeleteMapping("/wishlist/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWishlist(@PathVariable Long id, Authentication authentication) {
        wishlistService.delete(id, authentication.getName());
    }
}

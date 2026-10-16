package com.dayan.food.controller;

import com.dayan.food.entity.vo.FoodPageVO;
import com.dayan.food.entity.enums.FoodReviewStatus;
import com.dayan.food.entity.dto.FoodReviewDTO;
import com.dayan.food.service.FoodService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminFoodController {
    private final FoodService foodService;
    public AdminFoodController(FoodService foodService) { this.foodService = foodService; }

    @GetMapping("/foods")
    public FoodPageVO foods(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) FoodReviewStatus status
    ) {
        return foodService.listForAdmin(page, pageSize, status);
    }

    @PatchMapping("/foods/{id}/review")
    public void reviewFood(
            @PathVariable Long id,
            @RequestBody @Valid FoodReviewDTO request,
            Authentication authentication
    ) {
        foodService.review(id, request.status(), request.expectedVersion(), authentication.getName());
    }

}

package com.dayan.food.controller;

import com.dayan.food.entity.dto.FoodCheckinCreateDTO;
import com.dayan.food.entity.vo.FoodCheckinVO;
import com.dayan.food.service.FoodCheckinService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class FoodCheckinController {
    private final FoodCheckinService service;
    public FoodCheckinController(FoodCheckinService service) { this.service = service; }
    @PostMapping("/api/foods/{foodId}/check-ins") @ResponseStatus(HttpStatus.CREATED)
    public FoodCheckinVO create(@PathVariable Long foodId, @Valid @RequestBody FoodCheckinCreateDTO request, Authentication auth) { return service.create(foodId, request, auth.getName()); }
    @GetMapping("/api/profile/check-ins")
    public List<FoodCheckinVO> mine(@RequestParam(defaultValue="20") int limit, Authentication auth) { return service.listMine(auth.getName(), limit); }
    @DeleteMapping("/api/profile/check-ins/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) { service.deleteMine(id, auth.getName()); }
}

package com.dayan.food.controller;

import com.dayan.food.entity.dto.FoodTagAdminUpdateDTO;
import com.dayan.food.entity.dto.FoodTagMergeDTO;
import com.dayan.food.entity.vo.FoodTagPageVO;
import com.dayan.food.entity.vo.FoodTagVO;
import com.dayan.food.service.FoodTagService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/food-tags")
public class AdminFoodTagController {
    private final FoodTagService service;
    public AdminFoodTagController(FoodTagService service){this.service=service;}
    @GetMapping public FoodTagPageVO list(@RequestParam(required=false) String status,@RequestParam(required=false) String type,
            @RequestParam(required=false) String keyword,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int pageSize){return service.adminList(status,type,keyword,page,pageSize);}
    @PatchMapping("/{id}") public FoodTagVO update(@PathVariable Long id,@Valid @RequestBody FoodTagAdminUpdateDTO request,Authentication auth){return service.adminUpdate(id,request,auth.getName());}
    @PostMapping("/{id}/merge") public FoodTagVO merge(@PathVariable Long id,@Valid @RequestBody FoodTagMergeDTO request,Authentication auth){return service.merge(id,request.targetId(),request.version(),auth.getName());}
}

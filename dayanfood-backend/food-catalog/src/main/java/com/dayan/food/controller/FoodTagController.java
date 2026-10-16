package com.dayan.food.controller;
import com.dayan.food.entity.dto.FoodTagCreateDTO;
import com.dayan.food.entity.vo.FoodTagVO;
import com.dayan.food.service.FoodTagService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/food-tags") public class FoodTagController {
 private final FoodTagService service; public FoodTagController(FoodTagService service){this.service=service;}
 @GetMapping public List<FoodTagVO> list(@RequestParam(required=false) String type,@RequestParam(required=false) String keyword){return service.list(type,keyword);}
 @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public FoodTagVO create(@Valid @RequestBody FoodTagCreateDTO request,Authentication auth){return service.create(request,auth.getName());}
 @GetMapping("/food/{foodId}") public List<FoodTagVO> forFood(@PathVariable Long foodId,Authentication auth){String username=auth==null||"anonymousUser".equals(auth.getName())?null:auth.getName();return service.forFood(foodId,username);}
}

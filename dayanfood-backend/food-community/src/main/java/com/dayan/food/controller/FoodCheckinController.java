package com.dayan.food.controller;

import com.dayan.food.entity.dto.FoodCheckinCreateDTO;
import com.dayan.food.entity.vo.FoodCheckinVO;
import com.dayan.food.entity.vo.FoodCheckinPageVO;
import com.dayan.food.entity.dto.FoodCheckinUpdateDTO;
import com.dayan.food.service.FoodCheckinService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
public class FoodCheckinController {
    private final FoodCheckinService service;
    public FoodCheckinController(FoodCheckinService service) { this.service = service; }
    @PostMapping("/api/foods/{foodId}/check-ins") @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<FoodCheckinVO> create(@PathVariable Long foodId, @Valid @RequestBody FoodCheckinCreateDTO request,
            @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(service.create(foodId, request, auth.getName(), idempotencyKey));
    }
    @GetMapping("/api/profile/check-ins")
    public ResponseEntity<FoodCheckinPageVO> mine(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize,
            @RequestParam(required=false) String visibility, @RequestParam(required=false) LocalDate from,
            @RequestParam(required=false) LocalDate to, Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.listMine(auth.getName(), page, pageSize, visibility, from, to));
    }
    @GetMapping("/api/profile/check-ins/{id}")
    public ResponseEntity<FoodCheckinVO> get(@PathVariable Long id, Authentication auth) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.getMine(id, auth.getName())); }
    @PatchMapping("/api/profile/check-ins/{id}")
    public ResponseEntity<FoodCheckinVO> update(@PathVariable Long id, @Valid @RequestBody FoodCheckinUpdateDTO request, Authentication auth) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.updateMine(id, request, auth.getName())); }
    @DeleteMapping("/api/profile/check-ins/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam int version, Authentication auth) {
        service.deleteMine(id, version, auth.getName());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}

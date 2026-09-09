package com.dayan.food.controller;

import com.dayan.food.entity.dto.FoodCreateDTO;
import com.dayan.food.entity.vo.FoodVO;
import com.dayan.food.entity.vo.FoodCatalogVO;
import com.dayan.food.entity.vo.FoodImportResultVO;
import com.dayan.food.entity.vo.FoodMarkerVO;
import com.dayan.food.service.FoodImportService;
import com.dayan.food.service.FoodService;
import com.dayan.food.mapper.AppUserMapper;
import com.dayan.food.mapper.FoodIdempotencyMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/foods")
public class FoodController {

    private final FoodService foodService;
    private final FoodImportService foodImportService;
    private final AppUserMapper appUserMapper;
    private final FoodIdempotencyMapper idempotencyMapper;

    public FoodController(FoodService foodService, FoodImportService foodImportService, AppUserMapper appUserMapper, FoodIdempotencyMapper idempotencyMapper) {
        this.foodService = foodService;
        this.foodImportService = foodImportService;
        this.appUserMapper = appUserMapper;
        this.idempotencyMapper = idempotencyMapper;
    }

    @GetMapping
    public List<FoodVO> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) BigDecimal minLatitude,
            @RequestParam(required = false) BigDecimal maxLatitude,
            @RequestParam(required = false) BigDecimal minLongitude,
            @RequestParam(required = false) BigDecimal maxLongitude
    ) {
        return foodService.list(
                keyword,
                regionId,
                minLatitude,
                maxLatitude,
                minLongitude,
                maxLongitude
        );
    }

    @GetMapping("/markers")
    public List<FoodMarkerVO> markers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) BigDecimal minLatitude,
            @RequestParam(required = false) BigDecimal maxLatitude,
            @RequestParam(required = false) BigDecimal minLongitude,
            @RequestParam(required = false) BigDecimal maxLongitude
    ) {
        return foodService.markers(
                keyword,
                regionId,
                minLatitude,
                maxLatitude,
                minLongitude,
                maxLongitude
        );
    }

    @GetMapping("/catalog")
    public FoodCatalogVO catalog(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long regionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int pageSize
    ) {
        return foodService.catalog(keyword, regionId, page, pageSize);
    }

    @GetMapping("/{id}")
    public FoodVO detail(@PathVariable Long id, Authentication authentication) {
        // 游客浏览（匿名 token 的 isAuthenticated 为 false）不计入热度与每日访问。
        if (authentication != null && authentication.isAuthenticated()) {
            foodService.recordVisit(id, authentication.getName());
        }
        return foodService.detail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FoodVO create(@Valid @RequestBody FoodCreateDTO request, Authentication authentication, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String username = authentication == null ? "无名" : authentication.getName();
        if (idempotencyKey != null && !idempotencyKey.isBlank() && authentication != null) {
            var user = appUserMapper.findByUsername(username);
            if (user != null) {
                String key = idempotencyKey.trim();
                if (key.length() > 100) throw new IllegalArgumentException("幂等键过长");
                Long existingId = idempotencyMapper.findFoodId(user.getId(), key);
                if (existingId != null) {
                    return foodService.listMine(username).stream().filter(food -> existingId.equals(food.id())).findFirst().orElseThrow(() -> new IllegalArgumentException("幂等记录对应的菜品不可访问"));
                }
                FoodVO created = foodService.create(request, username);
                try { idempotencyMapper.insert(user.getId(), key, created.id()); }
                catch (org.springframework.dao.DuplicateKeyException ignored) {
                    Long duplicateId = idempotencyMapper.findFoodId(user.getId(), key);
                    return foodService.listMine(username).stream().filter(food -> duplicateId != null && duplicateId.equals(food.id())).findFirst().orElseThrow(() -> new IllegalArgumentException("幂等记录对应的菜品不可访问"));
                }
                return created;
            }
        }
        return foodService.create(request, username);
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public FoodImportResultVO importSpreadsheet(@RequestParam("file") MultipartFile file) {
        return foodImportService.importSpreadsheet(file);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        foodService.delete(id);
    }
}

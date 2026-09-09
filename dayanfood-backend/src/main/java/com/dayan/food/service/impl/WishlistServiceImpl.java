package com.dayan.food.service.impl;

import com.dayan.food.entity.po.AppUser;
import com.dayan.food.entity.po.WishlistItem;
import com.dayan.food.entity.vo.FoodVO;
import com.dayan.food.entity.vo.WishlistItemVO;
import com.dayan.food.entity.vo.WishlistMatchVO;
import com.dayan.food.entity.vo.WishlistStatusVO;
import com.dayan.food.mapper.AppUserMapper;
import com.dayan.food.mapper.FoodMapper;
import com.dayan.food.mapper.WishlistMapper;
import com.dayan.food.service.FoodService;
import com.dayan.food.service.WishlistService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class WishlistServiceImpl implements WishlistService {
    private static final int MAX_MATCHES = 3;
    private static final Set<String> STOP_WORDS =
            Set.of("想吃", "想尝", "尝尝", "一道", "当地", "特色", "菜品", "美食", "的", "菜");

    private final WishlistMapper wishlistMapper;
    private final AppUserMapper appUserMapper;
    private final FoodMapper foodMapper;
    private final FoodService foodService;

    public WishlistServiceImpl(
            WishlistMapper wishlistMapper,
            AppUserMapper appUserMapper,
            FoodMapper foodMapper,
            FoodService foodService
    ) {
        this.wishlistMapper = wishlistMapper;
        this.appUserMapper = appUserMapper;
        this.foodMapper = foodMapper;
        this.foodService = foodService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemVO> list(String username) {
        AppUser user = requireUser(username);
        List<FoodVO> catalog = foodService.matchingCatalog();
        return wishlistMapper.findByUserId(user.getId()).stream()
                .map(item -> toVO(item, catalog))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistStatusVO status(Long foodId, String username) {
        AppUser user = requireUser(username);
        return new WishlistStatusVO(wishlistMapper.existsBySourceFood(user.getId(), foodId) > 0);
    }

    @Override
    @Transactional
    public WishlistItemVO create(String content, Long foodId, String username) {
        AppUser user = requireUser(username);
        String value = content == null ? "" : content.trim();
        if (foodId != null) {
            var food = foodMapper.findById(foodId);
            if (food == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "菜品不存在或尚未通过审核");
            }
            if (value.isBlank()) value = food.getName();
        }
        if (value.length() < 2 || value.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "想吃内容需为 2 至 100 个字符");
        }
        String normalized = compact(value);
        if (normalized.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写有效的菜品关键词");
        }
        WishlistItem item = new WishlistItem(user.getId(), value, normalized, foodId);
        if (wishlistMapper.insertIgnore(item) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "这条想吃内容已经在清单中");
        }
        return toVO(item, foodService.matchingCatalog());
    }

    @Override
    @Transactional
    public void delete(Long id, String username) {
        AppUser user = requireUser(username);
        if (wishlistMapper.deleteByIdAndUserId(id, user.getId()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "想吃条目不存在");
        }
    }

    private WishlistItemVO toVO(WishlistItem item, List<FoodVO> catalog) {
        List<WishlistMatchVO> matches = catalog.stream()
                .map(food -> score(item, food))
                .filter(match -> match.score() >= 8)
                .sorted(Comparator.comparingInt(WishlistMatchVO::score).reversed()
                        .thenComparing(match -> match.food().id(), Comparator.reverseOrder()))
                .limit(MAX_MATCHES)
                .toList();
        return new WishlistItemVO(
                item.getId(),
                item.getContent(),
                item.getSourceFoodId(),
                item.getCreatedAt(),
                matches
        );
    }

    private WishlistMatchVO score(WishlistItem item, FoodVO food) {
        String wish = compact(item.getContent());
        String name = compact(food.name());
        String ingredients = compact(food.ingredients());
        String summary = compact(food.summary());
        String story = compact(food.story());
        String address = compact(food.address());
        String region = compact(food.region().province() + food.region().name());
        int score = item.getSourceFoodId() != null && item.getSourceFoodId().equals(food.id()) ? 1000 : 0;
        Set<String> fields = new LinkedHashSet<>();
        if (wish.equals(name)) {
            score += 120;
            fields.add("NAME");
        } else if (name.contains(wish) || wish.contains(name)) {
            score += 60;
            fields.add("NAME");
        }
        for (String token : tokens(item.getContent())) {
            int size = token.codePointCount(0, token.length());
            if (name.contains(token)) { score += size == 1 ? 8 : 20; fields.add("NAME"); }
            if (ingredients.contains(token)) { score += size == 1 ? 4 : 10; fields.add("INGREDIENTS"); }
            if (region.contains(token)) { score += size == 1 ? 3 : 8; fields.add("REGION"); }
            if (summary.contains(token)) { score += size == 1 ? 2 : 5; fields.add("SUMMARY"); }
            if (story.contains(token)) { score += size == 1 ? 1 : 3; fields.add("STORY"); }
            if (address.contains(token)) { score += size == 1 ? 1 : 2; fields.add("ADDRESS"); }
        }
        if (score >= 1000) fields.add("DIRECT");
        return new WishlistMatchVO(food, score, List.copyOf(fields));
    }

    private Set<String> tokens(String text) {
        Set<String> result = new LinkedHashSet<>();
        for (String part : normalize(text).split(" +")) {
            if (part.isBlank() || STOP_WORDS.contains(part)) continue;
            result.add(part);
            int length = part.codePointCount(0, part.length());
            int[] points = part.codePoints().toArray();
            if (length > 2) {
                for (int size = 2; size <= Math.min(3, length); size++) {
                    for (int index = 0; index <= length - size; index++) {
                        String token = new String(points, index, size);
                        if (!STOP_WORDS.contains(token)) result.add(token);
                    }
                }
            }
        }
        return result;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsHan}\\p{L}\\p{N}]+", " ")
                .trim();
    }

    private String compact(String value) {
        return normalize(value).replace(" ", "");
    }

    private AppUser requireUser(String username) {
        AppUser user = appUserMapper.findByUsername(username);
        if (user == null || !user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录用户不存在或已停用");
        }
        return user;
    }
}

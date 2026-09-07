package com.dayan.food.service.impl;

import com.dayan.food.entity.po.AppUser;
import com.dayan.food.entity.po.FoodFavorite;
import com.dayan.food.entity.vo.FavoriteStatusVO;
import com.dayan.food.entity.vo.FoodVO;
import com.dayan.food.mapper.AppUserMapper;
import com.dayan.food.mapper.FavoriteMapper;
import com.dayan.food.mapper.FoodMapper;
import com.dayan.food.service.FavoriteService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteMapper favoriteMapper;
    private final FoodMapper foodMapper;
    private final AppUserMapper appUserMapper;

    public FavoriteServiceImpl(FavoriteMapper favoriteMapper, FoodMapper foodMapper, AppUserMapper appUserMapper) {
        this.favoriteMapper = favoriteMapper;
        this.foodMapper = foodMapper;
        this.appUserMapper = appUserMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodVO> list(String username) {
        return favoriteMapper.findByUserId(requireUser(username).getId()).stream().map(FoodVO::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FavoriteStatusVO status(Long foodId, String username) {
        AppUser user = requireUser(username);
        return new FavoriteStatusVO(favoriteMapper.exists(user.getId(), foodId) > 0);
    }

    @Override
    @Transactional
    public FavoriteStatusVO add(Long foodId, String username) {
        requireFood(foodId);
        AppUser user = requireUser(username);
        favoriteMapper.insertIgnore(new FoodFavorite(user.getId(), foodId));
        return new FavoriteStatusVO(true);
    }

    @Override
    @Transactional
    public FavoriteStatusVO remove(Long foodId, String username) {
        AppUser user = requireUser(username);
        favoriteMapper.delete(user.getId(), foodId);
        return new FavoriteStatusVO(false);
    }

    private void requireFood(Long foodId) {
        if (foodMapper.findById(foodId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "菜品不存在或尚未通过审核");
        }
    }

    private AppUser requireUser(String username) {
        AppUser user = appUserMapper.findByUsername(username);
        if (user == null || !user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录用户不存在或已停用");
        }
        return user;
    }
}

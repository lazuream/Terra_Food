package com.dayan.food.service.impl;

import com.dayan.food.entity.dto.FoodCheckinCreateDTO;
import com.dayan.food.entity.vo.FoodCheckinVO;
import com.dayan.food.mapper.AppUserMapper;
import com.dayan.food.mapper.FoodCheckinMapper;
import com.dayan.food.mapper.FoodMapper;
import com.dayan.food.service.FoodCheckinService;
import com.dayan.food.service.FoodCommentService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@Service
public class FoodCheckinServiceImpl implements FoodCheckinService {
    private final FoodCheckinMapper mapper; private final FoodMapper foods; private final AppUserMapper users; private final FoodCommentService comments;
    public FoodCheckinServiceImpl(FoodCheckinMapper mapper, FoodMapper foods, AppUserMapper users, FoodCommentService comments) { this.mapper=mapper; this.foods=foods; this.users=users; this.comments=comments; }
    @Override @Transactional
    public FoodCheckinVO create(Long foodId, FoodCheckinCreateDTO request, String username) {
        var food = foods.findById(foodId);
        if (food == null || food.getReviewStatus() != com.dayan.food.entity.enums.FoodReviewStatus.APPROVED) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "美食不存在");
        var user = users.findByUsername(username); if (user == null || !user.isActive()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录用户不存在或已停用");
        if (request.eatenOn().isAfter(LocalDate.now())) throw new IllegalArgumentException("打卡日期不能晚于今天");
        String note = request.note() == null ? null : request.note().trim();
        String visibility = request.normalizedVisibility();
        mapper.insert(foodId, user.getId(), food.getName(), request.eatenOn(), note, visibility);
        if ("PUBLIC".equals(visibility)) comments.create(foodId, note == null || note.isBlank() ? "已打卡这道菜" : note, username);
        return mapper.findByUser(user.getId(), 1).get(0);
    }
    @Override @Transactional(readOnly = true)
    public List<FoodCheckinVO> listMine(String username, int limit) {
        var user = users.findByUsername(username); if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return mapper.findByUser(user.getId(), Math.min(Math.max(limit, 1), 100));
    }
    @Override @Transactional
    public void deleteMine(Long id, String username) {
        var user = users.findByUsername(username); if (user == null || mapper.deleteOwned(id, user.getId()) == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "打卡不存在");
    }
}

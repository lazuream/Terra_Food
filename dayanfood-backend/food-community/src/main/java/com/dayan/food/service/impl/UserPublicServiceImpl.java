package com.dayan.food.service.impl;
import com.dayan.food.entity.vo.FoodVO;
import com.dayan.food.entity.vo.UserPublicVO;
import com.dayan.food.mapper.FoodMapper;
import com.dayan.food.service.PublicUserProfileReader;
import com.dayan.food.service.UserPublicService;
import org.springframework.stereotype.Service;
@Service
public class UserPublicServiceImpl implements UserPublicService {
    private final PublicUserProfileReader users;
    private final FoodMapper foods;
    public UserPublicServiceImpl(PublicUserProfileReader users, FoodMapper foods) { this.users=users; this.foods=foods; }
    public UserPublicVO getProfile(Long id) {
        var user=users.getPublicProfile(id);
        var items=foods.findApprovedByCreatedBy(user.id(), 100).stream().map(f -> FoodVO.fromSummary(f, user.summary())).toList();
        return new UserPublicVO(user.id(), user.username(), user.displayName(), user.avatarUrl(), user.signature(),
                user.selectedAchievement(), user.selectedEtching(), items);
    }
}

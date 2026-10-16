package com.dayan.food.service.impl;
import com.dayan.food.entity.vo.*;
import com.dayan.food.mapper.AppUserMapper;
import com.dayan.food.mapper.AchievementMapper;
import com.dayan.food.service.EtchingDesignService;
import com.dayan.food.service.PublicUserProfileReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class LocalPublicUserProfileReader implements PublicUserProfileReader {
    private final AppUserMapper users;
    private final AchievementMapper achievements;
    private final EtchingDesignService etchings;
    public LocalPublicUserProfileReader(AppUserMapper users, AchievementMapper achievements, EtchingDesignService etchings) {
        this.users=users; this.achievements=achievements; this.etchings=etchings;
    }
    @Transactional(readOnly=true)
    public PublicUserProfileVO getPublicProfile(Long id) {
        var user=users.findPublicById(id);
        if (user == null || !user.isActive()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        var signature=user.getSignature();
        var selected=achievements.findUnlockedByUserId(id).stream().filter(a -> a.isSelected()).findFirst().map(AchievementVO::from).orElse(null);
        return new PublicUserProfileVO(user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl(),
                signature == null || signature.isBlank() ? null : signature.trim(), selected, etchings.getSelected(id));
    }
}

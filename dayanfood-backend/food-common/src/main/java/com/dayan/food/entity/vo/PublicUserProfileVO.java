package com.dayan.food.entity.vo;
public record PublicUserProfileVO(Long id, String username, String displayName, String avatarUrl,
        String signature, AchievementVO selectedAchievement, EtchingDesignVO selectedEtching) {
    public UserSummaryVO summary() { return new UserSummaryVO(id, username, displayName, avatarUrl); }
}

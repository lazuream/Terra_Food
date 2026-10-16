package com.dayan.food.service;
import com.dayan.food.entity.vo.ProfileStatsVO;

public interface ProfileStatsService {
    ProfileStatsVO getMine(String username);
}

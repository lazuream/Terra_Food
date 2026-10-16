package com.dayan.food.controller;

import com.dayan.food.entity.vo.ProfileStatsVO;
import com.dayan.food.service.ProfileStatsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile/stats")
public class ProfileStatsController {
    private final ProfileStatsService service;

    public ProfileStatsController(ProfileStatsService service) {
        this.service = service;
    }

    @GetMapping
    public ProfileStatsVO getMine(Authentication authentication) {
        return service.getMine(authentication.getName());
    }
}

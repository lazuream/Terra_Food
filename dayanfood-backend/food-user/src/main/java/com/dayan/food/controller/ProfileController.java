package com.dayan.food.controller;

import com.dayan.food.entity.dto.AvatarUpdateDTO;
import com.dayan.food.entity.dto.DisplayNameUpdateDTO;
import com.dayan.food.entity.dto.SignatureUpdateDTO;
import com.dayan.food.entity.vo.AuthUserVO;
import com.dayan.food.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final AppUserService appUserService;

    public ProfileController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @PatchMapping("/avatar")
    public AuthUserVO updateAvatar(
            @Valid @RequestBody AvatarUpdateDTO request,
            Authentication authentication
    ) {
        return appUserService.updateAvatar(authentication.getName(), request.avatarUrl());
    }

    @PatchMapping("/signature")
    public AuthUserVO updateSignature(
            @Valid @RequestBody SignatureUpdateDTO request,
            Authentication authentication
    ) {
        return appUserService.updateSignature(authentication.getName(), request.signature());
    }

    @PatchMapping("/display-name")
    public AuthUserVO updateDisplayName(
            @Valid @RequestBody DisplayNameUpdateDTO request,
            Authentication authentication
    ) {
        return appUserService.submitDisplayName(authentication.getName(), request.displayName());
    }

}

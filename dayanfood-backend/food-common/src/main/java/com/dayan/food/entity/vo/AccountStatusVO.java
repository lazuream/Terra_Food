package com.dayan.food.entity.vo;
import com.dayan.food.entity.enums.UserRole;
public record AccountStatusVO(String subjectId, long authVersion, boolean active, UserRole role) {}

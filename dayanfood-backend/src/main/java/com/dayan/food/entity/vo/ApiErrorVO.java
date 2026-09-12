package com.dayan.food.entity.vo;

public record ApiErrorVO(String code, String message, String requestId) {
    public ApiErrorVO(String message) {
        this("REQUEST_FAILED", message, null);
    }
}

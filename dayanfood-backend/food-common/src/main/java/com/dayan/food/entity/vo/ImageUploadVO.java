package com.dayan.food.entity.vo;

public record ImageUploadVO(String url, Long resourceId, int width, int height, String status) {
    public ImageUploadVO(String url) {
        this(url, null, 0, 0, "UNKNOWN");
    }
}

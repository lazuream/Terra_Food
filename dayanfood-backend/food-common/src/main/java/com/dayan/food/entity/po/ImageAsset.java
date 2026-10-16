package com.dayan.food.entity.po;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ImageAsset {
    private Long id;
    private String originalUrl;
    private int originalWidth;
    private int originalHeight;
    private String status;
    private String variant320Url;
    private String variant640Url;
    private String variant1280Url;
    private int processorVersion;

    public ImageAsset(String originalUrl, int originalWidth, int originalHeight) {
        this.originalUrl = originalUrl;
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
        this.status = "PENDING";
        this.processorVersion = 1;
    }
}

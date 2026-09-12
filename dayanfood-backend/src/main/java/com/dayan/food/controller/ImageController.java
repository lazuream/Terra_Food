package com.dayan.food.controller;

import com.dayan.food.entity.vo.ImageUploadVO;
import com.dayan.food.service.ImageStorageService;
import com.dayan.food.mapper.ImageAssetMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageStorageService imageStorageService;
    private final ImageAssetMapper imageAssetMapper;

    public ImageController(ImageStorageService imageStorageService, ImageAssetMapper imageAssetMapper) {
        this.imageStorageService = imageStorageService;
        this.imageAssetMapper = imageAssetMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ImageUploadVO upload(@RequestParam("file") MultipartFile file) {
        String url = imageStorageService.store(file);
        var asset = imageAssetMapper.findByOriginalUrl(url);
        return new ImageUploadVO(url, asset.getId(), asset.getOriginalWidth(), asset.getOriginalHeight(), asset.getStatus());
    }
}

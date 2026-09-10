package com.dayan.food.config;

import com.dayan.food.entity.po.ImageAsset;
import com.dayan.food.mapper.ImageAssetMapper;
import com.dayan.food.image.ImageDimensions;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ImageVariantTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageVariantTask.class);
    private static final int[] SIZES = {320, 640, 1280};
    private final ImageAssetMapper mapper;
    private final Path uploadDirectory;
    private final AtomicBoolean ready = new AtomicBoolean();

    public ImageVariantTask(ImageAssetMapper mapper,
                            @Value("${app.upload-directory:uploads}") String uploadDirectory) {
        this.mapper = mapper;
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @EventListener(ApplicationReadyEvent.class)
    void recoverInterruptedWork() {
        mapper.resetProcessing();
        ready.set(true);
    }

    @Scheduled(fixedDelayString = "${app.upload.variant-delay:1000}")
    public void processNext() {
        if (!ready.get()) return;
        for (ImageAsset asset : mapper.findPending(1)) {
            if (mapper.markProcessing(asset.getId()) != 1) continue;
            try {
                process(asset);
            } catch (Exception exception) {
                deletePartialVariants(asset);
                mapper.markFailed(asset.getId(), "PROCESSING_FAILED");
                LOGGER.warn("图片派生处理失败：assetId={}", asset.getId());
            }
        }
    }

    private void process(ImageAsset asset) throws Exception {
        Path original = resolveUrl(asset.getOriginalUrl());
        ImageDimensions dimensions = ImageDimensions.read(original);
        if (dimensions.pixels() > 24_000_000L) {
            mapper.markFailed(asset.getId(), "PIXEL_LIMIT");
            return;
        }
        BufferedImage image = ImageIO.read(original.toFile());
        if (image == null) {
            mapper.markFailed(asset.getId(), "DECODE_FAILED");
            return;
        }
        String format = image.getColorModel().hasAlpha() ? "png" : "jpg";
        String base = original.getFileName().toString().replaceFirst("\\.[^.]+$", "");
        String[] urls = new String[SIZES.length];
        Path variants = uploadDirectory.resolve("variants");
        Files.createDirectories(variants);
        for (int i = 0; i < SIZES.length; i++) {
            int size = SIZES[i];
            String filename = base + "-" + size + "." + format;
            Path destination = variants.resolve(filename);
            Path temporary = Files.createTempFile(variants, ".variant-", ".tmp");
            try {
                double scale = Math.min(1d, size / (double) Math.max(image.getWidth(), image.getHeight()));
                var builder = Thumbnails.of(image).scale(scale).outputFormat(format);
                if ("jpg".equals(format)) builder.outputQuality(0.82d);
                builder.toFile(temporary.toFile());
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temporary);
            }
            urls[i] = "/uploads/variants/" + filename;
        }
        mapper.markReady(asset.getId(), urls[0], urls[1], urls[2]);
    }

    private void deletePartialVariants(ImageAsset asset) {
        try {
            Path original = resolveUrl(asset.getOriginalUrl());
            String base = original.getFileName().toString().replaceFirst("\\.[^.]+$", "");
            Path variants = uploadDirectory.resolve("variants");
            for (int size : SIZES) {
                Files.deleteIfExists(variants.resolve(base + "-" + size + ".jpg"));
                Files.deleteIfExists(variants.resolve(base + "-" + size + ".png"));
            }
        } catch (Exception ignored) {
            LOGGER.warn("图片派生残留清理失败：assetId={}", asset.getId());
        }
    }

    private Path resolveUrl(String url) {
        if (url == null || !url.startsWith("/uploads/")) throw new IllegalArgumentException("invalid image url");
        Path path = uploadDirectory.resolve(url.substring("/uploads/".length())).normalize();
        if (!path.startsWith(uploadDirectory)) throw new IllegalArgumentException("invalid image path");
        return path;
    }
}

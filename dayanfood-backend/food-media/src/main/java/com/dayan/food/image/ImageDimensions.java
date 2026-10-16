package com.dayan.food.image;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

public record ImageDimensions(int width, int height) {
    public static ImageDimensions read(byte[] content) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            return read(input);
        }
    }

    public static ImageDimensions read(Path path) throws IOException {
        try (var stream = Files.newInputStream(path);
             ImageInputStream input = ImageIO.createImageInputStream(stream)) {
            return read(input);
        }
    }

    private static ImageDimensions read(ImageInputStream input) throws IOException {
        if (input == null) throw new IllegalArgumentException("无法读取图片内容");
        Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
        if (!readers.hasNext()) throw new IllegalArgumentException("无法读取图片内容");
        ImageReader reader = readers.next();
        try {
            reader.setInput(input, true, true);
            return new ImageDimensions(reader.getWidth(0), reader.getHeight(0));
        } finally {
            reader.dispose();
        }
    }

    public long pixels() {
        return (long) width * height;
    }
}

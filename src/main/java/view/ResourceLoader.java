package com.example.demo.view;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

public class ResourceLoader {
    public static BufferedImage loadImage(String path) {
        try {
            // Важно использовать ведущий слеш или правильный контекст
            var img = ImageIO.read(ResourceLoader.class.getResourceAsStream(path));
            if (img == null) throw new Exception("Файл не найден");
            return img;
        } catch (Exception e) {
            System.err.println("Не удалось загрузить спрайт: " + path);
            return null;
        }
    }
}
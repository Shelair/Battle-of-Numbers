package com.example.demo.view;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

public class ResourceLoader {
    public static BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(Objects.requireNonNull(ResourceLoader.class.getResourceAsStream("/sprites/" + path)));
        } catch (IOException | NullPointerException e) {
            System.err.println("Не удалось загрузить спрайт: " + path);
            return null;
        }
    }
}
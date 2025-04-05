package com.udacity.catpoint.image;

import java.awt.image.BufferedImage;

/**
 * Service that tries to guess if an image displays a cat.
 */
public class FakeImageService implements ImageService {

    @Override
    public boolean imageContainsCat(BufferedImage image) {
        // If image is null, return false
        if (image == null) return false;

        // Check for orange/brown pixels (simulate cat detection)
        int width = image.getWidth();
        int height = image.getHeight();

        // Sample 10% of pixels
        int sampleCount = (width * height) / 10;
        int catPixels = 0;

        for (int i = 0; i < sampleCount; i++) {
            int x = (int) (Math.random() * width);
            int y = (int) (Math.random() * height);
            int rgb = image.getRGB(x, y);

            // Detect orange/brown range
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;

            if (red > 150 && green > 50 && green < 200 && blue < 100) {
                catPixels++;
            }
        }

        return catPixels > (sampleCount / 4);
    }
}
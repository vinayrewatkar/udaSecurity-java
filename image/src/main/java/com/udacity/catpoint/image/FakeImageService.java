package com.udacity.catpoint.image;

import java.awt.image.BufferedImage;

/**
 * Service that tries to guess if an image displays a cat.
 */
public class FakeImageService implements ImageService {

    @Override
    public boolean imageContainsCat(BufferedImage image) {
        // If image is null, return false
        if (image == null) {
            return false;
        }

        // For a fake service, you can use some simple heuristic
        // For example, checking if the image has certain color patterns that might be common in cat images
        // This is just a simple example - a real implementation would be more sophisticated
        int height = image.getHeight();
        int width = image.getWidth();

        // If the image is very small, it's unlikely to be a meaningful cat image
        if (height < 10 || width < 10) {
            return false;
        }

        // Sample the image to see if it contains orange/brown colors common in some cats
        int orangeBrownPixels = 0;
        for (int y = 0; y < height; y += height/10) {
            for (int x = 0; x < width; x += width/10) {
                int pixel = image.getRGB(x, y);
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;

                // Rough check for orange/brown color range
                if (red > 200 && green > 100 && green < 200 && blue < 100) {
                    orangeBrownPixels++;
                }
            }
        }

        // If a certain percentage of sampled pixels are in the orange/brown range, assume it might be a cat
        return orangeBrownPixels > ((height * width) / 100);
    }
}
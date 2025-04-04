package com.udacity.catpoint.image;

import com.udacity.catpoint.image.ImageService;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Service that analyzes images provided from an external API.
 * For this project, you are just returning a random result to simulate the external API.
 */
public class AwsImageService implements ImageService {
    private final Random r = new Random();

    @Override
    public boolean imageContainsCat(BufferedImage image) {
        return r.nextBoolean();
    }
}
package com.udacity.catpoint.image;

import java.awt.image.BufferedImage;

/**
 * Interface for the Image Service component.
 * This interface makes it easier to test the application by allowing
 * for mocked implementations during unit testing.
 */
public interface ImageService {
    /**
     * Analyzes an image to determine if it contains a cat.
     * @param image The image to analyze
     * @return True if the image contains a cat, false otherwise
     */
    boolean imageContainsCat(BufferedImage image);
}
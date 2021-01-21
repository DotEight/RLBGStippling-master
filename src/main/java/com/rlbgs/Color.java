package com.rlbgs;

public class Color {

    public static final int BLACK = getRGB(0, 0, 0);
    public static final int WHITE = getRGB(255, 255, 255);

    private Color() {
    }

    public static int[] getRGB(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = (rgb & 0xff);
        return new int[]{r, g, b};
    }

    public static int getRGB(int red, int green, int blue) {
        return (red << 16) | (green << 8) | (blue) | 0xFF000000;
    }

    public static int getGrey(int color) {
        int r = (color & 0x00FF0000) >> 16;
        int g = (color & 0x0000FF00) >> 8;
        int b = (color & 0x000000FF);
        return (r + b + g) / 3;
    }

    public static int getRGB(int[] rgb) {
        return getRGB(rgb[0], rgb[1], rgb[2]);
    }

}

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;

public final class Tools {

    public final static float TWO_PI = 6.2831855f;
    public final static float PI = 3.1415927f;
    private static Random rg = new Random();

    private Tools() {

    }

    public static void set(PImage img, int x, int y, int color) {
        img.pixels[x + img.width * y] = color;
    }

    public static void set(PImage image, int x, int y, int[] rgb) {
        set(image, x, y, rgb[0], rgb[1], rgb[2]);
    }

    public static void set(PImage img, int x, int y, int r, int g, int b) {
        img.pixels[x + img.width * y] = (r << 16) | (g << 8) | (b);
    }

    public static int get(PImage img, int x, int y) {
        if ((x < 0) || (y < 0) || (x >= img.width) || (y >= img.height)) return 0;
        return img.pixels[x + img.width * y];
    }

    public static PImage createImage(int w, int h, int format) {
        return new PImage(w, h, format);
    }

    public static List<Integer> getNeighbors(PImage image, int x, int y) {

        List<Integer> neighbors = new ArrayList<>();

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                neighbors.add(image.get(x + i, y + j));
            }
        }
        return neighbors;
    }

    public static PImage createImage(int w, int h, int format, PImage original) {
        PImage image = createImage(w, h, format);
        image.parent = original.parent; // make save() work
        return image;
    }

    public static PImage createBlankImageLike(PImage image) {
        return createImage(image.width, image.height, image.format, image);
    }

    public static boolean between(int x, int start, int end) {
        return between((float) x, start, end);
    }

    public static boolean between(float x, float start, float end) {
        return x >= start && x <= end;
    }

    public static PImage copyImage(PImage image) {
        PGraphics output = image.parent.createGraphics(image.width, image.height);
        output.beginDraw();
        output.image(image, 0, 0);
        output.endDraw();
        return output;
    }

    public static Point addJitter(Point point, float range) {
        float amount = 2 * range * rg.nextFloat() + range;
        return new Point(point.x + amount, point.y + amount);
    }

    public static float random(float high) {
        if (high == 0 || high != high) {
            return 0;
        }

        if (rg == null) {
            rg = new Random();
        }

        float value;
        do {
            value = rg.nextFloat() * high;
        } while (value == high);
        return value;
    }

    public static float random(float low, float high) {
        if (low >= high) return low;
        float diff = high - low;
        float value;

        do {
            value = random(diff) + low;
        } while (value == high);
        return value;
    }

    public static double sq (double value) {
        return value * value;
    }

    public static float[][] createDensityMatrix(PImage a) {
        float[][] m = new float[a.pixelWidth][a.height];
        a.loadPixels();
        for (int y = 0; y < a.height; y++)
            for (int x = 0; x < a.width; x++)
                m[x][y] = (float) (1 - Color.getGrey(a.pixels[x+y*a.width]) / 255.0);

        return m;
    }

    public static List<int[]> generateCombinations(int n, int r) {
        List<int[]> combinations = new ArrayList<>();
        helper(combinations, new int[r], 0, n - 1, 0);
        return combinations;
    }

    private static void helper(List<int[]> combinations, int[] data, int start, int end, int index) {
        if (index == data.length) {
            int[] combination = data.clone();
            combinations.add(combination);
        } else {
            int max = Math.min(end, end + 1 - data.length + index);
            for (int i = start; i <= max; i++) {
                data[index] = i;
                helper(combinations, data, i + 1, end, index + 1);
            }
        }
    }

// OTSU THRESHOLD ####################################################################################################
    public static float computeOtsuThreshold(PImage pim)
    {
        int[] histData = new int[256];
        int maxLevelValue = 0;
        int threshold = 0;

        pim.loadPixels();
        int[] srcData = pim.pixels;

        int ptr;

        // Clear histogram data
        // Set all values to zero
        ptr = 0;
        while (ptr < histData.length) histData[ptr++] = 0;

        // Calculate histogram and find the level with the max value
        ptr = 0;

        while (ptr < srcData.length)
        {
            int h = srcData[ptr] & 0xFF;
            histData[h] ++;
            if (histData[h] > maxLevelValue) maxLevelValue = histData[h];
            ptr ++;
        }

        // Total number of pixels
        int total = srcData.length;

        float sum = 0;
        for (int t=0; t<256; t++) sum += t * histData[t];

        float sumB = 0;
        int wB = 0;
        int wF = 0;

        float varMax = 0;

        for (int t=0; t<256; t++)
        {
            wB += histData[t];          // Weight Background
            if (wB == 0) continue;

            wF = total - wB;            // Weight Foreground
            if (wF == 0) break;

            sumB += (float) (t * histData[t]);

            float mB = sumB / wB;        // Mean Background
            float mF = (sum - sumB) / wF;    // Mean Foreground

            // Calculate Between Class Variance
            float varBetween = (float)wB * (float)wF * (mB - mF) * (mB - mF);

            // Check if new maximum found
            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = t;
            }
        }
        return threshold/256.0f;
    }

    public static PImage erodeImage(PImage input) {
        input.loadPixels();

        int diameter = 3;
        int radius = diameter / 2;

        int width = input.width;
        int height = input.height;
        PImage output = Tools.createImage(width, height, PConstants.RGB, input);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (x >= radius && x < width-radius && y >= radius && y < height - radius) {
                    if (fitKernel(input, x, y, radius)) {
                        output.set(x, y,  Color.BLACK);
                    } else {
                        output.set(x, y, Color.WHITE);
                    }
                } else {
                    output.set(x, y, Color.WHITE);
                }
            }
        }

        return output;
    }

    private static boolean fitKernel(PImage input, int x, int y, int radius) {
        for (int i = -radius; i < radius; i++) {
            for (int j = -radius; j < radius; j++) {
                if (input.get(x + i, y + j) == Color.WHITE)
                    return false;
            }
        }
        return true;
    }
}

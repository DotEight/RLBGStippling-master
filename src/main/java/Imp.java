import nu.pattern.OpenCV;
import processing.core.PConstants;
import processing.core.PImage;

public class Imp {

    private static int minPixel(PImage _image, int _w, int _h, int r) {
        int minR = 255;
        int minG = 255;
        int minB = 255;
        int radiusPow = r * r;
        int _r = r/2;
        for (int i = -_r; i < _r; i++) {
            for (int j = -_r; j < _r; j++) {
                if (i * i + j * j < radiusPow) {
                    int c = _image.get(_w + i, _h + j);
                    minR = Math.min(minR, (c >> 16) & 0xFF);
                    minG = Math.min(minG, (c >> 8) & 0xFF);
                    minB = Math.min(minB, c & 0xFF);
                } // end if
            } // end j
        } // end i

        return (minR << 16) | (minG << 8) | minB;
    }

    private static int maxPixel(PImage _image, int _w, int _h, int r) {
        int maxR = 0;
        int maxG = 0;
        int maxB = 0;
        int radiusPow = r * r;
        int _r = r/2;
        for (int i = -_r; i < _r; i++) {
            for (int j = -_r; j < _r; j++) {
                if (i * i + j * j < radiusPow) {
                    int c = _image.get(_w + i, _h + j);
                    maxR = Math.max(maxR, (c >> 16) & 0xFF);
                    maxG = Math.max(maxG, (c >> 8) & 0xFF);
                    maxB = Math.max(maxB, c & 0xFF);
                } // end if
            } // end j
        } // end i
        return (maxR << 16) | (maxG << 8) | maxB;
    }


    public static PImage erode(PImage _image, int _radius) {

        int width = _image.width;
        int height = _image.height;
        int radius = _radius;
        PImage image = _image.copy();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                image.set(i, j, minPixel(_image, i, j, radius));
            }
        }

        return image;
    }
    public static PImage dilate(PImage _image, int _radius) {

        int width = _image.width;
        int height = _image.height;
        int radius = _radius;
        PImage image = _image.copy();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                image.set(i, j, maxPixel(_image, i, j, radius));
            }
        }

        return image;
    }
}    // TODO Class to act as a wrapper to OpenCV and find contours.


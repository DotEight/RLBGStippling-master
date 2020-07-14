package com.rlgbs;

import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import processing.core.PConstants;
import processing.core.PImage;

import java.lang.reflect.Field;
import java.nio.*;
import java.util.ArrayList;
import java.util.List;

public class Imp {

    // Helper method to find the pixel with minimum value in a kernel.
    private static int minPixel(PImage _image, int w, int h, int d) {
        int minR = 255;
        int minG = 255;
        int minB = 255;
        int radiusPow = d * d;
        int r = d / 2;
        for (int i = -r; i < r; i++) {
            for (int j = -r; j < r; j++) {
                if (i * i + j * j < radiusPow) {
                    int c = _image.get(w + i, h + j);
                    minR = Math.min(minR, (c >> 16) & 0xFF);
                    minG = Math.min(minG, (c >> 8) & 0xFF);
                    minB = Math.min(minB, c & 0xFF);
                }
            }
        }

        return (minR << 16) | (minG << 8) | minB;
    }

    // Helper method to find the pixel with maximum value in a kernel.
    private static int maxPixel(PImage _image, int w, int h, int d) {
        int maxR = 0;
        int maxG = 0;
        int maxB = 0;
        int radiusPow = d * d;
        int r = d / 2;
        for (int i = -r; i < r; i++) {
            for (int j = -r; j < r; j++) {
                if (i * i + j * j < radiusPow) {
                    int c = _image.get(w + i, h + j);
                    maxR = Math.max(maxR, (c >> 16) & 0xFF);
                    maxG = Math.max(maxG, (c >> 8) & 0xFF);
                    maxB = Math.max(maxB, c & 0xFF);
                }
            }
        }
        return (maxR << 16) | (maxG << 8) | maxB;
    }

    // Classic erosion method on white pixels.
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

    // Classic dilation method on white pixels.
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

    // Trial method to find and draw contours using OpenCV.
    public static PImage drawContours(PImage input) {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
        Mat mat = toMat(input);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        Core.bitwise_not(mat, mat);
        final List<MatOfPoint> contours = new ArrayList<>();
        final Mat hierarchy = new Mat();
        Imgproc.findContours(mat, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

        Mat drawing = Mat.zeros(mat.size(), CvType.CV_8UC4);

        List<List<Point>> polygons = new ArrayList<>(contours.size());

        for (int i = 0; i < contours.size(); i++) {
            Scalar color;
            if (hierarchy.get(0, i)[3] != -1)
                color = new Scalar(255, 255, 0, 0);
            else
                color = new Scalar(255, 0, 255, 0);

            Imgproc.drawContours(drawing, contours, i, color, 2,
                    Core.LINE_8, hierarchy, 0, new org.opencv.core.Point());
        }
        return toPImage(drawing);
    }

    // Finds and returns contours in the form of a nested point lists.
    // Shapes are approximated according to maxDelta.
    public static List<List<com.rlgbs.Point>> findContours(PImage input, double maxDelta) {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);

        Mat mat = toMat(input);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        Core.bitwise_not(mat, mat);
        Imgproc.blur(mat, mat, new Size(4, 4), new org.opencv.core.Point(-1, -1));

        final List<MatOfPoint> contours = new ArrayList<>();
        final Mat hierarchy = new Mat();
        Imgproc.findContours(mat, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

        List<List<com.rlgbs.Point>> polygons = new ArrayList<>();

        if (maxDelta == 0)
            contoursToPolygons(contours, polygons);
        else
            approxDP(contours, polygons, maxDelta);

        return polygons;
    }

    // Iterates over the list of found contours and converts them. Returns a nested point list.
    private static void contoursToPolygons(List<MatOfPoint> contours, List<List<com.rlgbs.Point>> polygons) {
        for (int i = 0; i < contours.size(); i++) {
            polygons.add(toPolygon(contours.get(i)));
        }
    }

    // Approximates contour shapes using OpenCV and returns them as a list.
    public static void approxDP(List<MatOfPoint> contours, List<List<com.rlgbs.Point>> polygons, double maxDelta) {
        MatOfPoint2f approxCurve = new MatOfPoint2f();

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.01;
            approxDistance = Math.min(maxDelta, approxDistance);

            //if (approxDistance > 1) {
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
            //Convert back to MatOfPoint
            MatOfPoint points = new MatOfPoint(approxCurve.toArray());
            polygons.add(toPolygon(points));
            //}
        }
    }

    // Helper method to convert OpenCV MatOfPoint data structure to a nested list of points
    private static List<com.rlgbs.Point> toPolygon(MatOfPoint matOfPoint) {
        List<org.opencv.core.Point> temp = matOfPoint.toList();
        List<com.rlgbs.Point> polygonPoints = new ArrayList<>(temp.size());

        for (org.opencv.core.Point p : temp) {
            com.rlgbs.Point newPoint = new com.rlgbs.Point((float) p.x, (float) p.y);
            polygonPoints.add(newPoint);
        }
        return polygonPoints;
    }

    // Convert PImage to Mat
    private static Mat toMat(PImage image) {
        image.loadPixels(); //???
        int w = image.width;
        int h = image.height;

        Mat mat = new Mat(h, w, CvType.CV_8UC4);
        byte[] data8 = new byte[w * h * 4];
        int[] data32 = new int[w * h];
        System.arraycopy(image.pixels, 0, data32, 0, image.pixels.length);
        ByteBuffer bBuf = ByteBuffer.allocate(w * h * 4);
        IntBuffer iBuf = bBuf.asIntBuffer();
        iBuf.put(data32);
        bBuf.get(data8);
        mat.put(0, 0, data8);
        image.updatePixels();
        return mat;
    }

    // Convert Mat to PImage
    private static PImage toPImage(Mat mat) {

        int w = mat.width();
        int h = mat.height();

        PImage image = Tools.createImage(w, h, PConstants.ARGB);

        image.loadPixels(); //???
        byte[] data8 = new byte[w * h * 4];
        int[] data32 = new int[w * h];
        mat.get(0, 0, data8);
        ByteBuffer.wrap(data8).asIntBuffer().get(data32);
        System.arraycopy(data32, 0, image.pixels, 0, image.pixels.length);
        image.updatePixels(); //???
        return image;
    }

    // Loads OpenCV native libraries. Currently no need for this
    public static void loadOpenCV() throws Exception {
        // get the model
        String model = System.getProperty("sun.arch.data.model");
        // the path the .dll lib location
        String libraryPath = "C:/opencv/build/java/x86/";
        // check for if system is 64 or 32
        if (model.equals("64")) {
            libraryPath = "C:/opencv/build/java/x64/";
        }
        // set the path
        System.setProperty("java.library.path", libraryPath);
        Field sysPath = ClassLoader.class.getDeclaredField("sys_paths");
        sysPath.setAccessible(true);
        sysPath.set(null, null);
        // load the lib
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
}    // TODO Class to act as a wrapper to OpenCV and find contours.


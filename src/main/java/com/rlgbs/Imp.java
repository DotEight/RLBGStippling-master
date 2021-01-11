package com.rlgbs;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PVector;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Imp {

    private static PApplet pa;
    private static PImage image;
    private static Mat currentMat;
    private static List<MatOfPoint> currentContours;
    private static Mat currentHierarchy;
    private static Mat currentDrawingMat;
    private static Mat currentTriMap;
    private static PImage blackSource;
    private static PImage whiteSource;
    private static int trimapBorderSize;

    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    public static void setPApplet(PApplet pApplet) {
        pa = pApplet;
    }

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

    public static void findContours(Mat mat) {
        final List<MatOfPoint> contours = new ArrayList<>();
        final Mat hierarchy = new Mat();
        Imgproc.findContours(mat, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_NONE);
        currentContours = contours;
        currentHierarchy = hierarchy;
    }

    public static void updateContours() {
        Mat mat = new Mat();
        Imgproc.cvtColor(currentDrawingMat, mat, Imgproc.COLOR_BGRA2GRAY);
        Core.bitwise_not(mat, mat);

        final List<MatOfPoint> contours = new ArrayList<>();
        final Mat hierarchy = new Mat();
        Imgproc.findContours(mat, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_NONE);
        currentContours = contours;
        currentHierarchy = hierarchy;
    }

    private static void filterContours(double factor) {
        List<MatOfPoint> filteredContours = new ArrayList<>();
        List<double[]> hierarchyList = new ArrayList<>();

        double minArea = getMaxContourArea() * factor;
        for (int i = 0; i < currentContours.size(); i++) {
            double polyArea = Imgproc.contourArea(currentContours.get(i));
            if (polyArea < minArea)
                continue;

            filteredContours.add(currentContours.get(i));
            double[] hd = currentHierarchy.get(0, i);
            hierarchyList.add(hd);
        }

        currentContours = filteredContours;

//        Mat filteredHierarchy = new Mat(0, hierarchyList.size(), CvType.CV_32SC4);
//        for (int i = 0; i < hierarchyList.size(); i++) {
//            filteredHierarchy.put(0, i, hierarchyList.get(i));
//        }
//
//        currentHierarchy = filteredHierarchy;
    }

    private static double getMaxContourArea() {
        double maxArea = 0.0f;
        for (int i = 0; i < currentContours.size(); i++) {
            double polyArea = Imgproc.contourArea(currentContours.get(i));
            if (polyArea >= maxArea)
                maxArea = polyArea;
        }

        return maxArea;
    }

    private static double[] getAreaLimits() {
        List<Double> areaList = new ArrayList<>();
        for (int i = 0; i < currentContours.size(); i++) {
            double polyArea = Imgproc.contourArea(currentContours.get(i));
            if (polyArea != 0)
                areaList.add(polyArea);
        }
        double[] areas = new double[areaList.size()];
        for (int i = 0; i < areaList.size(); i++) {
            areas[i] = areaList.get(i);
        }
        return Tools.outlierThresholds(areas);
    }


    // Deprecated
    private static void smoothContours(List<MatOfPoint> contours, List<MatOfPoint> procContours) {
        int filterRadius = 5;
        int filterSize = 2 * filterRadius + 1;
        double sigma = 10;

        for (int i = 0; i < contours.size(); i++) {
            // extract x and y coordinates of points. we'll consider these as 1-D signals
            // add circular padding to 1-D signals
            List<org.opencv.core.Point> pl = contours.get(i).toList();

            int len = pl.size() + 2 * filterRadius;
            int idx = pl.size() - filterRadius;

            List<Double> x = new ArrayList<>();
            List<Double> y = new ArrayList<>();

            for (int j = 0; j < len; j++) {
                org.opencv.core.Point p = pl.get((idx + j) % pl.size());
                x.add(p.x);
                y.add(p.y);
            }

            // filter 1-D signals
            MatOfDouble xFilt = new MatOfDouble();
            xFilt.fromList(x);
            MatOfDouble yFilt = new MatOfDouble();
            yFilt.fromList(y);

            Imgproc.GaussianBlur(xFilt, xFilt, new Size(filterSize, filterSize), sigma, sigma);
            Imgproc.GaussianBlur(yFilt, yFilt, new Size(filterSize, filterSize), sigma, sigma);

            // build smoothed contour
            List<org.opencv.core.Point> smoothPoints = new ArrayList<>(pl.size());
            for (int j = filterRadius; j < pl.size() + filterRadius; j++) {
                double xval = xFilt.get(j, 0)[0];
                double yval = yFilt.get(j, 0)[0];

                smoothPoints.add(new org.opencv.core.Point(xval, yval));
            }

            MatOfPoint smoothPointsMat = new MatOfPoint();
            smoothPointsMat.fromList(smoothPoints);
            procContours.add(smoothPointsMat);
        }
    }

    public static List<MatOfPoint> reSampleContours(int N) {
        List<MatOfPoint> procContours = new ArrayList<>(currentContours.size());

        for (int i = 0; i < currentContours.size(); i++) {
            List<org.opencv.core.Point> pl = currentContours.get(i).toList();
            List<org.opencv.core.Point> newpl;

            newpl = IntStream.range(0, pl.size())
                    .filter(n -> n % N == 0)
                    .mapToObj(pl::get)
                    .collect(Collectors.toList());

            MatOfPoint resampled = new MatOfPoint();
            resampled.fromList(newpl);

            procContours.add(resampled);
        }

        currentContours = procContours;
        return procContours;
    }

    // Approximates contour shapes according to maxDelta. Returns them as a list.
    public static List<MatOfPoint> approximateContours(double maxDelta, boolean closed) {
        List<MatOfPoint> procContours = new ArrayList<>();

        for (int i = 0; i < currentContours.size(); i++) {
            MatOfPoint2f poly = new MatOfPoint2f(currentContours.get(i).toArray());
            double peri = Imgproc.arcLength(poly, closed);
            double epsilon = Math.min(peri * 0.01, maxDelta);
            Imgproc.approxPolyDP(poly, poly, epsilon, closed);
            procContours.add(new MatOfPoint(poly.toArray()));
        }

        currentContours = procContours;
        return procContours;
    }

    public static List<MatOfPoint> gaussianSmoothContours(double sigma) {
        List<MatOfPoint> procContours = new ArrayList<>(currentContours.size());

        double kernelSize = (int) sigma * 3 * 2 + 1;

        for (int i = 0; i < currentContours.size(); i++) {
            MatOfPoint mop = currentContours.get(i);
            Map<Integer, org.opencv.core.Point> touching = getBorderPoints(mop, currentMat.size(), sigma);

            MatOfPoint2f poly = new MatOfPoint2f(mop.toArray());
            Imgproc.GaussianBlur(poly, poly, new Size(kernelSize, kernelSize), sigma, sigma);

            //Convert back to MatOfPoint
            org.opencv.core.Point[] polyArray = poly.toArray();

            for (Map.Entry<Integer, org.opencv.core.Point> pe : touching.entrySet()) {
                polyArray[pe.getKey()] = pe.getValue();
            }
            procContours.add(new MatOfPoint(polyArray));
        }

        currentContours = procContours;
        return procContours;
    }

    public static List<MatOfPoint> chaikinSmoothContours(int iterations, float angleThreshold) {
        List<MatOfPoint> procContours = new ArrayList<>();
        for (int i = 0; i < currentContours.size(); i++) {
            List<org.opencv.core.Point> pl = currentContours.get(i).toList();
            List<org.opencv.core.Point> newpl = chaikinSmooth(pl, angleThreshold);
            MatOfPoint newContour = new MatOfPoint();
            newContour.fromList(newpl);
            procContours.add(newContour);
        }
        currentContours = procContours;
        if (iterations > 1)
            chaikinSmoothContours(iterations - 1, angleThreshold);

        return procContours;
    }

    private static List<org.opencv.core.Point> chaikinSmooth(List<org.opencv.core.Point> pl, float angleThreshold) {
        List<org.opencv.core.Point> newpl = new ArrayList<>();
        for (int i = 0; i < pl.size(); i++) {

            org.opencv.core.Point previous = pl.get(((i - 1) + pl.size()) % pl.size());
            org.opencv.core.Point origin = pl.get(i);
            org.opencv.core.Point next = pl.get((i + 1) % pl.size());

            if (isOnBorder(origin)) {
                newpl.add(origin);
                continue;
            }

            PVector p = new PVector((float) previous.x, (float) previous.y);
            PVector o = new PVector((float) origin.x, (float) origin.y);
            PVector n = new PVector((float) next.x, (float) next.y);
            //a.add(0.5f,0.5f);
            //b.add(0.5f,0.5f);

            PVector po = PVector.sub(o, p).normalize();
            PVector on = PVector.sub(n, o).normalize();

            if (PVector.angleBetween(po, on) <= angleThreshold) {
                newpl.add(origin);
                continue;
            }

            float poMag = PVector.dist(p, o);
            float onMag = PVector.dist(o, n);

            PVector newPoint1 = PVector.add(p, PVector.mult(po, (float) (poMag * 0.75)));
            PVector newPoint2 = PVector.add(o, PVector.mult(on, (float) (onMag * 0.25)));

            newpl.add(new org.opencv.core.Point(newPoint1.x, newPoint1.y));
            newpl.add(new org.opencv.core.Point(newPoint2.x, newPoint2.y));
        }
        return newpl;
    }

    public static PImage drawContours(double epsilon, int thickness) {
        Scalar white = new Scalar(255, 255, 255, 255);
        Scalar black = new Scalar(0, 0, 0, 255);

        Mat drawing = Mat.zeros(currentMat.size(), CvType.CV_8UC4);
        drawing.setTo(white);

        double minArea = getMaxContourArea() * epsilon;
        //double[] areaLimits = getAreaLimits();
        //minArea = areaLimits[1] * epsilon;
        for (int i = 0; i < currentContours.size(); i++) {
            double polyArea = Imgproc.contourArea(currentContours.get(i));
            if (polyArea < minArea)
                continue;

            int parentID = (int) currentHierarchy.get(0, i)[3];
            Scalar patchColor;
            if (parentID != -1) {
                patchColor = white;
            } else {
                patchColor = black;
            }
            Imgproc.drawContours(drawing, currentContours, i, patchColor, thickness,
                    Imgproc.LINE_8, currentHierarchy, 0, new org.opencv.core.Point());
        }

        currentDrawingMat = drawing;

        return toPImage(drawing);
    }

    // Iterates over the list of found contours and converts them. Returns a nested point list.
    public static List<Polygon> getPolygons() {
        //filterContours(0.02);


        List<Polygon> polygons = new ArrayList<>(currentContours.size());
        double minArea = getMaxContourArea() * 0.02;
        int white = pa.color(255);
        int black = pa.color(0);

        for (int i = 0; i < currentContours.size(); i++) {
            double polyArea = Imgproc.contourArea(currentContours.get(i));
            if (polyArea < minArea)
                continue;

            int parentID = (int) currentHierarchy.get(0, i)[3];
            int color;
            if (parentID != -1) {
                color = white;
            } else {
                color = black;
            }
            Polygon polygon = new Polygon(convertToPoints(currentContours.get(i)), color);
            polygons.add(polygon);
        }

        return polygons;
    }

    // Helper method to convert OpenCV MatOfPoint data structure to a nested list of points
    private static List<com.rlgbs.Point> convertToPoints(MatOfPoint contour) {
        List<org.opencv.core.Point> temp = contour.toList();
        List<com.rlgbs.Point> polygonPoints = new ArrayList<>(temp.size());
        temp.stream().forEach(p -> polygonPoints.add(new com.rlgbs.Point((float) p.x, (float) p.y)));
        return polygonPoints;
    }

    private static Map<Integer, org.opencv.core.Point> getBorderPoints(MatOfPoint matOfPoint, Size size, double padding) {
        Map<Integer, org.opencv.core.Point> pm = new HashMap<>();
        List<org.opencv.core.Point> pl = matOfPoint.toList();
        for (int i = 0; i < pl.size(); i++) {
            org.opencv.core.Point p = pl.get(i);
            if (p.x >= size.width - 1 || p.x <= 0 || p.y >= size.height - 1 || p.y <= 0) {
                pm.put(i, pl.get(i));
                for (int j = 1; j <= (int) padding; j++) {
                    if (i + j < pl.size())
                        pm.putIfAbsent(i + j, pl.get(i + j));
                    if (i - j >= 0)
                        pm.putIfAbsent(i - j, pl.get(i - j));
                }
            }
        }
        return pm;
    }

    private static boolean isOnBorder(org.opencv.core.Point p) {
        return p.x >= currentMat.width() - 1 || p.x <= 0 || p.y >= currentMat.height() - 1 || p.y <= 0;
    }

    // Trial method to find and draw contours using OpenCV.
    public static void startProcess(Mat mat) {
        currentMat = mat;

        Core.bitwise_not(mat, mat);

        findContours(mat);
        drawContours(0.0, -1);
        Imgproc.cvtColor(currentDrawingMat, mat, Imgproc.COLOR_BGRA2GRAY);

        Core.bitwise_not(mat, mat);
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9, 9));
        Imgproc.morphologyEx(mat, mat, Imgproc.MORPH_CLOSE, element);
        Imgproc.morphologyEx(mat, mat, Imgproc.MORPH_OPEN, element);
        //Imgproc.morphologyEx(mat, mat, Imgproc.MORPH_CLOSE, element);
        //Imgproc.morphologyEx(mat, mat, Imgproc.MORPH_OPEN, element);

        findContours(mat);
    }

    public static PImage processImage(PImage reference, int th) {
        Mat input = prepareMat(reference);
        currentMat = input;
        Mat mat = new Mat();
        Imgproc.bilateralFilter(input, mat, 3, 75, 75);
        //Imgproc.medianBlur(input, mat, 5);

        Imgproc.threshold(mat, mat, th, 255, Imgproc.THRESH_BINARY);
        //Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        //Imgproc.morphologyEx(mat, mat, Imgproc.MORPH_CLOSE, element);

        startProcess(mat);
        gaussianSmoothContours(5);
        approximateContours(10, true);
        chaikinSmoothContours(2, PConstants.PI / 12);
        drawContours(0.02, -1);
        return toPImage(currentDrawingMat);
    }

    public static PImage createBackground(PImage reference, int th) {
        processImage(reference, th);

        //Mat o = new Mat();
        //Imgproc.cvtColor(currentDrawingMat, o, Imgproc.COLOR_BGRA2GRAY);

        return toPImage(currentDrawingMat);
    }

    public static PImage createBlackSource(PImage reference, int th) {
        processImage(reference, th);

        Mat o = new Mat();
        Imgproc.cvtColor(currentDrawingMat, o, Imgproc.COLOR_BGRA2GRAY);

        Imgproc.dilate(o, o, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(15, 15)));

        Mat i = prepareMat(reference);
        Core.bitwise_not(i, i);
        Core.bitwise_and(i, o, o);
        Core.bitwise_not(o, o);

        return toPImage(o);
    }

    public static PImage createWhiteSource(PImage reference, int th) {
        processImage(reference, th);

        Mat o = new Mat();
        Imgproc.cvtColor(currentDrawingMat, o, Imgproc.COLOR_BGRA2GRAY);

        Core.bitwise_not(o, o);
        Imgproc.dilate(o, o, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(15, 15)));

        Mat i = prepareMat(reference);
        Core.bitwise_and(i, o, o);

        return toPImage(o);
    }

    public static PImage createTriMap(int borderSize) {

        Mat mat = new Mat();
        Imgproc.cvtColor(currentDrawingMat, mat, Imgproc.COLOR_BGRA2GRAY);
        Core.bitwise_not(mat, mat);

        final List<MatOfPoint> contours = new ArrayList<>();
        final Mat hierarchy = new Mat();
        Imgproc.findContours(mat, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_NONE);

        Mat drawing = Mat.zeros(mat.size(), CvType.CV_8UC4);
        drawing.setTo(new Scalar(255, 255, 255, 255));

        for (int i = 0; i < contours.size(); i++) {

            int parentID = (int) hierarchy.get(0, i)[3];
            Scalar patchColor;
            if (parentID != -1) {
                patchColor = new Scalar(255, 255, 255, 255);
            } else {
                patchColor = new Scalar(0, 0, 0, 255);
            }

            Imgproc.drawContours(drawing, contours, i, patchColor, -1,
                    Imgproc.LINE_8, hierarchy, 0, new org.opencv.core.Point());

            if (borderSize == -1)
                continue;

            List<org.opencv.core.Point> pl = contours.get(i).toList();
            Scalar c = new Scalar(127, 127, 127, 255);
            Size s = new Size(borderSize + 1, borderSize + 1);

            for (int j = 0; j < pl.size(); j++) {
                org.opencv.core.Point p = pl.get(j);
                if (!isOnBorder(p)) {
                    Imgproc.ellipse(drawing, new RotatedRect(p, s, 180), c, -1);
                }
            }
        }
        currentTriMap = drawing;
        return toPImage(drawing);
    }

    public static PImage getContourGradient(int thickness) {
        Mat mat = new Mat();
        Imgproc.cvtColor(currentDrawingMat, mat, Imgproc.COLOR_BGRA2GRAY);
        Core.bitwise_not(mat, mat);
        findContours(mat);

        Mat drawing = Mat.zeros(currentMat.size(), CvType.CV_8UC4);
        drawing.setTo(new Scalar(255, 255, 255, 255));

        Scalar black = new Scalar(0, 0, 0, 255);
        for (int i = 0; i < currentContours.size(); i++) {
            Imgproc.drawContours(drawing, currentContours, i, black, thickness,
                    Imgproc.LINE_8, currentHierarchy, 0, new org.opencv.core.Point());
        }


        Imgproc.cvtColor(drawing, drawing, Imgproc.COLOR_BGRA2GRAY);
//        Core.bitwise_not(drawing, drawing);
//        Mat ref = prepareMat(reference);
//        Core.bitwise_not(ref, ref);
//        Core.bitwise_and(ref, drawing, ref);
//        Core.bitwise_not(ref, ref);
        double sigma = thickness * 2;
        double kernelSize = (sigma * 3 * 2) + 1;
        Imgproc.GaussianBlur(drawing, drawing, new Size(kernelSize, kernelSize), sigma, sigma);

        return toPImage(drawing);
    }

    public static PImage resize(PImage reference, int scale) {
        Mat img = toMat(reference);
        ARGBtoBGRA(img, img);
        Imgproc.resize(img, img, new Size(img.width() * scale, img.height() * scale), 0, 0, Imgproc.INTER_AREA);
        return toPImage(img);
    }

    public static PImage invert(PImage reference) {
        Mat o = toMat(reference);
        ARGBtoBGRA(o, o);
        Core.bitwise_not(o, o);
        return toPImage(o);
    }

    // Convert ARGB PImage to 4-Channel Mat
    /*private static Mat toMat(PImage image) {
        image.loadPixels();
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
        return mat;
    }

    // Convert 4-Channel Mat to ARGB PImage
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
    } */

    public static Mat prepareMat(PImage img) {
        Mat mat = toMat(img);
        ARGBtoBGRA(mat, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2GRAY);
        return mat;
    }

    public static PImage toPImage(Mat m) {

        PImage img = pa.createImage(m.width(), m.height(), PConstants.ARGB);

        img.loadPixels();

        if (m.channels() == 3) {
            Mat m2 = new Mat();
            Imgproc.cvtColor(m, m2, Imgproc.COLOR_BGR2BGRA);
            img.pixels = matToARGBPixels(m2);
        } else if (m.channels() == 1) {
            Mat m2 = new Mat();
            Imgproc.cvtColor(m, m2, Imgproc.COLOR_GRAY2BGRA);
            img.pixels = matToARGBPixels(m2);
        } else if (m.channels() == 4) {
            img.pixels = matToARGBPixels(m);
        }

        img.updatePixels();
        return img;
    }

    public static Mat toMat(PImage img) {

        int w = img.width;
        int h = img.height;

        Mat m = new Mat(h, w, CvType.CV_8UC4);

        BufferedImage image = (BufferedImage) img.getNative();
        int[] matPixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        ByteBuffer bb = ByteBuffer.allocate(matPixels.length * 4);
        IntBuffer ib = bb.asIntBuffer();
        ib.put(matPixels);

        byte[] bvals = bb.array();

        m.put(0, 0, bvals);
        return m;
    }

    private static int[] matToARGBPixels(Mat m) {
        int pImageChannels = 4;
        int numPixels = m.width() * m.height();
        int[] intPixels = new int[numPixels];
        byte[] matPixels = new byte[numPixels * pImageChannels];

        m.get(0, 0, matPixels);
        ByteBuffer.wrap(matPixels).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(intPixels);
        return intPixels;
    }

    public static void ARGBtoBGRA(Mat argb, Mat rgba) {
        ArrayList<Mat> channels = new ArrayList<>();
        Core.split(argb, channels);

        ArrayList<Mat> reordered = new ArrayList<>();

        reordered.add(channels.get(3));
        reordered.add(channels.get(2));
        reordered.add(channels.get(1));
        reordered.add(channels.get(0));

        Core.merge(reordered, rgba);
    }

    public static PImage modifyRef(PImage reference, int th, int thickness) {
        processImage(reference, th);

        Mat o = new Mat();
        Imgproc.cvtColor(currentDrawingMat, o, Imgproc.COLOR_BGRA2GRAY);

        Mat g = toMat(getContourGradient(thickness));
        ARGBtoBGRA(g, g);
        Imgproc.cvtColor(g, g, Imgproc.COLOR_BGRA2GRAY);
        Core.bitwise_not(g, g);
        //Core.divide(g, new Scalar(5), g);
        Core.absdiff(o, g, g);
        Mat i = prepareMat(reference);

        //Core.bitwise_not(g, g);

        return toPImage(g);
    }

}
// TODO Class to act as a wrapper to OpenCV and find contours.


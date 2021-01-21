package com.rlbgs;

import processing.core.PApplet;
import processing.core.PImage;

import java.util.ArrayList;

import static java.lang.Math.*;

// CELL CLASS ####################################################################################################
class Cell {
    int index;
    int reverse = 0;
    int onBorder = 0;

    ArrayList<Point> pixelList = new ArrayList<>();
    ArrayList<Point> whitePixelList = new ArrayList<>();
    ArrayList<Point> blackPixelList = new ArrayList<>();

    float area = 0;
    float sumDensity = 0;
    float avgDensity = 0;
    float orientation = 0;
    float eccentricity = 0;
    float cv = 0;

    float[] moments = new float[6];

    Point site = new Point(0, 0);
    Point centroid = new Point(0, 0);


    Cell(int index) {
        this.index = index;
    }

    Cell(int index, Point site) {
        this.index = index;
        this.site = site;
    }

    void resetProperties() {
        this.onBorder = 0;
        this.orientation = 0;
        this.eccentricity = 0;
        this.cv = 0;
        this.moments = new float[6];
        //this.site = new Point(0, 0);
        this.centroid = new Point(0, 0);
    }

    void calculateProperties(float[][] densityMatrix) {
        resetProperties();
        float sigmaDiff = 0;
        area = pixelList.size();

        for (Point p : pixelList) {
            // Cast point to pixel location
            int x = (int) p.x;
            int y = (int) p.y;
            float densityValue = abs(reverse - densityMatrix[x][y]);

            moments[0] += densityValue;
            moments[1] += x * densityValue;
            moments[2] += y * densityValue;
            moments[3] += x * y * densityValue;
            moments[4] += x * x * densityValue;
            moments[5] += y * y * densityValue;

            float diff = densityMatrix[x][y] - avgDensity;
            sigmaDiff += diff * diff;
        }

        // Calculate higher order properties
        // Standart deviation
        cv = (float) sqrt(sigmaDiff * 255 / area) / (avgDensity * 255);

        // Moments are placed in the array from 0 to 5 with the order: m00, m10, m01, m11, m20, m02
        float[] m = moments;

        // Centroid
        centroid.setX(m[1] / m[0]);
        centroid.setY(m[2] / m[0]);

        // Central moments
        float x = m[4] / m[0] - centroid.x * centroid.x;
        float y = m[3] / m[0] - centroid.x * centroid.y; // Stopped multiplying this by 2 because no point
        float z = m[5] / m[0] - centroid.y * centroid.y;

        // Orientation
        orientation = (float) (0.5 * atan(2 * y / (x - z)));

        // Eccentricity
        double lambda1 = (x + z) / 2 + Math.sqrt(4 * Tools.sq(y) + Tools.sq(x - z)) / 2;
        double lambda2 = (x + z) / 2 - Math.sqrt(4 * Tools.sq(y) + Tools.sq(x - z)) / 2;
        eccentricity = (float) Math.sqrt((1 - lambda2 / lambda1));
        //eccentricity = (float) (((com.rlgbs.Tools.sq(x - z)) - (4 * com.rlgbs.Tools.sq(y))) / com.rlgbs.Tools.sq(x + z));
    }

    void evaluateCell(float[][] densityMatrix, PImage adjustImage, PApplet pa) {
        ArrayList<Point> pixels = pixelList;
        ArrayList<Point> whitePixels = new ArrayList<>();
        ArrayList<Point> blackPixels = new ArrayList<>();

        for (Point p : pixels) {
            int x = (int) p.x;
            int y = (int) p.y;
            int index = x + adjustImage.width * y;
            int color = adjustImage.pixels[index];

            if (color == pa.color(255)) {
                whitePixels.add(p);
            } else if (color == pa.color(0)) {
                blackPixels.add(p);
            }
        }

        int ci = (int) centroid.x + adjustImage.width * (int) centroid.y;
        int cc = adjustImage.pixels[ci];
        if (cc == pa.color(255)) {
            reverse = 0;
            pixelList = whitePixels;
        } else {
            reverse = 1;
            pixelList = blackPixels;
        }

//        double diff = abs(whitePixels.size() - blackPixels.size());
//
//        if (whitePixels.size() > blackPixels.size()) {
//            c.reverse = 0;
//            if (diff != c.area) {
//                c.pixelList = whitePixels;
//            }
//        } else {
//            c.reverse = 1;
//            if (diff != c.area) {
//                c.pixelList = blackPixels;
//            }
//        }

        calculateProperties(densityMatrix);
        pixelList = pixels;
    }

    public void flip() {
        this.reverse = 1 - this.reverse;
    }
}

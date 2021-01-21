package com.rlbgs;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

import java.util.ArrayList;
import java.util.List;

import static processing.core.PConstants.*;


class StipplePainter {
    PApplet pa;
    StippleGenerator stippleGenerator;
    boolean paintAddedSites = false;
    boolean showStippleIndexes = false;

    private final PGraphics stippleImage;
    private PImage backgroundImage;
    private final int scale;
    private final int w;
    private final int h;

    StipplePainter(PApplet pa, StippleGenerator rlbgs, int scale) {
        this.stippleGenerator = rlbgs;
        this.pa = pa;
        w = rlbgs.img.width * scale;
        h = rlbgs.img.height * scale;
        this.scale = scale;
        stippleImage = pa.createGraphics(this.scale * w, this.scale * h);
        stippleImage.smooth(8);
        backgroundImage = pa.createImage(this.scale * w, this.scale * h, ARGB);
    }

    public void setBackgroundImage(PImage bg) {
        this.backgroundImage = bg;
    }

    public PGraphics getStippleImage() {

        return this.stippleImage;
    }

    public PImage getBackgroundImage() {

        return this.backgroundImage.get();
    }

    PImage paint() {
        stippleImage.beginDraw();
        stippleImage.background(paintBackground());
        paintStipples();
        if (paintAddedSites)
            paintAddedSites();
        if (showStippleIndexes)
            paintStippleIndexes();
        stippleImage.endDraw();

        return stippleImage.get();
    }

    public PImage update() {
        stippleImage.beginDraw();
        stippleImage.background(this.backgroundImage);
        paintStipples();
        if (paintAddedSites)
            paintAddedSites();
        if (showStippleIndexes)
            paintStippleIndexes();
        stippleImage.endDraw();

        return stippleImage.get();
    }

    void clearBackground() {
        for (int i = 0; i < backgroundImage.pixels.length; i++) {
            backgroundImage.pixels[i] = pa.color(255);
        }
    }

    PImage paintBackground() {
        backgroundImage.loadPixels();
        //clearBackground();
        for (Cell stippleCell : stippleGenerator.getStippleCells()) {
            for (Point pp : stippleCell.pixelList) {
                int x = (int) (scale * pp.x);
                int y = (int) (scale * pp.y);

                backgroundImage.pixels[x + y * backgroundImage.width] = pa.color(255 * (1 - stippleCell.reverse));
                for (int i = 1; i < scale; i++) {
                    backgroundImage.pixels[(x + i) + y * backgroundImage.width] = pa.color(255 * (1 - stippleCell.reverse));
                    backgroundImage.pixels[x + (y + i) * backgroundImage.width] = pa.color(255 * (1 - stippleCell.reverse));
                    backgroundImage.pixels[(x + i) + (y + i) * backgroundImage.width] = pa.color(255 * (1 - stippleCell.reverse));
                }
            }
        }
        backgroundImage.updatePixels();
        return backgroundImage;
    }

    public void paintCell(Cell cell, int cc) {
        backgroundImage.loadPixels();
        for (Point pp : cell.pixelList) {
            int x = (int) (scale * pp.x);
            int y = (int) (scale * pp.y);

            backgroundImage.pixels[x + y * backgroundImage.width] = cc;
            for (int i = 1; i < scale; i++) {
                backgroundImage.pixels[(x + i) + y * backgroundImage.width] = cc;
                backgroundImage.pixels[x + (y + i) * backgroundImage.width] = cc;
                backgroundImage.pixels[(x + i) + (y + i) * backgroundImage.width] = cc;
            }
        }
        backgroundImage.updatePixels();
        update();
    }

    private void paintCells(ArrayList<Cell> cells, int cc) {
        backgroundImage.loadPixels();
        for (Cell stippleCell : stippleGenerator.getStippleCells()) {
            for (Point pp : stippleCell.pixelList) {
                backgroundImage.pixels[(int) pp.x + (int) pp.y * w] = cc;
            }
        }
        backgroundImage.updatePixels();
        update();
    }

    private void paintStipples() {
        stippleImage.ellipseMode(CENTER);
        stippleImage.noStroke();
        for (Stipple s : stippleGenerator.getStipples()) {
            stippleImage.fill(s.c);
            float d = s.size;
            stippleImage.ellipse(scale * (s.location.x), scale * (s.location.y), scale * d, scale * d);
        }
    }

    public void paintStipplesOnContours(List<Polygon> contours) {
        stippleImage.beginDraw();
        stippleImage.image(backgroundImage, 0, 0);
        //stippleImage.background(this.backgroundImage);
        if (paintAddedSites)
            paintAddedSites();
        if (showStippleIndexes)
            paintStippleIndexes();
        for (int i = 0; i < contours.size(); i++) {
            paintStipplesOnContour(contours.get(i).getVetices());
        }
        paintStipples();
        stippleImage.endDraw();
    }

    public void paintStipplesOnContour(List<Point> contour) {
        float ssize = stippleGenerator.options.stippleSizeMin;
        stippleImage.noStroke();
        boolean black = true;
        for (int i = 0; i < contour.size(); i += ssize * 2) {
            Point p = contour.get(i);
            Tools.addJitter(p, 0.5f);
            float x = p.x;
            float y = p.y;

            //float chance = Tools.random(100);
            if (black) {
                stippleImage.fill(pa.color(0));
            } else {
                stippleImage.fill(pa.color(255));
            }
            stippleImage.ellipse(x, y, ssize, ssize);
            black = !black;
        }
    }

    public void paintBorderStipples(PImage image) {
        stippleGenerator.fixStippleColors(image);
        changeBackground(image);
    }

    private void paintStippleIndexes() {
        stippleImage.textFont(pa.createFont("Georgia", (float) (stippleGenerator.options.maxIterations / Math.sqrt((stippleGenerator.status.iterations + 1)))));
        for (Cell stippleCell : stippleGenerator.getStippleCells()) {
            stippleImage.fill(stippleCell.reverse * 255);
            stippleImage.text(stippleCell.index, stippleCell.site.x, stippleCell.site.y);
            stippleImage.ellipse(stippleCell.site.x, stippleCell.site.y, 5, 5);
        }
    }

    private void paintAddedSites() {
        for (Point p : stippleGenerator.getStippleSites()) {
            stippleImage.noStroke();
            stippleImage.fill(pa.color(0, 0, 255));
            float d = (float) ((stippleGenerator.options.stippleSizeMax + stippleGenerator.options.stippleSizeMin) * 0.5);
            stippleImage.ellipse(p.x, p.y, d, d);
        }
    }

    // Smooth jagged cell edges by thresholding the image again after blurring.
    public void smoothBackground() {
        PImage temp = this.backgroundImage.get();
        temp.filter(BLUR, 2);
        temp.filter(THRESHOLD, 0.5f);
        changeBackground(temp);
    }

    public void ellipse(float x, float y, float lambda1, float lambda2, float orientation) {
        stippleImage.beginDraw();
        stippleImage.pushMatrix();
        stippleImage.translate(x, y);
        stippleImage.rotate(orientation);
        stippleImage.ellipse(0, 0, lambda1, lambda2);
        stippleImage.popMatrix();
        stippleImage.endDraw();
    }

    public void line(float x, float y, float orientation) {
        stippleImage.beginDraw();
        stippleImage.line(x, y, x + PApplet.cos(orientation) * 5, y - PApplet.sin(orientation) * 5);
        stippleImage.endDraw();
    }

    public void changeBackground(PImage bg) {
        this.backgroundImage = bg.copy();
        update();
    }

    public void saveSVG() {
        List<Polygon> contours = Imp.getPolygons();
        PGraphics svg = pa.createGraphics(w, h, SVG, "/Users/kerem/Desktop/output2.svg");
        svg.beginDraw();

        svg.noStroke();
        for (Polygon contour : contours) {
            svg.beginShape(POLYGON);
            svg.fill(contour.color);

            Point pp = contour.getVetices().get(0);
            float xx = pp.x;
            float yy = pp.y;
            svg.curveVertex(xx, yy);

            for (int i = 0; i < contour.getVetices().size(); i++) {
                Point p = contour.getVetices().get(i);
                float x = p.x;
                float y = p.y;
                svg.curveVertex(x, y);
            }

            pp = contour.getVetices().get(contour.getVetices().size() - 1);
            xx = pp.x;
            yy = pp.y;
            svg.curveVertex(xx, yy);
            svg.endShape();
        }

        svg.noStroke();
        svg.ellipseMode(CENTER);
        for (Stipple s : stippleGenerator.getStipples()) {
            svg.fill(s.c);
            float d = s.size;
            svg.ellipse(scale * (s.location.x), scale * (s.location.y), scale * d, scale * d);
        }

        svg.dispose();
        svg.endDraw();
    }
    // TODO method to check boundaries and handle black cells near edges, contours etc.

}
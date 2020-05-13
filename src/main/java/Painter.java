import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

import static processing.core.PConstants.*;

import java.util.ArrayList;


class Painter {
    PApplet pa;
    Stippler stippler;
    PGraphics painting;
    PImage background;
    boolean paintAddedSites = false;
    boolean showStippleIndexes = false;


    private final int w;
    private final int h;

    Painter(PApplet pa, Stippler rlbgs) {
        this.stippler = rlbgs;
        this.pa = pa;
        w = rlbgs.img.width;
        h = rlbgs.img.height;
        painting = pa.createGraphics(w, h);
        background = pa.createImage(w, h, RGB);
    }

    PImage getPainting() {
        return this.painting;
    }
    public PImage getBackground() { return this.background; }

    PImage paint() {
        painting.beginDraw();
        painting.background(paintBackground());
        paintStipples();

        if (paintAddedSites)
            paintAddedSites();

        if (showStippleIndexes)
            showStippleIndexes();

        painting.endDraw();
        return painting;
    }

    void updateBackground() {
        painting.beginDraw();
        painting.background(background);
        paintStipples();

        if (paintAddedSites)
            paintAddedSites();

        if (showStippleIndexes)
            showStippleIndexes();

        painting.endDraw();
    }


    PImage paintBackground() {
        background.loadPixels();
        for (Cell stippleCell : stippler.getStippleCells()) {
            for (Point pp : stippleCell.pixelList) {
                background.pixels[(int) pp.x + (int) pp.y * w] = pa.color(255 * (1 - stippleCell.reverse));
            }
        }
        background.updatePixels();
        return background;
    }


    private void paintCell(Cell cell, int cc) {
        background.loadPixels();
        for (Point pp : cell.pixelList) {
            background.pixels[(int) pp.x + (int) pp.y * w] = cc;
        }
        background.updatePixels();
        updateBackground();
    }

    private void paintCells(ArrayList<Cell> cells, int cc) {
        background.loadPixels();
        for (Cell stippleCell : stippler.getStippleCells()) {
            for (Point pp : stippleCell.pixelList) {
                background.pixels[(int) pp.x + (int) pp.y * w] = cc;
            }
        }
        background.updatePixels();
        updateBackground();
    }

    private void paintStipples() {
        for (Stipple s : stippler.getStipples()) {
            painting.noStroke();
            painting.fill(s.c);
            float d = s.size;
            painting.ellipseMode(CENTER);
            painting.ellipse(s.location.x, s.location.y, d, d);
        }
    }

    private void showStippleIndexes() {
        painting.textFont(pa.createFont("Georgia", (float) (stippler.options.maxIterations / Math.sqrt((stippler.status.iterations + 1)))));
        for (Cell stippleCell : stippler.getStippleCells()) {
            painting.fill(stippleCell.reverse * 255);
            painting.text(stippleCell.index, stippleCell.site.x, stippleCell.site.y);
            painting.ellipse(stippleCell.site.x, stippleCell.site.y, 5, 5);
        }
    }

    private void paintAddedSites() {
        for (Point p : stippler.getStippleSites()) {
            painting.noStroke();
            painting.fill(pa.color(0, 0, 255));
            float d = (float) ((stippler.options.stippleSizeMax + stippler.options.stippleSizeMin) * 0.5);
            painting.ellipse(p.x, p.y, d, d);
        }
    }

    // Smooth jagged cell edges by thresholding the image again after blurring.
    public void smoothBackground() {
        //erodeBackground(2);
        background.filter(BLUR, 2);
        background.filter(THRESHOLD, 0.5f);
        updateBackground();
    }

    //  Method to perform erosion on the background. Foreground pixels are black in this instance.
    public void erodeBackground(int diameter) {
        background.loadPixels();
        int radius = diameter / 2;

        PImage output = pa.createImage(w, h, RGB);

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (x >= radius && x < w - radius && y >= radius && y < h - radius) {
                    if (fitKernel(x, y, radius)) {
                        output.set(x, y, Color.BLACK);
                    } else {
                        output.set(x, y, Color.WHITE);
                    }
                } else {
                    output.set(x, y, Color.WHITE);
                }
            }
        }
        background = output;
        updateBackground();
    }

    private boolean fitKernel(int x, int y, int radius) {
        for (int i = -radius; i < radius; i++) {
            for (int j = -radius; j < radius; j++) {
                if (background.get(x + i, y + j) == Color.WHITE)
                    return false;
            }
        }
        return true;
    }

    // TODO method to check boundaries and handle black cells near edges, contours etc.

}
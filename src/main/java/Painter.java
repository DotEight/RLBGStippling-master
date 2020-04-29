import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

import static processing.core.PConstants.*;

import java.util.ArrayList;


class Painter {
    PApplet pa;
    Stippler rlbgs;
    PGraphics painting;
    PImage background;
    boolean paintAddedSites = false;
    boolean connectCellsEachIteration = false;
    boolean showStippleIndexes = false;


    private int w, h;

    Painter(PApplet pa, Stippler rlbgs) {
        this.rlbgs = rlbgs;
        this.pa = pa;
        w = rlbgs.img.width;
        h = rlbgs.img.height;
        painting = pa.createGraphics(w, h);
        background = pa.createImage(w, h, RGB);
    }

    PImage getPainting() {
        return this.painting;
    }

    PImage paint() {
        painting.beginDraw();
        painting.background(paintBackground());
        paintStipples();

        if (paintAddedSites)
            paintAddedSites();

        if (showStippleIndexes)
            showStippleIndexes();

        if (connectCellsEachIteration) {
        }


        painting.endDraw();
        return painting;
    }

    void updatePainting() {

        painting.beginDraw();
        painting.background(background);
        paintStipples();

        if (paintAddedSites)
            paintAddedSites();

        if (showStippleIndexes)
            showStippleIndexes();

        if (connectCellsEachIteration)

            painting.endDraw();
    }

    PImage paintBackground() {
        background.loadPixels();
        for (Cell stippleCell : rlbgs.getStippleCells()) {
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
        updatePainting();
    }

    private void paintCells(ArrayList<Cell> cells, int cc) {
        background.loadPixels();
        for (Cell stippleCell : rlbgs.getStippleCells()) {
            for (Point pp : stippleCell.pixelList) {
                background.pixels[(int) pp.x + (int) pp.y * w] = cc;
            }
        }
        background.updatePixels();
        updatePainting();
    }

    private void paintStipples() {
        for (Stipple s : rlbgs.getStipples()) {
            painting.noStroke();
            painting.fill(s.c);
            float d = s.size;
            painting.ellipse(s.location.x, s.location.y, d, d);
        }
    }

    private void showStippleIndexes() {
        for (Cell stippleCell : rlbgs.getStippleCells()) {
            painting.fill(stippleCell.reverse * 255);
            painting.textFont(pa.createFont("Georgia", (float) (rlbgs.options.maxIterations / Math.sqrt((rlbgs.status.iterations + 1)))));
            painting.text(stippleCell.index, stippleCell.centroid.x, stippleCell.centroid.y);
        }
    }

    private void paintAddedSites() {
        for (Point p : rlbgs.getStippleSites()) {
            painting.noStroke();
            painting.fill(pa.color(0, 0, 255));
            float d = (float) ((rlbgs.options.stippleSizeMax + rlbgs.options.stippleSizeMin) * 0.5);
            painting.ellipse(p.x, p.y, d, d);
        }
    }
    // TODO method to do erosion on the background
    void erodeBackground() {
        background.loadPixels();

        //perform erosion
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (background.pixels[x + y * w] == pa.color(0)) {
                    // Using a 3x3 kernel
                    boolean flag = false;   //this will be set if a pixel of reverse value is found in the mask
                    for (int ty = y - 1; ty <= y + 1 && flag == false; ty++) {
                        for (int tx = x - 1; tx <= x + 1 && flag == false; tx++) {
                            if (ty >= 0 && ty < h && tx >= 0 && tx < w) {
                                //origin of the mask is on the image pixels
                                if (background.pixels[x + y * background.width] != pa.color(0)) {
                                    flag = true;
                                    background.pixels[x + y * w] = pa.color(255);
                                }
                            }
                        }
                    }
                    if (flag == false) {
                        //all pixels inside the mask [i.e., kernel] were of targetValue
                        background.pixels[x + y * w] = pa.color(0);
                    }
                } else {
                    background.pixels[x + y * w] = pa.color(255);
                }
            }
        }
        background.updatePixels();

    }
    // TODO method to check boundaries and handle black cells near edges, contours etc.

}
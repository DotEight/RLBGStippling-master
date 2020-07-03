import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

import static processing.core.PConstants.*;

import java.util.ArrayList;


class Painter {
    PApplet pa;
    StippleGenerator stippleGenerator;
    PGraphics stippleImage;
    PImage backgroundImage;
    boolean paintAddedSites = false;
    boolean showStippleIndexes = false;


    private final int w;
    private final int h;

    Painter(PApplet pa, StippleGenerator rlbgs) {
        this.stippleGenerator = rlbgs;
        this.pa = pa;
        w = rlbgs.img.width;
        h = rlbgs.img.height;
        stippleImage = pa.createGraphics(2*w, 2*h);
        stippleImage.smooth(2);
        backgroundImage = pa.createImage(2*w, 2*h, RGB);
    }

    public PGraphics getStippleImage() {
        return this.stippleImage;
    }
    public PImage getBackgroundImage() { return this.backgroundImage; }

    public void display() {
        this.stippleImage.background(this.backgroundImage);
        pa.image(this.stippleImage,0 ,0);
    }

    PImage paint() {
        this.backgroundImage = paintBackground();

        stippleImage.beginDraw();
        this.stippleImage.background(this.backgroundImage);
        paintStipples();
        if (paintAddedSites)
            paintAddedSites();
        if (showStippleIndexes)
            showStippleIndexes();
        PImage image = stippleImage.get();
        stippleImage.endDraw();
        return image;
    }

    void updateBackground() {
        stippleImage.beginDraw();
        stippleImage.background(this.backgroundImage);

        paintStipples();

        if (paintAddedSites)
            paintAddedSites();

        if (showStippleIndexes)
            showStippleIndexes();

        stippleImage.endDraw();
    }

    PImage paintBackground() {
        backgroundImage.loadPixels();
        for (Cell stippleCell : stippleGenerator.getStippleCells()) {
            for (Point pp : stippleCell.pixelList) {
                backgroundImage.pixels[(int) (2*pp.x)+ (int) (2*pp.y * backgroundImage.width)] = pa.color(255 * (1 - stippleCell.reverse));
                backgroundImage.pixels[(int) (2*pp.x+1)+ (int) (2*pp.y * backgroundImage.width)] = pa.color(255 * (1 - stippleCell.reverse));
                backgroundImage.pixels[(int) (2*pp.x)+ (int) ((2*pp.y+1) * backgroundImage.width)] = pa.color(255 * (1 - stippleCell.reverse));
                backgroundImage.pixels[(int) (2*pp.x+1)+ (int) ((2*pp.y+1) * backgroundImage.width)] = pa.color(255 * (1 - stippleCell.reverse));
            }
        }
        backgroundImage.updatePixels();
        return backgroundImage;
    }

    public void paintCell(Cell cell, int cc) {
        backgroundImage.loadPixels();
        for (Point pp : cell.pixelList) {
            backgroundImage.pixels[(int) pp.x + (int) pp.y * w] = cc;
        }
        backgroundImage.updatePixels();
        updateBackground();
    }

    private void paintCells(ArrayList<Cell> cells, int cc) {
        backgroundImage.loadPixels();
        for (Cell stippleCell : stippleGenerator.getStippleCells()) {
            for (Point pp : stippleCell.pixelList) {
                backgroundImage.pixels[(int) pp.x + (int) pp.y * w] = cc;
            }
        }
        backgroundImage.updatePixels();
        updateBackground();
    }

    private void paintStipples() {
        for (Stipple s : stippleGenerator.getStipples()) {
            stippleImage.noStroke();
            stippleImage.fill(s.c);
            float d = s.size;
            stippleImage.ellipseMode(CENTER);
            stippleImage.ellipse(2*s.location.x, 2*s.location.y, 2*d, 2*d);
        }
    }

    private void showStippleIndexes() {
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
        //erodeBackground(2);
        backgroundImage.filter(BLUR, 2);
        backgroundImage.filter(THRESHOLD, 0.5f);
        updateBackground();
    }

    public void dilateBackground(int diameter) {
        backgroundImage.loadPixels();
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
        backgroundImage = output;
        updateBackground();
    }

    //  Method to perform erosion on the background. Foreground pixels are black in this instance.
    public void erodeBackground(int diameter) {
        backgroundImage.loadPixels();
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
        backgroundImage = output;
        updateBackground();
    }

    private boolean fitKernel(int x, int y, int radius) {
        for (int i = -radius; i < radius; i++) {
            for (int j = -radius; j < radius; j++) {
                if (backgroundImage.get(x + i, y + j) == Color.WHITE)
                    return false;
            }
        }
        return true;
    }

    public void ellipse(float x, float y, float lambda1, float lambda2, float orientation) {
        stippleImage.beginDraw();
        stippleImage.pushMatrix();
        stippleImage.translate(x, y);
        stippleImage.rotate(orientation);
        stippleImage.ellipse(0,0, (float)(lambda1), (float)(lambda2));
        stippleImage.popMatrix();
        stippleImage.beginDraw();
    }

    public void changeBackground(PImage bg) {
        this.backgroundImage = bg;
        updateBackground();
    }

    // TODO method to check boundaries and handle black cells near edges, contours etc.

}
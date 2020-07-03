import processing.core.*;
import java.util.ArrayList;

public class RLBGStippling extends PApplet {


    // MAIN ########################################################################################################
    int n = 1000;
    ArrayList<Stipple> stipples;
    StippleGenerator rLBGStippleGenerator;
    Painter painter;
    PImage reference, bg, image;

    public void settings() {
        reference = loadImage("input2.jpg"); // Load the image into the program
        size(reference.width*2, reference.height*2, P3D);
    }

    public void setup() {
        reference.filter(GRAY);
        rLBGStippleGenerator = new StippleGenerator(this, reference, new Options(4, 50, 1, true));
        painter = new Painter(this, rLBGStippleGenerator);
        image = painter.paint();
    }

    public void draw() { image(image,0,0); }

    public void mousePressed() {
        Cell cell = rLBGStippleGenerator.wrv.getCell(mouseX, mouseY);
        println("Area: " + cell.area + " Avg Density: " + cell.avgDensity + " Ecc: " + cell.eccentricity + " Cv: " + cell.cv);
        boolean reversible = rLBGStippleGenerator.testReversibility(cell);
        println(reversible);
        if (reversible) {
            rLBGStippleGenerator.flipCell(cell);
            Stipple s = rLBGStippleGenerator.getStipple(cell.index);
            println("Centroid: x:" + cell.centroid.x + " y: " + cell.centroid.y);
            image = painter.paint();
        }

//        float x = cell.moments[4] / cell.moments[0] - cell.centroid.x * cell.centroid.x;
//        float y = cell.moments[3] / cell.moments[0] - cell.centroid.x * cell.centroid.y; // Stopped multiplying this by 2 because no point
//        float z = cell.moments[5] / cell.moments[0] - cell.centroid.y * cell.centroid.y;
//        // Eccentricity
//        double l = (x + z) / 2 + Math.sqrt(4 * Tools.sq(y) + Tools.sq(x - z)) / 2;
//        l = Math.sqrt(8 * l);
//        double w = (x + z) / 2 - Math.sqrt(4 * Tools.sq(y) + Tools.sq(x - z)) / 2;
//        w = Math.sqrt(8 * w);
//        painter.painting.beginDraw();
//        painter.painting.pushMatrix();
//        painter.painting.noFill();
//        painter.painting.strokeWeight(2);
//        painter.painting.stroke(255,0,0);
//        painter.painting.translate(cell.centroid.x, cell.centroid.y);
//        painter.painting.rotate(cell.orientation);
//        painter.painting.ellipse(0,0, (float)l, (float)w);
//        painter.painting.popMatrix();
//        painter.painting.endDraw();
    }

    public void keyPressed() {
        if (key == 'i') {
            rLBGStippleGenerator.iterate();
            System.out.println(rLBGStippleGenerator.getStipples().size());
            System.out.println("Iteration " + rLBGStippleGenerator.status.iterations + " complete");
            System.out.println("Splits: " + rLBGStippleGenerator.status.splits + " Merges: " + rLBGStippleGenerator.status.merges );

            image = painter.paint();
        }
        if (key == 'c') {
            rLBGStippleGenerator.connectReverseCells();
            image = painter.paint();

        }
        if (key == 't') {
            painter.showStippleIndexes = !painter.showStippleIndexes;
            image = painter.paint();
        }
        if (key == 's') {
            painter.paintAddedSites = !painter.paintAddedSites;
            image = painter.paint();
        }
        if (key == 'r') {
            rLBGStippleGenerator.restart(reference, new Options(3, 50, 1, true));
            image = painter.paint();
        }
        if (key == 'e') {
            painter.smoothBackground();
            image = painter.getStippleImage();
        }

        if (key == 'b') {
            image = painter.getBackgroundImage();
        }

        if (key == 'a') {
            PImage thref = reference.copy();
            thref.filter(BLUR, 2);
            thref.filter(THRESHOLD, 0.5f);

            println(Tools.computeOtsuThreshold(reference));
            painter.changeBackground(thref);
            painter.erodeBackground(4);
            painter.erodeBackground(4);

            image = painter.getStippleImage();
        }
        if (key == 'p') {
            PImage thref = reference.copy();
            thref.filter(BLUR, 2);
            thref.filter(THRESHOLD, Tools.computeOtsuThreshold(reference));
            thref = Imp.dilate(thref, 2);
            thref = Imp.erode(thref, 2);
            thref = Imp.dilate(thref, 2);
            thref = Imp.erode(thref, 2);
            painter.changeBackground(thref);

            image = painter.getStippleImage();
        }
    }

    public static void main(String[] args) {
        PApplet.main("RLBGStippling", args);
    }
}

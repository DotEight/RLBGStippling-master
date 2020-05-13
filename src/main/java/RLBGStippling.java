import processing.core.*;
import java.util.ArrayList;

public class RLBGStippling extends PApplet {


    // MAIN ########################################################################################################
    int n = 1000;
    ArrayList<Stipple> stipples;
    StippleGenerator RLBGStippleGenerator;
    Painter painter;
    PImage reference, bg, image;

    public void settings() {
        reference = loadImage("input1.jpg"); // Load the image into the program
        size(reference.width, reference.height, P3D);
    }

    public void setup() {
        reference.filter(GRAY);
        RLBGStippleGenerator = new StippleGenerator(this, reference, new Options(4, 50, 1, true));
        painter = new Painter(this, RLBGStippleGenerator);
        image = painter.paint();
    }

    public void draw() {
        image(image, 0, 0);
    }

    public void mousePressed() {
        Cell cell = RLBGStippleGenerator.wrv.getCell(mouseX, mouseY);
        println("Area: " + cell.area + " avg density:" + cell.avgDensity + "Ecc: " + cell.eccentricity);
        boolean reversible = RLBGStippleGenerator.testReversibility(cell);
        println(reversible);
        if (reversible)
            RLBGStippleGenerator.flipCell(cell);

        painter.paint();
    }

    public void keyPressed() {
        if (key == 'i') {
            RLBGStippleGenerator.iterate();
            image = painter.paint();
        }
        if (key == 'c') {
            RLBGStippleGenerator.connectReverseCells();
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
            RLBGStippleGenerator.restart(reference, new Options(3, 50, 1, true));
            image = painter.paint();
        }

        if (key == 'e') {
            painter.smoothBackground();
            image = painter.getPainting();
        }

        if (key == 'b') {
            image = painter.getBackground();
        }
    }

    public static void main(String[] args) {
        PApplet.main("RLBGStippling", args);
    }
}

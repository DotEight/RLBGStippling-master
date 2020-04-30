import processing.core.*;
import java.util.ArrayList;

public class RLGBStippling extends PApplet {


    // MAIN ########################################################################################################
    int n = 1000;
    ArrayList<Stipple> stipples;
    Stippler rlgbStippler;
    Painter painter;
    PImage reference, bg, image;

    public void settings() {
        reference = loadImage("input2.jpg"); // Load the image into the program
        size(reference.width, reference.height, P3D);
    }

    public void setup() {
        reference.filter(GRAY);
        rlgbStippler = new Stippler(this, reference, new Options(4, 50, 1, true));
        painter = new Painter(this, rlgbStippler);
        image = painter.paint();
    }

    public void draw() {
        image(painter.getPainting(), 0, 0);
    }

    public void mousePressed() {
        int index = Tools.colorToInt(rlgbStippler.wrv.diagram.get(mouseX, mouseY));
        Cell cell = rlgbStippler.wrv.cells.get(index);

        println(cell.area + " avg density:" + cell.avgDensity);
    }

    public void keyPressed() {
        if (key == 'i') {
            rlgbStippler.iterate();
            image = painter.paint();
        }
        if (key == 'c') {
            rlgbStippler.connectReverseCells();
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

        if (key == 'e') {
            painter.erodeBackground();
            painter.updatePainting();
        }

        if (key == 'r') {
            rlgbStippler.restart(new Options(3, 50, 2, true));
            image = painter.paint();
        }
    }

    public static void main(String[] args) {
        PApplet.main("RLGBStippling", args);
    }
}

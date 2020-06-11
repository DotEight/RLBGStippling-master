import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PApplet;

import java.util.*;
import java.util.stream.Collectors;

import static processing.core.PApplet.dist;
import static processing.core.PConstants.*;

// VORONOI CLASS #################################################################################################
public class WRVoronoi {

    ArrayList<Point> sites;
    ArrayList<Cell> cells;
    float[][] densityMatrix;
    private final int w;
    private final int h;
    float th;

    PApplet pa;
    PImage diagram;

    WRVoronoi(PApplet pa, ArrayList<Point> sites, float[][] densityMatrix, float th) {
        this.pa = pa;
        this.sites = sites;
        this.densityMatrix = densityMatrix;
        this.w = densityMatrix.length;
        this.h = densityMatrix[0].length;
        this.th = th;

        this.cells = new ArrayList<>(sites.size());
        this.diagram = createDiagram();
    }

    void setSites(ArrayList<Point> sites) {
        this.sites = sites;
        this.cells.clear();
        this.diagram = createDiagram();
    }

    PImage createDiagram() {

        PGraphics dg = pa.createGraphics(w, h, P3D);
        float r = dist(0, 0, w, h);
        float sides = 64;
        float p = Tools.TWO_PI / sides;

        dg.noSmooth();
        dg.beginDraw();
        dg.ortho(-w / 2f, w / 2f, -h / 2f, h / 2f, 0, 2 * r);
        dg.noStroke();
        for (int i = 0; i < sites.size(); i++) {
            Point site = sites.get(i);
            int color = indexToColor(i);
            dg.fill(color);
            dg.pushMatrix();
            dg.translate(site.x, site.y, 0);

            float angle = 0;
            dg.beginShape(QUAD_STRIP);
            for (int j = 0; j < sides; j++) {
                dg.vertex(0, 0, 0);
                dg.vertex(r * (float) Math.cos(angle), r * (float) Math.sin(angle), -r);
                angle += p;
            }
            dg.vertex(0, 0, 0);
            dg.vertex(r, 0, -r);
            dg.endShape();
            dg.popMatrix();
        }
        PImage dimg = dg.get();
        dg.endDraw();

        return dimg;
    }

    // CELL FUNCTIONS #################################################################################################

    public ArrayList<Cell> collectCells() {

        this.cells.clear();
        for (int i = 0; i < sites.size(); i++) {
            this.cells.add(new Cell(i, sites.get(i)));
        }

        this.diagram.loadPixels();
        for (int y = 0; y < diagram.height; y++) {
            for (int x = 0; x < diagram.width; x++) {
                int index = colorToIndex(diagram.pixels[x + y * diagram.width]);

                Cell cell = this.cells.get(index);
                cell.pixelList.add(new Point(x, y));
                cell.area++;
                cell.sumDensity += densityMatrix[x][y];
            }
        }

        for (Cell c : this.cells) {
            // Check cell density to determine whether to apply reverse stippling
            if (c.sumDensity == 0)
                continue;

            if (c.sumDensity / c.area > this.th) {
                c.reverse = 1;
            }
            c.calculateProperties(densityMatrix);
            //calculateCellProperties(c);
        }

        return cells;
    }

    void calculateCellProperties(Cell c) {

        // Calculate moments and variance accordingly
        float avgDensity = c.sumDensity / c.area;
        c.avgDensity = avgDensity;

        float sigmaDiff = 0;
        c.moments[0] = Math.abs(c.reverse * c.area - c.sumDensity);

        for (Point p : c.pixelList) {
            // Cast point to pixel location
            int x = (int) p.x;
            int y = (int) p.y;
            float densityValue = Math.abs(c.reverse - densityMatrix[x][y]);

            c.moments[1] += x * densityValue;
            c.moments[2] += y * densityValue;
            c.moments[3] += x * y * densityValue;
            c.moments[4] += x * x * densityValue;
            c.moments[5] += y * y * densityValue;

            float diff = avgDensity - densityMatrix[x][y];
            sigmaDiff += diff * diff;
        }

        // Calculate higher order properties
        // Coefficient of variation
        c.cv = (float) Math.sqrt(sigmaDiff / c.area) / avgDensity;

        // Moments are placed in the array from 0 to 5 with the order: m00, m10, m01, m11, m20, m02
        float[] m = c.moments;

        // Centroid
        c.centroid.setX(m[1] / m[0]);
        c.centroid.setY(m[2] / m[0]);

        // Central moments
        float x = m[4] / m[0] - c.centroid.x * c.centroid.x;
        float y = m[3] / m[0] - c.centroid.x * c.centroid.y; // Stopped multiplying this by 2 because no point
        float z = m[5] / m[0] - c.centroid.y * c.centroid.y;

        // Orientation
        c.orientation = (float) (0.5 * Math.atan(2 * y / (x - z)));

        // Eccentricity
        double lambda1 = (x + z) / 2 + Math.sqrt(4 * Tools.sq(y) + Tools.sq(x - z)) / 2;
        double lambda2 = (x + z) / 2 - Math.sqrt(4 * Tools.sq(y) + Tools.sq(x - z)) / 2;
        c.eccentricity = (float) Math.sqrt((1- lambda2 / lambda1));
    }

    List<Cell> getKNearestNeighbours(Cell a, int k) {
        ArrayList<Cell> neighbours = new ArrayList<>(cells);

        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        List<Cell> gridNeighbours = neighbours.stream()
                .filter(cell -> cell.site.x > a.site.x - diagram.width / 5f
                                && cell.site.x < a.site.x + diagram.width / 5f
                                && cell.site.y > a.site.y - diagram.width / 5f
                                && cell.site.y < a.site.y + diagram.width / 5f)
                .sorted(new DistanceToCellComparator(a))
                .collect(Collectors.toList());

        //final Map<String, Integer> sortedMap = distMap.entrySet().stream().sorted(comparingByValue());
        return gridNeighbours.subList(1, Math.min(k + 1, gridNeighbours.size()));
    }

    ArrayList<Cell> getDelaunayNeighbours(Cell a) {

        ArrayList<Cell> otherCells = new ArrayList<>(cells);
        ArrayList<Cell> neighbours = new ArrayList<>();

        for (int bPointer = 0; bPointer < otherCells.size() - 1; bPointer++) {
            if (bPointer == a.index)
                continue;

            Cell b = cells.get(bPointer);

            for (int cPointer = bPointer + 1; cPointer < otherCells.size(); cPointer++) {
                if (cPointer == a.index)
                    continue;

                Cell c = cells.get(cPointer);

                if (testDelaunayTriangle(a, b, c)) {
                    if (!neighbours.contains(b))
                        neighbours.add(b);
                    if (!neighbours.contains(c))
                        neighbours.add(c);
                }
            }
        }
        return neighbours;
    }

    private boolean testDelaunayTriangle(Cell a, Cell b, Cell c) {
        boolean tri = true;
        Circle circle = new Circle(a.site, b.site, c.site);

        for (Cell tc : cells) {
            if (tc == a || tc == b || tc == c)
                continue;

            if (circle.includesPoint(tc.site)) {
                //println("circle includes " + tc.index);
                tri = false;
                break;
            }
        }
        return tri;
    }
    // TODO: full delaunay implementation

    public Cell getCell(int x, int y) {
        int index = colorToIndex(diagram.get(x, y));
        return cells.get(index);
    }

    public int colorToIndex(int c) {
        int r = (c>> 16 & 0xFF);
        int g = (c>> 8 & 0xFF);
        int b = (c & 0xFF);
        return r + (g<<8) + (b<<16);
    }

    public int indexToColor(int i) {
        int r = i & 0xFF;
        int g = (i>>8) & 0xFF;
        int b = (i>>16) & 0xFF;
        return (r << 16) | (g << 8) | b | 0xFF000000;
    }

}


package com.rlbgs;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static processing.core.PApplet.dist;
import static processing.core.PConstants.P3D;
import static processing.core.PConstants.QUAD_STRIP;

// VORONOI CLASS #################################################################################################
public class WRVoronoi {

    ArrayList<Point> sites;
    ArrayList<Cell> cells;

    private final PApplet pa;
    private final float[][] densityMatrix;
    private final int w;
    private final int h;
    private final float th;
    private PImage diagram;

    WRVoronoi(PApplet pa, int w, int h, float[][] densityMatrix, float th) {
        this.pa = pa;
        this.densityMatrix = densityMatrix;
        this.w = w;
        this.h = h;
        this.th = th;
    }

    WRVoronoi(PApplet pa, int w, int h, float[][] densityMatrix, float th, ArrayList<Point> sites) {
        this.pa = pa;
        this.densityMatrix = densityMatrix;
        this.w = w;
        this.h = h;
        this.th = th;
        setSites(sites);
    }

    public void setSites(ArrayList<Point> sites) {
        this.sites = sites;
        this.cells = new ArrayList<>(sites.size());
        for (int i = 0; i < sites.size(); i++) {
            this.cells.add(new Cell(i, sites.get(i)));
        }
    }

    public PImage getDiagram() {
        return this.diagram;
    }

    private PImage createDiagram() {

        PGraphics dg = pa.createGraphics(w, h, P3D);
        dg.noSmooth();

        float r = dist(0, 0, w, h);
        float sides = 64;
        float p = Tools.TWO_PI / sides;

        dg.beginDraw();
        dg.ortho(-w / 2f, w / 2f, -h / 2f, h / 2f, 0, 2 * r);
        dg.noStroke();
        for (int i = 0; i < sites.size(); i++) {
            Point site = sites.get(i);
            int color = indexToColor(i);
            dg.fill(color);
            dg.pushMatrix();
            dg.translate(site.x + 0.5f, site.y + 0.5f, 0);

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
        dg.save("/Users/kerem/Desktop/save.jpg");
        return dimg;
    }

    // CELL FUNCTIONS #################################################################################################
    public ArrayList<Cell> collectConstrainedCells(float[][] densityMatrix, PImage mask) {

        this.cells = new ArrayList<>(sites.size());
        for (int i = 0; i < sites.size(); i++) {
            this.cells.add(new Cell(i, sites.get(i)));
        }

        this.diagram.loadPixels();
        mask.loadPixels();
        for (int y = 0; y < diagram.height; y++) {
            for (int x = 0; x < diagram.width; x++) {
                if (mask.pixels[x + mask.width * y] == pa.color(0)) {
                    int index = colorToIndex(diagram.pixels[x + y * diagram.width]);
                    Cell cell = this.cells.get(index);
                    cell.pixelList.add(new Point(x, y));
                    cell.area++;
                    cell.sumDensity += densityMatrix[x][y];
                }
            }
        }

        for (Cell c : this.cells) {
            c.avgDensity = c.sumDensity / c.area;
            // Check cell density to determine whether to apply reverse stippling
            c.reverse = 1;
            c.calculateProperties(densityMatrix);
        }

        return cells;
    }

    public ArrayList<Cell> collectCells(float[][] densityMatrix, boolean evaluate) {
        this.diagram = createDiagram();
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
            c.avgDensity = c.sumDensity / c.area;

            // Check cell density to determine whether to apply reverse stippling
            if (evaluate) {
                if (c.avgDensity > this.th) {
                    c.reverse = 1;
                } else {
                    c.reverse = 0;
                }
            }
            c.calculateProperties(densityMatrix);
        }

        return cells;
    }

    public void calculateCellProperties(Cell cell) {
        for (Cell c : this.cells) {
            c.avgDensity = c.sumDensity / c.area;
            // Check cell density to determine whether to apply reverse stippling

            if (c.avgDensity > this.th) {
                c.reverse = 1;
            } else {
                c.reverse = 0;
            }

            c.calculateProperties(densityMatrix);
            //calculateCellProperties(c);
        }
    }

    public ArrayList<Cell> relax(float[][] densityMatrix) {

        for (Cell c : this.cells) {
            sites.set(c.index, new Point(c.centroid.x, c.centroid.y));
            c.resetProperties();
            c.pixelList = new ArrayList<>();
        }

        this.diagram = createDiagram();
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
            c.avgDensity = c.sumDensity / c.area;
            c.calculateProperties(densityMatrix);
        }

        this.diagram.save("/Users/kerem/Desktop/save.jpg");
        return cells;
    }


    List<Cell> getKNearestNeighbours(Cell a, int k) {
        ArrayList<Cell> neighbours = new ArrayList<>(cells);
        float gridSize = diagram.width;
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        List<Cell> gridNeighbours = neighbours.stream()
                .filter(cell -> cell.site.x > a.site.x - gridSize
                        && cell.site.x < a.site.x + gridSize
                        && cell.site.y > a.site.y - gridSize
                        && cell.site.y < a.site.y + gridSize)
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


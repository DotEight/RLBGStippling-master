package com.rlbgs;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.*;

// STIPPLER CLASS #################################################################################################
class StippleGenerator {

    public Status status;
    public Options options;
    public PImage img;
    public PApplet pa;
    public WRVoronoi wrv;
    private float[][] densityMatrix;
    private ArrayList<Point> stippleSites;
    private ArrayList<Cell> stippleCells;
    private ArrayList<Stipple> acceptedStipples;
    private ArrayList<Stipple> splitStipples;
    private ArrayList<Stipple> deletedStipples;
    private ArrayList<Stipple> stipples;
    private float th;
    public float[][] grad;


    StippleGenerator(PApplet pa, PImage img, Options options, float th) {
        this.pa = pa;
        this.status = new Status();
        this.options = options;
        this.th = th;
        //int ssf = options.superSamplingFactor;
        //img.resize(img.width * ssf, 0);
        this.img = img;
        this.densityMatrix = Tools.createDensityMatrix(img);
        this.wrv = new WRVoronoi(pa, densityMatrix.length, densityMatrix[0].length, densityMatrix, 1 - (th / 255));
        initStipples();
    }

    public PImage modifyDensityMatrix(PImage adjustImage, int factor) {
        PImage gradient = Imp.getContourGradient(factor);
        PImage newimg = pa.createImage(img.width, img.height, PConstants.ARGB);
        float[][] grad = Tools.createDensityMatrix(gradient);
        int black = pa.color(0);
        int white = pa.color(255);
        newimg.loadPixels();
        adjustImage.loadPixels();
        for (int x = 0; x < densityMatrix.length; x++) {
            for (int y = 0; y < densityMatrix[0].length; y++) {
                float value = densityMatrix[x][y];
                int index = x + adjustImage.width * y;
                int color = adjustImage.pixels[index];
                if (color == black) {
                    densityMatrix[x][y] = Math.max(0, Math.min(1, densityMatrix[x][y] + grad[x][y] / 2));
                } else {
                    densityMatrix[x][y] = Math.max(0, Math.min(1, densityMatrix[x][y] + grad[x][y] / 2));
                }
                newimg.pixels[index] = pa.color(1 - densityMatrix[x][y]) * 255;
            }
        }
        newimg.updatePixels();
        return newimg;
    }

    public void restart(PImage img, Options options, float th) {
        this.status = new Status();
        this.options = options;

        this.th = th;
        this.img = img;

        this.stippleSites.clear();
        this.stipples.clear();
        this.deletedStipples.clear();
        this.splitStipples.clear();
        this.acceptedStipples.clear();

        this.densityMatrix = Tools.createDensityMatrix(img);
        this.wrv = new WRVoronoi(pa, densityMatrix.length, densityMatrix[0].length, densityMatrix, 1 - (th / 255));
        initStipples();
    }

    public ArrayList<Stipple> getStipples() {
        return stipples;
    }

    public ArrayList<Stipple> getAcceptedStipples() {
        return acceptedStipples;
    }

    public ArrayList<Stipple> getSplitStipples() {
        return splitStipples;
    }

    public ArrayList<Stipple> getDeletedStipples() {
        return deletedStipples;
    }

    public void addStipples(ArrayList<Stipple> stipples) {
        if (stipples != null)
            this.stipples.addAll(stipples);
    }

    public ArrayList<Cell> getStippleCells() {
        return stippleCells;
    }

    public ArrayList<Point> getStippleSites() {
        ArrayList<Point> sites = new ArrayList<>(stipples.size());
        for (Stipple s : stipples) {
            sites.add(s.location);
        }
        return sites;
    }

    private void initStipples() {
        this.stippleSites = new ArrayList<>(options.initialStipples);
        this.stipples = new ArrayList<>(stippleSites.size());
        this.deletedStipples = new ArrayList<>(stippleSites.size());
        this.splitStipples = new ArrayList<>(stippleSites.size());
        this.acceptedStipples = new ArrayList<>(stippleSites.size());

        int i = 0;
        do {
            Point candidatePoint = new Point(Tools.random(densityMatrix.length), Tools.random(densityMatrix[0].length));

            boolean pointAccepted = false;
            if (densityMatrix[(int) candidatePoint.x][(int) candidatePoint.y] > 0.1f)
                pointAccepted = true;

            if (pointAccepted) {
                stipples.add(new Stipple(-1, candidatePoint, pa.color(255, 0, 0), options.initialStippleDiameter));
                stippleSites.add(candidatePoint);
                i++;
            }
        } while (i < this.options.initialStipples);
    }

    public void iterate() {
        this.status.splits = 0;
        this.status.merges = 0;

        stippleSites = getStippleSites();

        wrv.setSites(stippleSites);
        stippleCells = wrv.collectCells(densityMatrix, true);
        stipples.clear();
        deletedStipples.clear();
        splitStipples.clear();
        acceptedStipples.clear();

        this.status.hysteresis = computeCurrentHysteresis();

        generateStipples();
    }

    public void adjustIterate(PImage adjustImage) {
        this.status.splits = 0;
        this.status.merges = 0;

        stippleSites = getStippleSites();

        wrv.setSites(stippleSites);
        stippleCells = wrv.collectCells(densityMatrix, false);
        adjustStipples(adjustImage);
        stipples.clear();
        deletedStipples.clear();
        splitStipples.clear();
        acceptedStipples.clear();

        this.status.hysteresis = computeCurrentHysteresis();

        generateStipples();
    }

    private void generateStipples() {
        for (Cell cell : stippleCells) {

            float sd = getStippleDiameter(cell);
            if (Float.isNaN(sd)) {
                PApplet.println(wrv.indexToColor(cell.index));
                PApplet.println(wrv.colorToIndex(wrv.indexToColor(cell.index)));
            }

            float[] thresholds = getThresholds(cell, sd, this.status.hysteresis);
            float lowerThreshold = thresholds[0];
            float upperThreshold = thresholds[1];
            float cellDensity = cell.moments[0];

            Stipple s;

            if (cellDensity < lowerThreshold || cell.area == 0) {
                // Merge cell (dont do anything)
                deletedStipples.add(new Stipple(cell.index, cell.centroid, pa.color(255, 0, 0), sd));

                this.status.merges++;

            } else if (cellDensity > upperThreshold) {
                if (cell.reverse != 1 || cell.cv > 0.0) {
                    // Split cell according to cell size and orientation
                    float splitAmount = (float) (0.5f * sqrt(max(1.0f, cell.area) / PI));
                    float angle = cell.orientation;
                    float splitX = (float) (splitAmount * cos(angle + PI / 2));
                    float splitY = (float) (splitAmount * sin(angle + PI / 2));

                    Point splitSeed1 = new Point(cell.centroid.x - splitX, cell.centroid.y - splitY);
                    Point splitSeed2 = new Point(cell.centroid.x + splitX, cell.centroid.y + splitY);
                    Tools.addJitter(splitSeed1, 0.001f);
                    Tools.addJitter(splitSeed2, 0.001f);

                    // stippleSites.add(splitSeed1);
                    // stippleSites.add(splitSeed2);

                    splitStipples.add(new Stipple(cell.index, splitSeed1, pa.color(0, 255, 0), sd));
                    splitStipples.add(new Stipple(cell.index, splitSeed2, pa.color(0, 255, 0), sd));

                    this.status.splits++;
                }
            } else {
                s = new Stipple(cell.index, cell.centroid, cell.reverse * 255, sd);
                acceptedStipples.add(s);
                // stippleSites.add(cell.centroid);
            }
        }
        stipples.addAll(splitStipples);
        stipples.addAll(acceptedStipples);
        this.status.iterations++;
        this.status.size = stipples.size();
    }

    public void relax() {
        ArrayList<Point> sites = getStippleSites();
        wrv.setSites(sites);
        stippleCells = wrv.collectCells(densityMatrix, true);
        stipples.clear();

        int count = 0;
        for (Cell cell : stippleCells) {
            float sd = getStippleDiameter(cell);
            if (Float.isNaN(sd))
                count++;
            stipples.add(new Stipple(cell.index, cell.centroid, pa.color(cell.reverse * 255), sd));
        }

        System.out.println("NaN stipples after relaxation: " + count);
    }

    public void adjustRelax(PImage adjustImage) {
//        for (Cell c : getStippleCells()) {
//            evaluateCell(c, adjustImage);
//        }
        ArrayList<Point> sites = getStippleSites();
        wrv.setSites(sites);
        stippleCells = wrv.collectCells(densityMatrix, false);
        //stippleCells = wrv.relax(densityMatrix);

        //stipples.clear();
        adjustImage.loadPixels();
        for (Cell c : getStippleCells()) {
            evaluateCell(c, adjustImage);
            float sd = getStippleDiameter(c);
            //float sd = stipples.get(c.index).size;
            stipples.set(c.index, new Stipple(c.index, c.centroid, pa.color(c.reverse * 255), sd));
        }
    }

    public void adjustStipples(PImage adjustImage) {
        //ArrayList<Point> sites = getStippleSites();
        //wrv.setSites(sites);
        //stippleCells = wrv.collectCells(densityMatrix, false);
        stipples.clear();
        adjustImage.loadPixels();
        for (Cell c : getStippleCells()) {
            evaluateCell(c, adjustImage);
        }
    }

    private void evaluateCell(Cell c, PImage adjustImage) {
        ArrayList<Point> pixels = c.pixelList;
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

        int ci = (int) c.centroid.x + adjustImage.width * (int) c.centroid.y;
        int cc = adjustImage.pixels[ci];
        if (cc == pa.color(255)) {
            c.reverse = 0;
            c.pixelList = whitePixels;
        } else if (cc == pa.color(0)) {
            c.reverse = 1;
            c.pixelList = blackPixels;
        } else {
            c.pixelList = new ArrayList<>();
            c.pixelList.add(new Point(0, 0));
        }

        if (whitePixels.size() != 0 && blackPixels.size() != 0)
            c.onBorder = 1;

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

        c.calculateProperties(densityMatrix);
        c.pixelList = pixels;
    }

    public void fixStippleColors(PImage background) {
        background.loadPixels();
        for (Stipple s : stipples) {
            int c = background.pixels[(int) s.location.x + ((int) s.location.y) * background.width];
            if (c == pa.color(255)) {
                s.c = pa.color(0);
            } else {
                s.c = pa.color(255);
            }
        }
    }

    public void cleanEccentricCells() {
        ArrayList<Stipple> newStipples = new ArrayList<>();
        for (Cell c : getStippleCells()) {
            System.out.println(c.eccentricity);
            //float th = c.eccentricity * abs(c.reverse - c.avgDensity) * c.cv;
            boolean th = c.eccentricity < 0.75 && abs(c.reverse - c.avgDensity) > 0.001 && c.cv < 10;
            if (th) {
                float sd = getStippleDiameter(c);
                newStipples.add(new Stipple(c.index, c.centroid, pa.color(c.reverse * 255), sd));
            }
        }
        stipples = newStipples;
        ArrayList<Point> sites = getStippleSites();
        wrv.setSites(sites);
        stippleCells = wrv.collectCells(densityMatrix, false);
    }

    public void cleanBorders(int borderSize) {
        PImage background = Imp.createTriMap(borderSize);
        background.loadPixels();
        ArrayList<Stipple> newStipples = new ArrayList<>();
        for (int i = 0; i < stipples.size(); i++) {
            int x = (int) stipples.get(i).location.x;
            int y = (int) stipples.get(i).location.y;
            int index = x + background.width * y;
            int color = background.pixels[index];
            if (color == pa.color(0) || color == pa.color(255)) {
                newStipples.add(stipples.get(i));
            }
        }
        stipples = newStipples;
    }

    public void cleanUp() {
        ArrayList<Stipple> newStipples = new ArrayList<>();
        for (int i = 0; i < stipples.size(); i++) {
            if (!isAlone(stipples.get(i))) {
                newStipples.add(stipples.get(i));
            }
        }
        stipples = newStipples;
    }

    public boolean isAlone(Stipple s) {
        Point l1 = s.location;
        boolean alone = true;

        for (int j = 0; j < stipples.size(); j++) {
            if (s.index == j)
                continue;

            Point l2 = stipples.get(j).location;
            if (l1.distanceTo(l2) < 5) {
                alone = false;
                break;
            }
        }

        return alone;
    }

    public float computeCurrentHysteresis() {
        return options.initialHysteresis + status.iterations * options.hysteresisDelta;
    }

    public boolean finished() {
        return ((status.splits == 0 && status.merges == 0) || (status.iterations == options.maxIterations)) && (status.iterations > 1);
    }

    private float getStippleDiameter(Cell cell) {
//        if(cell.onBorder == 1)
//            return options.stippleSizeMin;
        if (options.adaptiveStippleSize) {
            // First element in moments array is the sum density that also takes reversed status into account
            // If the cell is reversed, lighter areas are considered more dense
            //float cellDensity = abs(cell.reverse * cell.area - cell.sumDensity);
            float cellDensity = cell.moments[0];
            float avgIntensitySqrt = (float) sqrt(cellDensity / cell.area);
            return options.stippleSizeMin * (1.0f - avgIntensitySqrt) + options.stippleSizeMax * avgIntensitySqrt;
        } else {
            return options.initialStippleDiameter;
        }
    }

    private float[] getThresholds(Cell cell, float stippleDiameter, float hysteresis) {
        float stippleArea = (float) (PI * pow(stippleDiameter / 2.0f, 2));
        float lowerValue = (float) ((1.0f - hysteresis / 2.0f) * stippleArea * pow(options.superSamplingFactor, 2));

        float upperValue = (float) ((1.0f + hysteresis / 2.0f) * stippleArea * pow(options.superSamplingFactor, 2));
        //upperValue = upperValue + upperValue * cell.stdev;
        return new float[]{lowerValue, upperValue};
    }

    private double[] areaThresholds(List<Cell> cells) {
/*        cells.sort((c1, c2) -> {
            int area1 = (int) c1.area;
            int area2 = (int) c2.area;
            return area1 - area2;
        });*/
        double[] areaValues = new double[cells.size()];

        for (int i = 0; i < areaValues.length; i++) {
            areaValues[i] = cells.get(i).area;
        }

        return Tools.outlierThresholds(areaValues);
    }

    // TODO fix this method
    public void connectReverseCells() {
        System.out.println("Attempting to connect reverse cells");
        for (Cell cell : stippleCells) {
            boolean shouldReverse = testReversibility(cell);

            if (shouldReverse) {
                flipCell(cell);
            }
        }
        System.out.println("Cells connected");
    }

    public boolean testReversibility(Cell cell) {
        if (cell.reverse == 1 || cell.eccentricity >= 1.0f) {
            //System.out.println("FALSE: com.rlgbs.Cell is reverse OR eccentricity is greater than 1");
            return false;
        }

        List<Cell> neighbours = this.wrv.getKNearestNeighbours(cell, 5);
        double[] areaThresholds = areaThresholds(neighbours);
        ArrayList<Cell> reverseNeighbours = new ArrayList<>(neighbours.size());
        for (Cell n : neighbours) {
            if (n.reverse == 1)
                reverseNeighbours.add(n);
        }

        if (reverseNeighbours.size() <= 2 || cell.area > areaThresholds[1] || cell.eccentricity > 0.85f) {
            //System.out.println("FALSE: Not enough reverse neighbours OR cell is outlier OR cell eccentricity greater than 1");
            return false;
        }

        for (int[] combination : Tools.generateCombinations(reverseNeighbours.size(), 2)) {
            Cell c2 = reverseNeighbours.get(combination[0]);
            Cell c3 = reverseNeighbours.get(combination[1]);
            float l = linearity(cell.centroid, c2.centroid, c3.centroid);
            System.out.println("Linearity of " + cell.index + ", " + c2.index + " and " + c3.index + " is " + l);
            double angle = 3 * PI / 4;
            if (l > angle) {
                System.out.println("TRUE: com.rlgbs.Cell is between 2 reverse cells because linearity greater than " + angle);
                return true;
            }
        }

        return false;
    }

    private double areaAverage(List<Cell> neighbours) {
        double sum = 0;
        for (Cell c : neighbours) {
            sum += c.area;
        }
        return sum / neighbours.size();
    }

    private float linearity(Point p1, Point p2, Point p3) {
        double angle1 = atan2(p1.y - p2.y,
                p1.x - p2.x);
        double angle2 = atan2(p1.y - p3.y,
                p1.x - p3.x);
        double theta = angle1 - angle2;
        theta = theta - Tools.TWO_PI * Math.floor((theta + Tools.PI) / Tools.TWO_PI);
        return (float) abs(theta);
    }

    public void flipCell(Cell cell) {
        cell.reverse = abs(cell.reverse - 1);
        cell.calculateProperties(densityMatrix);
    }
}
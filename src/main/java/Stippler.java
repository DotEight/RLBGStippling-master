import org.paukov.combinatorics3.Generator;
import processing.core.PApplet;
import processing.core.PImage;

import java.util.*;

import static java.lang.Math.*;

// STIPPLER CLASS #################################################################################################
class Stippler {

    public Status status;
    public Options options;
    public PImage img;
    public PApplet pa;

    private float[][] densityMatrix;
    private ArrayList<Point> stippleSites;
    private ArrayList<Cell> stippleCells;
    private ArrayList<Stipple> stipples;
    public WRVoronoi wrv;

    Stippler(PApplet pa, PImage img, Options options) {
        this.pa = pa;
        this.status = new Status();
        this.options = options;
        this.img = img;

        this.densityMatrix = Tools.createDensityMatrix(img);
        initStipples();
    }

    public void restart(Options options) {
        this.status = new Status();
        this.options = options;

        this.stippleSites.clear();
        this.stipples.clear();
        initStipples();
    }

    void initStipples() {
        this.stippleSites = new ArrayList<>(options.initialStipples);
        this.stipples = new ArrayList<>(options.initialStipples);

        int i = 0;
        do {
            Point candidatePoint = new Point(Tools.random(densityMatrix.length), Tools.random(densityMatrix[0].length));

            boolean pointAccepted = false;
            if (densityMatrix[(int)candidatePoint.x][(int)candidatePoint.y] > 0.5f)
                pointAccepted = true;

            if (pointAccepted) {
                stippleSites.add(candidatePoint);
                stipples.add(new Stipple(candidatePoint, pa.color(0), options.initialStippleDiameter));
                i++;
            }
        } while (i<this.options.initialStipples);

        this.wrv = new WRVoronoi(pa, stippleSites, densityMatrix, Tools.computeOtsuThreshold(img));
        this.stippleCells = wrv.collectCells();
    }

    public ArrayList<Stipple> getStipples() {
        return stipples;
    }

    public ArrayList<Cell> getStippleCells() {
        return stippleCells;
    }

    public ArrayList<Point> getStippleSites() {
        return stippleSites;
    }

    public void iterate() {
        wrv.setSites(stippleSites);
        stippleCells = wrv.collectCells();
        stippleSites.clear();
        stipples.clear();

        this.status.hysteresis = computeCurrentHysteresis();
        for (Cell cell : stippleCells) {

            float sd = getStippleDiameter(cell);
            int c = 0;
            if (cell.reverse == 1) {
                c = 255;
            }
            stipples.add(new Stipple(cell.centroid, pa.color(c), sd));

            float[] splitThresholds = getSplitThresholds(cell, sd, this.status.hysteresis);

            if (cell.moments[0] < splitThresholds[0] || cell.area == 0) {
                // Merge cell (dont do anything)
                this.status.merges++;
                continue;
            }

            if (cell.moments[0] > splitThresholds[1] && cell.cv > 0.5) {
                // Split cell according to cell size and orientation
                float splitAmount = (float) (0.5f * sqrt(max(1.0f, cell.area) / PI));
                float angle = cell.orientation;
                float splitX = (float) (splitAmount * cos(angle));
                float splitY = (float) (splitAmount * sin(angle));

                Point splitSeed1 = new Point(cell.centroid.x - splitX, cell.centroid.y - splitY);
                Point splitSeed2 = new Point(cell.centroid.x + splitX, cell.centroid.y + splitY);
                int col = pa.color(Tools.random(255), Tools.random(255)/2, Tools.random(255)/4);
                stippleSites.add(Tools.addJitter(splitSeed1, 0.001f));
                //stipples.add(new Stipple(addJitter(splitSeed1, 0.001), col, sd));
                stippleSites.add(Tools.addJitter(splitSeed2, 0.001f));
                //stipples.add(new Stipple(addJitter(splitSeed2, 0.001), col, sd));


                // check boundaries
                //splitSeed1.setX(std::max(0.0f, std::min(splitSeed1.x(), 1.0f)));
                //splitSeed1.setY(std::max(0.0f, std::min(splitSeed1.y(), 1.0f)));

                //splitSeed2.setX(std::max(0.0f, std::min(splitSeed2.x(), 1.0f)));
                //splitSeed2.setY(std::max(0.0f, std::min(splitSeed2.y(), 1.0f)));

                this.status.splits++;
                continue;
            }

            stippleSites.add(cell.centroid);
        }
        this.status.iterations++;
        this.status.size = stipples.size();
        System.out.println(wrv.cells.size() + ", " + stipples.size());
        System.out.println("Iteration complete");
    }

    public float computeCurrentHysteresis() {
        return options.initialHysteresis + status.iterations * options.hysteresisDelta;
    }

    public boolean isFinished() {
        return ((status.splits == 0 && status.merges == 0) || (status.iterations == options.maxIterations)) && (status.iterations > 1);
    }

    private float getStippleDiameter(Cell cell) {
        if (options.adaptiveStippleSize) {
            // First element in moments array is the sum density that also takes reversed status into account
            // If the cell is reversed, lighter areas are considered more dense
            float cellDensity = cell.moments[0];
            float avgIntensitySqrt = (float) sqrt(cellDensity / cell.area);
            return options.stippleSizeMin * (1.0f - avgIntensitySqrt) + options.stippleSizeMax * avgIntensitySqrt;
        } else {
            return options.initialStippleDiameter;
        }
    }

    private float[] getSplitThresholds(Cell cell, float stippleDiameter, float hysteresis) {
        float stippleArea = (float) (PI * pow(stippleDiameter / 2.0f, 2));
        float lowerValue = (float) ((1.0f - hysteresis / 2.0f) * stippleArea * pow(options.superSamplingFactor, 2));

        float upperValue = (float) ((1.0f + hysteresis / 2.0f) * stippleArea * pow(options.superSamplingFactor, 2));
        //upperValue = upperValue + upperValue * cell.stdev;
        return new float[]{lowerValue, upperValue};
    }

    // TODO fix this method
    public void connectReverseCells() {
        int k = 5;
        for (Cell cell : stippleCells) {
            if(cell.reverse == 1)
                continue;

            List<Cell> neighbours = this.wrv.getKNearestNeighbours(cell, k);
            neighbours.sort((c1, c2) -> {
                int area1 = (int) c1.area;
                int area2 = (int) c2.area;
                return area1 - area2;
            });

            float medianArea = neighbours.get((neighbours.size() / 2) + 1).area;

            ArrayList<Cell> reverseNeighbours = new ArrayList<>(k);
            for (Cell n : neighbours) {
               if (n.reverse == 1)
                    reverseNeighbours.add(n);
            }

            if (reverseNeighbours.size() > 2 && cell.area <= medianArea) {
                for (int[] combination: Tools.generateCombinations(reverseNeighbours.size(), 2)) {
                    Cell c2 = reverseNeighbours.get(combination[0]);
                    Cell c3 = reverseNeighbours.get(combination[1]);
                    float l = linearity(cell.site, c2.site, c3.site);
                    if (l > PI / 2) {
                        flipCell(cell);
                        Stipple s = stipples.get(cell.index);
                        s.c = Color.WHITE;
                        break;
                    }
                }
            }
        }
        System.out.println("Cells connected");
    }

    public static float linearity(Point p1, Point p2, Point p3) {
        double angle1 = atan2(p1.y - p2.y,
                p1.x - p2.x);
        double angle2 = atan2(p1.y - p3.y,
                p1.x - p3.x);
        return (float) abs(angle1 - angle2);
    }

    private void flipCell(Cell cell) {
        cell.reverse = 1;
        wrv.calculateCellProperties(cell);
    }

}
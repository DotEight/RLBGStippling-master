import processing.core.PApplet;
import processing.core.PImage;

import java.util.*;

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
        this.stippleSites = new ArrayList<Point>(options.initialStipples);
        this.stipples = new ArrayList<Stipple>(options.initialStipples);

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
        status.splits = 0;
        status.merges = 0;

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

            if (cell.moments[0] > splitThresholds[1]) {
                // Split cell according to cell size and orientation
                float splitAmount = (float) (0.5f * Math.sqrt(Math.max(1.0f, cell.area) / Math.PI));
                float angle = cell.orientation;
                float splitX = (float) (splitAmount * Math.cos(angle));
                float splitY = (float) (splitAmount * Math.sin(angle));

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

            float avgIntensitySqrt = (float) Math.sqrt(cell.moments[0] / cell.area);

            return options.stippleSizeMin * (1.0f - avgIntensitySqrt) + options.stippleSizeMax * avgIntensitySqrt;
        } else {
            return options.initialStippleDiameter;
        }
    }

    private float[] getSplitThresholds(Cell cell, float stippleDiameter, float hysteresis) {
        float stippleArea = (float) (Math.PI * Math.pow(stippleDiameter / 2.0f, 2));
        float lowerValue = (float) ((1.0f - hysteresis / 2.0f) * stippleArea * Math.pow(options.superSamplingFactor, 2));

        float upperValue = (float) ((1.0f + hysteresis / 2.0f) * stippleArea * Math.pow(options.superSamplingFactor, 2));
        //upperValue = upperValue + upperValue * cell.stdev;
        return new float[]{lowerValue, upperValue};
    }

    public void connectReverseCells() {
        // TODO Check co-linearity of the points to see if the cell is between two black cells, change accordingly
        for (Cell cell : stippleCells) {
            if(cell.reverse == 1)
                continue;
            List<Cell> neigh = this.wrv.getKNearestNeighbours(cell, 4);
            Collections.sort(neigh, new Comparator<Cell>() {
                        public int compare(Cell c1, Cell c2) {
                            int area1 = (int) c1.area;
                            int area2 = (int) c2.area;
                            return area1 - area2;
                        }
                    }
            );

            int count = 0;

            for (Cell n : neigh) {
                //print(n.index + " ");
                if (n.reverse == 1)
                    count ++;
            }

            float medianArea = neigh.get(neigh.size()/2).area;

            if (count > 2 && cell.area <= medianArea) {
                flipCell(cell);
                Stipple s = stipples.get(cell.index);
                s.c = pa.color(255);

            }
        }
    }

    private void flipCell(Cell cell) {
        cell.reverse = 1;
        wrv.calculateCellProperties(cell);
    }
}
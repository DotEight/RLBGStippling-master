import java.util.ArrayList;

// POINT CLASS ###################################################################################################
class Point {
    float x, y;

    Point(float x, float y) {
        this.x = x;
        this.y = y;
    }

    void setX(float x) {
        this.x = x;
    }

    void setY(float y) {
        this.y = y;
    }

    double distanceTo(Point p) {
        return Math.sqrt(Tools.sq(this.x - p.x) + Tools.sq(this.y - p.y));
    }
}

// STIPPLE CLASS #################################################################################################
class Stipple {
    Point location;
    float size;
    int c;

    Stipple(Point location, int c, float size) {
        this.location = location;
        this.c = c;
        this.size = size;
    }
}

// CIRCLE CLASS ####################################################################################################

class Circle {
    Point center;
    float radius;

    Circle (Point p1, Point p2, Point p3) {

        double a13, b13, c13;
        double a23, b23, c23;
        double x, y, rad;

        a13 = 2 * (p1.x - p3.x);
        b13 = 2 * (p1.y - p3.y);
        c13 = (p1.y * p1.y - p3.y * p3.y) + (p1.x * p1.x - p3.x * p3.x);
        a23 = 2 * (p2.x - p3.x);
        b23 = 2 * (p2.y - p3.y);
        c23 = (p2.y * p2.y - p3.y * p3.y) + (p2.x * p2.x - p3.x * p3.x);

        double smallNumber = 0.01;
        if ((Math.abs(a13) < smallNumber && Math.abs(b13) < smallNumber) || (Math.abs(a23) < smallNumber && Math.abs(b23) < smallNumber)) {
            // points too close so set to default circle
            x = 0;
            y = 0;
            rad = 0;
        } else {
            // y calculation
            y = (a13 * c23 - a23 * c13) / (a13 * b23 - a23 * b13);
            // x calculation
            if (Math.abs(a13) > Math.abs(a23)) {
                x = (c13 - b13 * y) / a13;
            } else {
                x = (c23 - b23 * y) / a23;
            }
            // radius calculation
            rad = Math.sqrt((x - p1.x) * (x - p1.x) + (y - p1.y) * (y - p1.y));
        }
        this.center= new Point((float)x, (float)y);
        this.radius = (float)rad;
    }

    boolean includesPoint(Point p) {
        return Tools.sq(p.x - center.x) + Tools.sq(p.y - center.y) < Tools.sq(radius);
    }
}

// CELL CLASS ####################################################################################################
class Cell {
    int index;
    int reverse = 0;

    float area = 0;
    float sumDensity = 0;
    float avgDensity = 0;
    float orientation = 0;
    float eccentricity = 0;
    float cv = 0;

    float[] moments = new float[6];

    Point site = new Point(0, 0);
    Point centroid = new Point(0, 0);
    ArrayList<Point> pixelList = new ArrayList<Point>();

    Cell(int index) {
        this.index = index;
    }

    Cell(int index, Point site) {
        this.index = index;
        this.site = site;
    }

    void calculateProperties(float [][] densityMatrix) {
        avgDensity = sumDensity / area;

        float sigmaDiff = 0;
        moments[0] = Math.abs(reverse * area - sumDensity);

        for (Point p : pixelList) {
            // Cast point to pixel location
            int x = (int) p.x;
            int y = (int) p.y;
            float densityValue = Math.abs(reverse - densityMatrix[x][y]);

            moments[1] += x * densityValue;
            moments[2] += y * densityValue;
            moments[3] += x * y * densityValue;
            moments[4] += x * x * densityValue;
            moments[5] += y * y * densityValue;

            float diff = avgDensity - densityMatrix[x][y];
            sigmaDiff += diff * diff;
        }

        // Calculate higher order properties
        // Standart deviation
        cv =  (float) Math.sqrt(sigmaDiff / area) / avgDensity;

        // Moments are placed in the array from 0 to 5 with the order: m00, m10, m01, m11, m20, m02
        float[] m = moments;

        // Centroid
        centroid.setX(m[1] / m[0]);
        centroid.setY(m[2] / m[0]);

        // Central moments
        float x = m[4] / m[0] - centroid.x * centroid.x;
        float y = m[3] / m[0] - centroid.x * centroid.y; // Stopped multiplying this by 2 because no point
        float z = m[5] / m[0] - centroid.y * centroid.y;

        // Orientation
        orientation = (float) (0.5 * Math.atan(2 * y / (x - z)));

        // Eccentricity
        eccentricity = (float) (((Tools.sq(x - z)) + (4 * Tools.sq(y))) / Tools.sq(x + z));
    }
}


package com.rlbgs;

import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

// POINT CLASS ###################################################################################################
class Point {
    float x, y;

    Point() {
        this.x = -1;
        this.y = -1;
    }

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
        return sqrt(Tools.sq(this.x - p.x) + Tools.sq(this.y - p.y));
    }

    void move(float dx, float dy) {
        this.x += dx;
        this.y += dy;
    }

    double norm() {
        return Math.sqrt((this.x * this.x) + (this.y * this.y));
    }

    static Point add(Point p1, Point p2) {
        float x = p1.x + p2.x;
        float y = p1.y + p2.y;
        return new Point(x, y);
    }
}

// STIPPLE CLASS #################################################################################################
class Stipple {
    int index;
    Point location;
    float size;
    int c;

    Stipple(int index, Point location, int c, float size) {
        this.index = index;
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
        if ((abs(a13) < smallNumber && abs(b13) < smallNumber) || (abs(a23) < smallNumber && abs(b23) < smallNumber)) {
            // points too close so set to default circle
            x = 0;
            y = 0;
            rad = 0;
        } else {
            // y calculation
            y = (a13 * c23 - a23 * c13) / (a13 * b23 - a23 * b13);
            // x calculation
            if (abs(a13) > abs(a23)) {
                x = (c13 - b13 * y) / a13;
            } else {
                x = (c23 - b23 * y) / a23;
            }
            // radius calculation
            rad = sqrt((x - p1.x) * (x - p1.x) + (y - p1.y) * (y - p1.y));
        }
        this.center= new Point((float)x, (float)y);
        this.radius = (float)rad;
    }

    boolean includesPoint(Point p) {
        return Tools.sq(p.x - center.x) + Tools.sq(p.y - center.y) < Tools.sq(radius);
    }
}

class Polygon {
    List<Point> vetices;
    int color;

    public Polygon(List<Point> vetices, int color) {
        this.vetices = vetices;
        this.color = color;
    }

    public List<Point> getVetices() {
        return vetices;
    }

    public void setVetices(List<Point> vetices) {
        this.vetices = vetices;
    }
}

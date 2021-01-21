package com.rlbgs;

import java.util.ArrayList;
import java.util.List;

public final class PolygonSimplifier {

    private PolygonSimplifier() {
    }

    private static double distanceBetweenPoints(double vx, double vy, double wx, double wy) {
        return Tools.sq(vx - wx) + Tools.sq(vy - wy);
    }

    private static double distanceToSegmentSquared(double px, double py, double vx, double vy, double wx, double wy) {
        final double l2 = distanceBetweenPoints(vx, vy, wx, wy);
        if (l2 == 0)
            return distanceBetweenPoints(px, py, vx, vy);
        final double t = ((px - vx) * (wx - vx) + (py - vy) * (wy - vy)) / l2;
        if (t < 0)
            return distanceBetweenPoints(px, py, vx, vy);
        if (t > 1)
            return distanceBetweenPoints(px, py, wx, wy);
        return distanceBetweenPoints(px, py, (vx + t * (wx - vx)), (vy + t * (wy - vy)));
    }

    private static double perpendicularDistance(double px, double py, double vx, double vy, double wx, double wy) {
        return Math.sqrt(distanceToSegmentSquared(px, py, vx, vy, wx, wy));
    }

    private static void compute(List<Point> list, int s, int e, double epsilon, List<Point> resultList) {
        // Find the point with the maximum distance
        double dmax = 0;
        int index = 0;

        final int start = s;
        final int end = e - 1;
        for (int i = start + 1; i < end; i++) {
            // com.rlgbs.Point
            final double px = list.get(i).x;
            final double py = list.get(i).y;
            // Start
            final double vx = list.get(start).x;
            final double vy = list.get(start).y;
            // End
            final double wx = list.get(end).x;
            final double wy = list.get(end).y;
            final double d = perpendicularDistance(px, py, vx, vy, wx, wy);
            if (d > dmax) {
                index = i;
                dmax = d;
            }
        }
        // If max distance is greater than epsilon, recursively simplify
        if (dmax > epsilon) {
            // Recursive call
            compute(list, s, index, epsilon, resultList);
            compute(list, index, e, epsilon, resultList);
        } else {
            if ((end - start) > 0) {
                resultList.add(list.get(start));
                resultList.add(list.get(end));
            } else {
                resultList.add(list.get(start));
            }
        }
    }

    public static List<Point> douglasPeucker(List<Point> list, double epsilon) {
        final List<Point> resultList = new ArrayList<>();
        compute(list, 0, list.size(), epsilon, resultList);
        return resultList;
    }
}

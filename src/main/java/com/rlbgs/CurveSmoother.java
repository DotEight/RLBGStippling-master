package com.rlbgs;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.sqrt;
import static org.opencv.core.CvType.CV_64F;
import static org.opencv.imgproc.Imgproc.getGaussianKernel;

class CurveSmoother {

    private double[] g;
    private double[] dg;
    private double[] d2g;
    private double[] gx;
    private double[] dx;
    private double[] d2x;
    private double gx1;
    private double dgx1;
    private double d2gx1;

    public double[] kappa;
    public double[] smoothX;
    public double[] smoothY;
    public double[] contourX;
    public double[] contourY;

    /* 1st and 2nd derivative of 1D gaussian  */
    void getGaussianDerivs(double sigma, int M) {

        int L = (M - 1) / 2;
        double sigma_sq = sigma * sigma;
        double sigma_quad = sigma_sq * sigma_sq;

        dg = new double[M];
        d2g = new double[M];
        g = new double[M];

        Mat tmpG = getGaussianKernel(M, sigma, CV_64F);

        for (double i = -L; i < L + 1.0; i += 1.0) {
            int idx = (int) (i + L);

            g[idx] = tmpG.get(idx, 0)[0];

            // from http://www.cedar.buffalo.edu/~srihari/CSE555/Normal2.pdf
            dg[idx] = (-i / sigma_sq) * g[idx];
            d2g[idx] = (-sigma_sq + i * i) / sigma_quad * g[idx];
        }
    }

    /* 1st and 2nd derivative of smoothed curve point */
    void getdX(double[] x, int n, double sigma, boolean isOpen) {

        int L = (g.length - 1) / 2;

        gx1 = dgx1 = d2gx1 = 0.0;
        for (int k = -L; k < L + 1; k++) {
            double x_n_k;
            if (n - k < 0) {
                if (isOpen) {
                    //open curve - mirror values on border
                    x_n_k = x[-(n - k)];
                } else {
                    //closed curve - take values from end of curve
                    x_n_k = x[x.length + (n - k)];
                }
            } else if (n - k > x.length - 1) {
                if (isOpen) {
                    //mirror value on border
                    x_n_k = x[n + k];
                } else {
                    x_n_k = x[(n - k) - x.length];
                }
            } else {
                x_n_k = x[n - k];
            }

            gx1 += x_n_k * g[k + L]; //gaussians go [0 -> M-1]
            dgx1 += x_n_k * dg[k + L];
            d2gx1 += x_n_k * d2g[k + L];
        }
    }

    /* 0th, 1st and 2nd derivatives of whole smoothed curve */
    void getdXcurve(double[] x, double sigma, boolean isOpen) {

        gx = new double[x.length];
        dx = new double[x.length];
        d2x = new double[x.length];

        for (int i = 0; i < x.length; i++) {
            getdX(x, i, sigma, isOpen);
            gx[i] = gx1;
            dx[i] = dgx1;
            d2x[i] = d2gx1;
        }
    }

    /*
        compute curvature of curve after gaussian smoothing
        from "Shape similarity retrieval under affine transforms", Mokhtarian & Abbasi 2002
        curvex - x position of points
        curvey - y position of points
        kappa - curvature coeff for each point
        sigma - gaussian sigma
    */
    void computeCurveCSS(double[] curvex, double[] curvey, double sigma, boolean isOpen) {
        int M = (int) Math.round((10.0 * sigma + 1.0) / 2.0) * 2 - 1;
        assert (M % 2 == 1); //M is an odd number

        getGaussianDerivs(sigma, M);//, g, dg, d2g

        double[] X, XX, Y, YY;

        getdXcurve(curvex, sigma, isOpen);
        smoothX = gx.clone();
        X = dx.clone();
        XX = d2x.clone();

        getdXcurve(curvey, sigma, isOpen);
        smoothY = gx.clone();
        Y = dx.clone();
        YY = d2x.clone();


        kappa = new double[curvex.length];

        for (int i = 0; i < curvex.length; i++) {
            // Mokhtarian 02' eqn (4)
            kappa[i] = (X[i] * YY[i] - XX[i] * Y[i]) / Math.pow(X[i] * X[i] + Y[i] * Y[i], 1.5);
        }
    }

    /* find zero crossings on curvature */
    ArrayList<Integer> findCSSInterestPoints() {

        assert (kappa != null);

        ArrayList<Integer> crossings = new ArrayList<>();

        for (int i = 0; i < kappa.length - 1; i++) {
            if ((kappa[i] < 0.0 && kappa[i + 1] > 0.0) || kappa[i] > 0.0 && kappa[i + 1] < 0.0) {
                crossings.add(i);
            }
        }
        return crossings;
    }

    public void polyLineSplit(MatOfPoint pl) {
        contourX = new double[pl.height()];
        contourY = new double[pl.height()];

        for (int j = 0; j < contourX.length; j++) {
            contourX[j] = pl.get(j, 0)[0];
            contourY[j] = pl.get(j, 0)[1];
        }
    }

    public MatOfPoint polyLineMerge(double[] xContour, double[] yContour) {

        assert (xContour.length == yContour.length);

        MatOfPoint pl = new MatOfPoint();

        List<Point> list = new ArrayList<>();

        for (int j = 0; j < xContour.length; j++)
            list.add(new Point(xContour[j], yContour[j]));

        pl.fromList(list);

        return pl;
    }

    public MatOfPoint smoothCurve(MatOfPoint curve, double sigma, boolean isOpen) {
        int M = (int) Math.round((10.0 * sigma + 1.0) / 2.0) * 2 - 1;
        assert (M % 2 == 1); //M is an odd number

        //create kernels
        getGaussianDerivs(sigma, M);
        polyLineSplit(curve);

        getdXcurve(contourX, sigma, isOpen);
        smoothX = gx.clone();

        getdXcurve(contourY, sigma, isOpen);
        smoothY = gx;

        return polyLineMerge(smoothX, smoothY);
    }

    MatOfPoint resampleCurve(MatOfPoint curve, int N, boolean isOpen) {

        List<Point> pl = curve.toList();
        List<Point> resamplepl = new ArrayList<>(N);
        resamplepl.add(pl.get(0));

        double pl_length = Imgproc.arcLength(new MatOfPoint2f(curve.toArray()), isOpen);
        double resample_size = pl_length / (double) N;

        int curr = 0;
        double dist = 0.0;
        for (int i = 1; i < N; ) {
            if (curr == pl.size() - 1)
                continue;
            Point cp = new Point(pl.get(curr).x, pl.get(curr).y);
            Point cp1 = new Point(pl.get(curr + 1).x, pl.get(curr + 1).y);
            Point dirv = new Point(cp1.x - cp.x, cp1.y - cp.y);

            double last_dist = sqrt(dirv.x * dirv.x + dirv.y * dirv.y);
            dist += last_dist;

            if (dist >= resample_size) {
                //put a point on line
                double _d = last_dist - (dist - resample_size);
                dirv.x = dirv.x * (1.0 / last_dist) * _d;
                dirv.y = dirv.y * (1.0 / last_dist) * _d;

                Point np = new Point(cp.x + dirv.x, cp.y + dirv.y);
                resamplepl.add(np);
                i++;

                dist = last_dist - _d; //remaining dist

                //if remaining dist to next point needs more sampling... (within some epsilon)
                while (dist - resample_size > 1e-3) {
//              cout << "point " << i << " between " << curr << " and " << curr+1 << " remaining " << dist << endl;
                    assert (i < resamplepl.size());
                    dirv.x = dirv.x * resample_size;
                    dirv.y = dirv.y * resample_size;
                    Point p = new Point(resamplepl.get(i - 1).x, resamplepl.get(i - 1).y);
                    p.x = +dirv.x;
                    p.y = +dirv.y;
                    resamplepl.add(p);
                    dist -= resample_size;
                    i++;
                }
            }

            curr++;
        }
        MatOfPoint newCurve = new MatOfPoint();
        newCurve.fromList(resamplepl);
        return newCurve;
    }

    double distance(Point p1, Point p2) {
        return sqrt(Tools.sq(p1.x - p2.x) + Tools.sq(p1.y - p2.y));
    }

}

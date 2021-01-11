package com.rlgbs;

public class Options {
    int initialStipples = 1;
    float initialStippleDiameter = 3.0f;

    boolean adaptiveStippleSize = false;
    float stippleSizeMin = 2.0f;
    float stippleSizeMax = 4.0f;

    int superSamplingFactor = 2;
    int maxIterations = 50;

    float initialHysteresis = 0.6f;
    float hysteresisDelta = 0.01f;

    Options(int initialStipples, int maxIterations, int superSamplingFactor, boolean adaptiveStippleSize) {
        this.initialStipples = initialStipples;
        this.maxIterations = maxIterations;
        this.superSamplingFactor = superSamplingFactor;
        this.adaptiveStippleSize = adaptiveStippleSize;
    }

    void setStippleSize(float initialStippleDiameter, float stippleSizeMin, float stippleSizeMax) {
        this.initialStippleDiameter = initialStippleDiameter;
        this.stippleSizeMin = stippleSizeMin;
        this.stippleSizeMax = stippleSizeMax;
    }

    void setHysteresis(float initialHysteresis, float hysteresisDelta) {
        this.initialHysteresis = initialHysteresis;
        this.hysteresisDelta = hysteresisDelta;
    }
}

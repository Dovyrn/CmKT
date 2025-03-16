package com.logicalgeekboy.logical_zoom.java_util.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtil {
    private static boolean DEBUG = true;

    private static void debug(String message) {
        if (DEBUG) {
            System.out.println("[MathUtil Debug] " + message);
        }
    }

    public static float findMiddleValue(float f, float f2, float f3) {
        float result = f < f2 ? f2 : Math.min(f, f3);
        if (DEBUG) {
            debug("findMiddleValue(" + f + ", " + f2 + ", " + f3 + ") = " + result);
        }
        return result;
    }

    public static float interpolate(float current, float target, float factor) {
        if (DEBUG) {
            debug("interpolate: current=" + current + ", target=" + target + ", factor=" + factor);
        }

        // Ensure factor is in valid range
        factor = findMiddleValue(factor, 0, 1);
        if (DEBUG && factor <= 0) {
            debug("WARNING: interpolation factor is <= 0! This will make no change to current value.");
        }

        float result = current + (target - current) * factor;
        debug("interpolation result: " + result + " (moved " + (factor * 100) + "% toward target)");
        return result;
    }

    public static float scaleAndRoundFloat(float n, int newScale) {
        if (newScale >= 0) {
            return BigDecimal.valueOf(n).setScale(newScale, RoundingMode.FLOOR).floatValue();
        }
        return -1;
    }
}
package com.triono.juken5autotune;

import java.util.Locale;

/**
 * Core AFR Auto Tune calculation.
 *
 * The engine is deliberately separated from Bluetooth/ECU transport:
 * the actual Juken 5 packet/protocol must be confirmed from the original
 * APK before any write command is sent to a real ECU.
 */
public final class AutoTuneEngine {
    private AutoTuneEngine() {}

    public static final double MIN_CORRECTION_PERCENT = -30.0;
    public static final double MAX_CORRECTION_PERCENT = 30.0;
    public static final double MIN_CELL_VALUE = 20.0;
    public static final double MAX_CELL_VALUE = 200.0;

    /** Positive correction means more fuel. */
    public static double correctionPercent(double actualAfr, double targetAfr) {
        if (!(actualAfr > 0.0) || !(targetAfr > 0.0)) {
            throw new IllegalArgumentException("AFR harus lebih besar dari 0");
        }

        double correction = ((actualAfr / targetAfr) - 1.0) * 100.0;
        return clamp(correction, MIN_CORRECTION_PERCENT, MAX_CORRECTION_PERCENT);
    }

    /** Applies AFR correction to a percentage-style Juken fuel cell. */
    public static double applyCorrection(double currentCell, double correctionPercent) {
        double value = currentCell * (1.0 + correctionPercent / 100.0);
        return clamp(value, MIN_CELL_VALUE, MAX_CELL_VALUE);
    }

    /** Optional learning factor for smoother Auto Tune operation. 0..1. */
    public static double learnedCorrection(double correctionPercent, double learningFactor) {
        double factor = clamp(learningFactor, 0.0, 1.0);
        return correctionPercent * factor;
    }

    public static String format(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

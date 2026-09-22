package com.triono.juken5autotune;

import java.util.Locale;

/**
 * Juken 5 protocol layer.
 *
 * The command names and live-frame layout here are based on public
 * reverse-engineering work for Juken 5 and are kept separate from the
 * Bluetooth transport so they can be verified against a real ECU before
 * any write operation is enabled.
 */
public final class EcuProtocol {
    private EcuProtocol() {}

    public static final String SPP_UUID =
            "00001101-0000-1000-8000-00805F9B34FB";

    public static final String LIVE_START = "160A\r\n";
    public static final String LIVE_STOP = "160B\r\n";
    public static final String IDENTITY = "1617\r\n";
    public static final String SETTINGS = "1607\r\n";
    public static final String TPS_MONITOR = "4601\r\n";

    public static final String LIVE_SYNC = "A603";
    public static final String ACK = "1A00";

    // Original Juken-style load breakpoints: 21 columns.
    public static final int[] TPS_BREAKPOINTS = {
            0, 2, 5, 10, 15, 20, 25, 30, 35, 40, 45,
            50, 55, 60, 65, 70, 75, 80, 85, 90, 100
    };

    // 61 RPM rows: 1000..16000 in 250-RPM steps.
    public static final int RPM_FIRST = 1000;
    public static final int RPM_STEP = 250;
    public static final int RPM_ROWS = 61;

    public static int rpmForRow(int row) {
        return RPM_FIRST + (row * RPM_STEP);
    }

    public static int rowForRpm(int rpm) {
        int row = Math.round((rpm - RPM_FIRST) / (float) RPM_STEP);
        return clamp(row, 0, RPM_ROWS - 1);
    }

    public static int colForTps(int tps) {
        int best = 0;
        int diff = Integer.MAX_VALUE;
        for (int i = 0; i < TPS_BREAKPOINTS.length; i++) {
            int d = Math.abs(TPS_BREAKPOINTS[i] - tps);
            if (d < diff) {
                diff = d;
                best = i;
            }
        }
        return best;
    }

    public static LiveData parseLiveLine(String input) {
        if (input == null) return null;

        String line = input.trim();
        int sync = line.indexOf(LIVE_SYNC + ";");
        if (sync < 0) sync = line.toUpperCase(Locale.US).indexOf(LIVE_SYNC + ";");
        if (sync < 0) return null;

        String[] p = line.substring(sync).split(";");
        if (p.length < 13) return null;

        try {
            int tpsRaw = Integer.parseInt(p[1].trim());
            float battery = Float.parseFloat(p[2].trim());
            int rpm = Integer.parseInt(p[4].trim());
            float eot = Float.parseFloat(p[5].trim()) / 10f;
            float fuel = Float.parseFloat(p[6].trim());
            float afr = Float.parseFloat(p[7].trim());
            float base = Float.parseFloat(p[8].trim());
            float injectorTiming = Float.parseFloat(p[9].trim());
            float ignition = Float.parseFloat(p[10].trim()) / 10f;
            float mapRaw = Float.parseFloat(p[11].trim());
            float iat = Float.parseFloat(p[12].trim()) / 10f;

            int tps;
            if (tpsRaw <= 0) tps = 0;
            else if (tpsRaw == 1) tps = 5;
            else if (tpsRaw < 20) tps = (tpsRaw * 5) - 5;
            else tps = 100;

            return new LiveData(rpm, tps, battery, eot, iat, afr,
                    base, injectorTiming, ignition, fuel, mapRaw, line);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String readMapCommand(String mapOpcode, int row) {
        return mapOpcode + ";2;" + clamp(row, 0, RPM_ROWS - 1) + "\r\n";
    }

    /**
     * Candidate-only builder. It is intentionally not called by MainActivity
     * because the real Juken 5 write opcode/packet is not verified.
     */
    public static String writeMapCommand(String writeOpcode, int row, float[] values,
                                         boolean decimal) {
        StringBuilder b = new StringBuilder();
        b.append(writeOpcode).append(";2;").append(clamp(row, 0, RPM_ROWS - 1));
        for (float v : values) {
            if (decimal) b.append(';').append(String.format(Locale.US, "%.2f", v));
            else b.append(';').append(Math.round(v));
        }
        return b.append("\r\n").toString();
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static final class LiveData {
        public final int rpm;
        public final int tps;
        public final float battery;
        public final float exhaustTemp;
        public final float intakeTemp;
        public final float afr;
        public final float baseMap;
        public final float injectorTiming;
        public final float ignitionTiming;
        public final float fuelCorrection;
        public final float mapRaw;
        public final String raw;

        LiveData(int rpm, int tps, float battery, float exhaustTemp,
                 float intakeTemp, float afr, float baseMap,
                 float injectorTiming, float ignitionTiming,
                 float fuelCorrection, float mapRaw, String raw) {
            this.rpm = rpm;
            this.tps = tps;
            this.battery = battery;
            this.exhaustTemp = exhaustTemp;
            this.intakeTemp = intakeTemp;
            this.afr = afr;
            this.baseMap = baseMap;
            this.injectorTiming = injectorTiming;
            this.ignitionTiming = ignitionTiming;
            this.fuelCorrection = fuelCorrection;
            this.mapRaw = mapRaw;
            this.raw = raw;
        }
    }
    /** Builds a read request for a map row. Kept isolated until the opcode is verified. */
    public static String buildReadMapRow(String readOpcode, int row) {
        return readMapCommand(readOpcode, row);
    }

    /** Returns a normalized 21-cell row for comparison/display. */
    public static float[] normalizeMapRow(float[] values) {
        float[] out = new float[TPS_BREAKPOINTS.length];
        if (values == null) return out;
        for (int i = 0; i < out.length && i < values.length; i++) out[i] = values[i];
        return out;
    }

    /** Absolute cell-by-cell difference between ECU and app maps. */
    public static float[] compareMapRow(float[] ecuValues, float[] appValues) {
        float[] ecu = normalizeMapRow(ecuValues);
        float[] app = normalizeMapRow(appValues);
        float[] diff = new float[TPS_BREAKPOINTS.length];
        for (int i = 0; i < diff.length; i++) diff[i] = ecu[i] - app[i];
        return diff;
    }

}

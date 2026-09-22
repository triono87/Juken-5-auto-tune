package com.triono.juken5autotune;

/**
 * Conservative 21-cell map-row parser.
 * It accepts a row only when a candidate marker is supplied and exactly 21 numeric cells follow.
 * No Juken 5 map opcode is hard-coded here.
 */
public final class MapRowCandidateParser {
    private MapRowCandidateParser() {}

    public static float[] parse(String line, String expectedMarker) {
        if (line == null || expectedMarker == null) return null;
        String[] p = line.trim().split(";");
        if (p.length != 22) return null;
        if (!expectedMarker.equalsIgnoreCase(p[0].trim())) return null;

        float[] out = new float[21];
        for (int i = 0; i < 21; i++) {
            try {
                out[i] = Float.parseFloat(p[i + 1].trim());
            } catch (Exception e) {
                return null;
            }
        }
        return out;
    }

    public static boolean is21CellCandidate(String line) {
        if (line == null) return false;
        String[] p = line.trim().split(";");
        if (p.length != 22) return false;
        for (int i = 1; i < p.length; i++) {
            try { Float.parseFloat(p[i].trim()); }
            catch (Exception e) { return false; }
        }
        return true;
    }
}

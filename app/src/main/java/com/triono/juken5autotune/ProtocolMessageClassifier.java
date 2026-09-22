package com.triono.juken5autotune;

import java.util.Locale;

/**
 * Separates transport payloads into verified LIVE frames and unverified MAP/WRITE candidates.
 * Only A603; is treated as a LIVE message because that marker is already used by the live parser.
 * Everything else remains UNKNOWN until packet semantics are verified.
 */
public final class ProtocolMessageClassifier {
    public enum Type { LIVE, MAP_CANDIDATE, FUEL_CORRECTION_CANDIDATE, UNKNOWN }

    public static final class Message {
        public final Type type;
        public final String raw;
        public final int fieldCount;
        public final int byteLength;

        Message(Type type, String raw, int fieldCount) {
            this.type = type;
            this.raw = raw;
            this.fieldCount = fieldCount;
            this.byteLength = raw == null ? 0 : raw.length();
        }
    }

    private ProtocolMessageClassifier() {}

    public static Message classify(String line) {
        if (line == null) return null;
        String s = line.trim();
        if (s.isEmpty()) return null;
        String u = s.toUpperCase(Locale.US);

        if (u.startsWith(EcuProtocol.LIVE_SYNC + ";") || u.equals(EcuProtocol.LIVE_SYNC)) {
            return new Message(Type.LIVE, s, countFields(s));
        }

        // These are deliberately only candidates. No opcode is assigned here.
        String[] p = s.split(";");
        if (p.length == 22 && allNumericFrom(p, 1)) {
            return new Message(Type.MAP_CANDIDATE, s, p.length);
        }

        // A repeated short command/response is useful for Fuel Correction investigation,
        // but cannot be called the real Fuel Correction packet without capture evidence.
        if (p.length >= 2 && p.length <= 8 && containsNumericField(p)) {
            return new Message(Type.FUEL_CORRECTION_CANDIDATE, s, p.length);
        }

        return new Message(Type.UNKNOWN, s, p.length);
    }

    private static int countFields(String s) {
        return s.split(";").length;
    }

    private static boolean allNumericFrom(String[] p, int start) {
        for (int i = start; i < p.length; i++) {
            try { Float.parseFloat(p[i].trim()); }
            catch (Exception e) { return false; }
        }
        return true;
    }

    private static boolean containsNumericField(String[] p) {
        for (int i = 1; i < p.length; i++) {
            try { Float.parseFloat(p[i].trim()); return true; }
            catch (Exception ignored) {}
        }
        return false;
    }
}

package com.triono.juken5autotune;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Offline investigator for finding repeated command/response patterns around Fuel Correction.
 * It never sends a packet and never labels a packet as verified.
 */
public final class FuelCorrectionCaptureAnalyzer {
    public static final class Candidate {
        public final String before;
        public final String after;
        public final int sharedPrefixBytes;
        public final int lengthDelta;

        Candidate(String before, String after, int sharedPrefixBytes, int lengthDelta) {
            this.before = before;
            this.after = after;
            this.sharedPrefixBytes = sharedPrefixBytes;
            this.lengthDelta = lengthDelta;
        }
    }

    private FuelCorrectionCaptureAnalyzer() {}

    public static String analyze(List<ProtocolAnalyzer.Frame> frames) {
        if (frames == null || frames.size() < 2) {
            return "Fuel Correction capture: belum cukup frame.\n"
                    + "Lakukan capture dengan aksi koreksi ON/OFF dan ulangi beberapa kali.";
        }

        List<Candidate> candidates = new ArrayList<>();
        for (int i = 1; i < frames.size(); i++) {
            byte[] a = frames.get(i - 1).data;
            byte[] b = frames.get(i).data;
            int shared = sharedPrefix(a, b);
            int delta = b.length - a.length;
            if (shared >= 2 || Math.abs(delta) >= 2) {
                candidates.add(new Candidate(hex(a), hex(b), shared, delta));
            }
        }

        StringBuilder out = new StringBuilder();
        out.append("FUEL CORRECTION PACKET INVESTIGATION\n");
        out.append("Frames: ").append(frames.size()).append("\n");
        out.append("Candidates: ").append(candidates.size()).append("\n\n");
        out.append("CATATAN: semua hasil di bawah adalah kandidat pola, bukan packet Fuel Correction terverifikasi.\n\n");

        int limit = Math.min(30, candidates.size());
        for (int i = 0; i < limit; i++) {
            Candidate c = candidates.get(i);
            out.append(i + 1).append(". shared-prefix=")
               .append(c.sharedPrefixBytes)
               .append(" length-delta=").append(c.lengthDelta).append("\n")
               .append("A: ").append(c.before).append("\n")
               .append("B: ").append(c.after).append("\n\n");
        }
        return out.toString();
    }

    private static int sharedPrefix(byte[] a, byte[] b) {
        if (a == null || b == null) return 0;
        int n = Math.min(a.length, b.length);
        int i = 0;
        while (i < n && a[i] == b[i]) i++;
        return i;
    }

    private static String hex(byte[] data) {
        if (data == null) return "";
        StringBuilder b = new StringBuilder(data.length * 3);
        for (byte v : data) b.append(String.format(Locale.US, "%02X ", v & 0xFF));
        return b.toString().trim();
    }
}

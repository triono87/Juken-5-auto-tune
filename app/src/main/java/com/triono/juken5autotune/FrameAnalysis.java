package com.triono.juken5autotune;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Offline analysis helpers for captured ECU frames. No Juken-5 packet format is assumed. */
public final class FrameAnalysis {
    private FrameAnalysis() {}

    public static String hex(byte[] data) {
        if (data == null) return "";
        StringBuilder s = new StringBuilder();
        for (byte b : data) s.append(String.format(Locale.US, "%02X ", b & 255));
        return s.toString().trim();
    }

    public static String analyze(List<ProtocolAnalyzer.Frame> frames) {
        if (frames == null || frames.isEmpty()) return "Belum ada frame.";
        StringBuilder out = new StringBuilder();
        out.append("FRAME ANALYSIS\n");
        out.append("Total frame: ").append(frames.size()).append("\n");

        int min = Integer.MAX_VALUE, max = 0;
        for (ProtocolAnalyzer.Frame f : frames) {
            min = Math.min(min, f.data.length);
            max = Math.max(max, f.data.length);
        }
        out.append("Panjang frame: ").append(min).append("..").append(max).append(" byte\n");

        // Count common first bytes (possible headers; only candidates, not conclusions).
        int[] first = new int[256];
        for (ProtocolAnalyzer.Frame f : frames)
            if (f.data.length > 0) first[f.data[0] & 255]++;
        out.append("\nBYTE-0 CANDIDATES\n");
        for (int i = 0; i < 256; i++)
            if (first[i] > 0)
                out.append(String.format(Locale.US, "%02X = %d (%.1f%%)\n",
                        i, first[i], 100.0 * first[i] / frames.size()));

        // Test common one-byte checksum candidates against the last byte.
        String[] names = {"SUM8", "XOR8", "SUM8_PLUS_LEN"};
        int[] hits = new int[3];
        int tested = 0;
        for (ProtocolAnalyzer.Frame f : frames) {
            if (f.data.length < 2) continue;
            tested++;
            int sum = 0, xor = 0;
            for (int i = 0; i < f.data.length - 1; i++) {
                sum = (sum + (f.data[i] & 255)) & 255;
                xor ^= f.data[i] & 255;
            }
            int last = f.data[f.data.length - 1] & 255;
            if (sum == last) hits[0]++;
            if (xor == last) hits[1]++;
            if (((sum + f.data.length) & 255) == last) hits[2]++;
        }
        out.append("\nCHECKSUM CANDIDATES (last byte)\n");
        if (tested == 0) out.append("Tidak cukup data.\n");
        else for (int i = 0; i < names.length; i++)
            out.append(names[i]).append(": ").append(hits[i]).append("/").append(tested).append("\n");

        out.append("\nCatatan: kandidat di atas bukan bukti format protokol Juken 5.\n");
        out.append("Untuk mengidentifikasi RPM/TPS/AFR, capture harus dilakukan pada kondisi mesin/ECU yang berbeda.");
        return out.toString();
    }

    public static List<ProtocolAnalyzer.Frame> snapshot(ProtocolAnalyzer analyzer) {
        return analyzer == null ? new ArrayList<ProtocolAnalyzer.Frame>() : analyzer.snapshot();
    }
}

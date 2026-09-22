package com.triono.juken5autotune;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Passive protocol analyzer. It records raw ECU frames without inventing a protocol. */
public final class ProtocolAnalyzer {
    public static final class Frame {
        public final long timestamp;
        public final byte[] data;
        Frame(long timestamp, byte[] data) {
            this.timestamp = timestamp;
            this.data = data;
        }
    }

    private final List<Frame> frames = new ArrayList<>();
    private boolean recording;

    public synchronized void start() {
        frames.clear();
        recording = true;
    }

    public synchronized void stop() {
        recording = false;
    }

    public synchronized boolean isRecording() {
        return recording;
    }

    public synchronized void add(byte[] data, int length) {
        if (!recording || data == null || length <= 0) return;
        frames.add(new Frame(System.currentTimeMillis(),
                Arrays.copyOf(data, Math.min(length, data.length))));
        if (frames.size() > 2000) frames.remove(0);
    }

    public synchronized int frameCount() {
        return frames.size();
    }

    public synchronized String summary() {
        if (frames.isEmpty()) return "Frames: 0";
        int min = Integer.MAX_VALUE, max = 0, total = 0;
        for (Frame f : frames) {
            int n = f.data.length;
            min = Math.min(min, n);
            max = Math.max(max, n);
            total += n;
        }
        double avg = total / (double) frames.size();
        return String.format(java.util.Locale.US,
                "Frames: %d | Len min/max: %d/%d | Avg: %.1f",
                frames.size(), min, max, avg);
    }

    public synchronized String exportText() {
        StringBuilder out = new StringBuilder();
        for (Frame f : frames) {
            out.append(f.timestamp).append(" | ");
            for (byte b : f.data) {
                out.append(String.format(java.util.Locale.US, "%02X ", b & 0xFF));
            }
            out.append('\n');
        }
        return out.toString();
    }

    public synchronized void clear() {
        frames.clear();
    }
}

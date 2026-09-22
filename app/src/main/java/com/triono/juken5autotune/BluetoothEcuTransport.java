package com.triono.juken5autotune;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bluetooth serial transport for an ECU modem.
 *
 * This class deliberately does NOT invent a Juken-5 packet format.
 * It exposes received bytes to the caller so the real protocol can be
 * wired in once the original APK's communication code is available.
 */
public final class BluetoothEcuTransport {
    public interface Listener {
        void onConnected(BluetoothDevice device);
        void onBytes(byte[] data, int length);
        void onDisconnected();
        void onError(String message);
    }

    // Common SPP UUID used by classic Bluetooth serial modems.
    private static final UUID SPP_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final Activity activity;
    private final Listener listener;
    private final BluetoothAdapter adapter;

    private BluetoothSocket socket;
    private InputStream input;
    private OutputStream output;
    private volatile boolean running;

    public BluetoothEcuTransport(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        this.adapter = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean isAvailable() {
        return adapter != null;
    }

    public List<BluetoothDevice> pairedDevices() {
        List<BluetoothDevice> result = new ArrayList<>();
        if (adapter == null || !hasConnectPermission()) return result;
        result.addAll(adapter.getBondedDevices());
        return result;
    }

    public void connect(BluetoothDevice device) {
        if (device == null) {
            listener.onError("Bluetooth device tidak dipilih");
            return;
        }
        if (!hasConnectPermission()) {
            listener.onError("Izin Bluetooth CONNECT belum diberikan");
            return;
        }

        disconnect();

        new Thread(() -> {
            try {
                BluetoothSocket s = device.createRfcommSocketToServiceRecord(SPP_UUID);
                s.connect();

                socket = s;
                input = s.getInputStream();
                output = s.getOutputStream();
                running = true;

                activity.runOnUiThread(() -> listener.onConnected(device));
                readLoop();
            } catch (Exception e) {
                disconnectInternal();
                activity.runOnUiThread(() ->
                        listener.onError("Gagal konek Bluetooth: " + e.getMessage()));
            }
        }, "juken5-bt-connect").start();
    }

    private void readLoop() {
        byte[] buffer = new byte[1024];

        try {
            while (running && input != null) {
                int n = input.read(buffer);
                if (n > 0) {
                    byte[] packet = new byte[n];
                    System.arraycopy(buffer, 0, packet, 0, n);
                    activity.runOnUiThread(() -> listener.onBytes(packet, packet.length));
                }
            }
        } catch (IOException e) {
            if (running) {
                activity.runOnUiThread(() ->
                        listener.onError("Koneksi Bluetooth terputus: " + e.getMessage()));
            }
        } finally {
            disconnectInternal();
            activity.runOnUiThread(listener::onDisconnected);
        }
    }

    /**
     * Raw write only. The Auto Tune layer must call this only after the
     * verified Juken-5 command/packet has been recovered from the original APK.
     */
    public synchronized boolean writeRaw(byte[] data) {
        if (output == null || data == null) return false;
        try {
            output.write(data);
            output.flush();
            return true;
        } catch (IOException e) {
            listener.onError("Gagal mengirim data: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        running = false;
        disconnectInternal();
    }

    private synchronized void disconnectInternal() {
        try { if (input != null) input.close(); } catch (Exception ignored) {}
        try { if (output != null) output.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        input = null;
        output = null;
        socket = null;
    }

    private boolean hasConnectPermission() {
        if (Build.VERSION.SDK_INT < 31) return true;
        return activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }
}

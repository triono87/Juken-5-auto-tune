package com.triono.juken5autotune;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;

import java.util.Locale;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private TextView status;
    private TextView correctionText;
    private EditText afrInput;
    private EditText targetInput;
    private EditText rpmInput;
    private EditText tpsInput;

    private EditText selectedCell;
    private int selectedRow = -1;
    private int selectedCol = -1;

    private static final int RPM_ROWS = 61;
    private static final int TPS_COLS = 21;

    private final String[] rpmAxis = new String[RPM_ROWS];
    private final String[] tpsAxis = new String[TPS_COLS];
    private final EditText[][] fuelCells = new EditText[RPM_ROWS][TPS_COLS];

    private double correction = 0.0;
    private boolean autoTuneRunning = false;
    private BluetoothEcuTransport ecuTransport;
    private Spinner btSpinner;
    private TextView btStatus;
    private TextView rawData;
    private TextView liveTelemetry;
    private final StringBuilder ecuTextBuffer = new StringBuilder();
    private TextView analyzerStatus;
    private Button recordButton;
    private final ProtocolAnalyzer protocolAnalyzer = new ProtocolAnalyzer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildAxes();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(12, 12, 12, 12);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("Juken 5 Auto Tune");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.DKGRAY);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Fuel Map 61 RPM × 21 TPS | RPM 0–16000 | TPS 0–100%");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.GRAY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 4, 0, 10);
        root.addView(subtitle);

        status = new TextView(this);
        status.setText("STATUS: READY");
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        status.setPadding(4, 8, 4, 8);
        root.addView(status);

        LinearLayout btPanel = new LinearLayout(this);
        btPanel.setOrientation(LinearLayout.VERTICAL);

        btStatus = new TextView(this);
        btStatus.setText("ECU BLUETOOTH: DISCONNECTED");
        btStatus.setTextSize(15);
        btStatus.setGravity(Gravity.CENTER);
        btPanel.addView(btStatus);

        LinearLayout btButtons = new LinearLayout(this);
        btSpinner = new Spinner(this);
        btButtons.addView(btSpinner, new LinearLayout.LayoutParams(0, 52, 1));

        Button refreshBt = new Button(this);
        refreshBt.setText("REFRESH");
        refreshBt.setOnClickListener(v -> loadPairedBluetooth());
        btButtons.addView(refreshBt, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 52));

        Button connectBt = new Button(this);
        connectBt.setText("CONNECT ECU");
        connectBt.setOnClickListener(v -> connectSelectedBluetooth());
        btButtons.addView(connectBt, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 52));
        btPanel.addView(btButtons);

        rawData = new TextView(this);
        rawData.setText("RAW ECU DATA: -");
        rawData.setTextSize(11);
        rawData.setMaxLines(3);
        btPanel.addView(rawData);

        liveTelemetry = new TextView(this);
        liveTelemetry.setText("LIVE ECU: belum aktif");
        liveTelemetry.setTextSize(14);
        liveTelemetry.setPadding(4, 6, 4, 6);
        btPanel.addView(liveTelemetry);

        LinearLayout protocolButtons = new LinearLayout(this);
        Button liveStart = new Button(this);
        liveStart.setText("LIVE START");
        liveStart.setOnClickListener(v -> sendLiveStart());
        Button liveStop = new Button(this);
        liveStop.setText("LIVE STOP");
        liveStop.setOnClickListener(v -> sendLiveStop());
        protocolButtons.addView(liveStart, new LinearLayout.LayoutParams(0, 52, 1));
        protocolButtons.addView(liveStop, new LinearLayout.LayoutParams(0, 52, 1));
        btPanel.addView(protocolButtons);
        root.addView(btPanel);
        LinearLayout analyzerPanel = new LinearLayout(this);
        analyzerPanel.setOrientation(LinearLayout.VERTICAL);

        TextView analyzerTitle = new TextView(this);
        analyzerTitle.setText("ECU PROTOCOL ANALYZER");
        analyzerTitle.setTextSize(16);
        analyzerTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        analyzerPanel.addView(analyzerTitle);

        analyzerStatus = new TextView(this);
        analyzerStatus.setText("CAPTURE: OFF | Frames: 0");
        analyzerPanel.addView(analyzerStatus);

        LinearLayout analyzerButtons = new LinearLayout(this);
        recordButton = new Button(this);
        recordButton.setText("START CAPTURE");
        recordButton.setOnClickListener(v -> toggleCapture());
        analyzerButtons.addView(recordButton, new LinearLayout.LayoutParams(0, 52, 1));

        Button clearCapture = new Button(this);
        clearCapture.setText("CLEAR");
        clearCapture.setOnClickListener(v -> {
            protocolAnalyzer.clear();
            updateAnalyzerStatus();
        });
        analyzerButtons.addView(clearCapture, new LinearLayout.LayoutParams(0, 52, 1));

        Button exportCapture = new Button(this);
        exportCapture.setText("SHOW CAPTURE");
        exportCapture.setOnClickListener(v -> showCapture());
        analyzerButtons.addView(exportCapture, new LinearLayout.LayoutParams(0, 52, 1));

        Button analyzeCapture = new Button(this);
        analyzeCapture.setText("ANALYZE");
        analyzeCapture.setOnClickListener(v -> showAnalysis());
        analyzerButtons.addView(analyzeCapture, new LinearLayout.LayoutParams(0, 52, 1));

        analyzerPanel.addView(analyzerButtons);
        root.addView(analyzerPanel);



        ecuTransport = new BluetoothEcuTransport(this, new BluetoothEcuTransport.Listener() {
            @Override public void onConnected(BluetoothDevice device) {
                btStatus.setText("ECU BLUETOOTH: CONNECTED - " + device.getName());
                status.setText("STATUS: ECU CONNECTED");
                sendCommand(EcuProtocol.IDENTITY);
                sendCommand(EcuProtocol.SETTINGS);
            }
            @Override public void onBytes(byte[] data, int length) {
                StringBuilder hex = new StringBuilder();
                protocolAnalyzer.add(data, length);
                consumeEcuText(data, length);
                updateAnalyzerStatus();
                for (int i = 0; i < length; i++) hex.append(String.format(Locale.US, "%02X ", data[i] & 0xFF));
                rawData.setText("RAW ECU DATA: " + hex.toString().trim());
            }
            @Override public void onDisconnected() {
                btStatus.setText("ECU BLUETOOTH: DISCONNECTED");
            }
            @Override public void onError(String message) {
                btStatus.setText("ECU BLUETOOTH: ERROR");
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
        loadPairedBluetooth();

        LinearLayout inputPanel = new LinearLayout(this);
        inputPanel.setOrientation(LinearLayout.VERTICAL);

        rpmInput = createInput("RPM", "3000");
        tpsInput = createInput("TPS %", "20");
        afrInput = createInput("Actual AFR", "14.7");
        targetInput = createInput("Target AFR", "13.2");

        inputPanel.addView(rpmInput);
        inputPanel.addView(tpsInput);
        inputPanel.addView(afrInput);
        inputPanel.addView(targetInput);
        root.addView(inputPanel);

        Button calculate = new Button(this);
        calculate.setText("CALCULATE AFR CORRECTION");
        calculate.setOnClickListener(v -> calculateCorrection());
        root.addView(calculate);

        correctionText = new TextView(this);
        correctionText.setText("Fuel Correction: 0.0 %");
        correctionText.setTextSize(21);
        correctionText.setGravity(Gravity.CENTER);
        correctionText.setPadding(4, 8, 4, 8);
        root.addView(correctionText);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);

        Button start = new Button(this);
        start.setText("START AUTO TUNE");
        start.setOnClickListener(v -> {
            autoTuneRunning = true;
            status.setText("STATUS: AUTO TUNE RUNNING");
            status.setTextColor(Color.rgb(0, 120, 0));
        });

        Button stop = new Button(this);
        stop.setText("STOP");
        stop.setOnClickListener(v -> {
            autoTuneRunning = false;
            status.setText("STATUS: STOPPED");
            status.setTextColor(Color.RED);
        });

        controls.addView(start, new LinearLayout.LayoutParams(0, 55, 1));
        controls.addView(stop, new LinearLayout.LayoutParams(0, 55, 1));
        root.addView(controls);

        TextView mapTitle = new TextView(this);
        mapTitle.setText("FUEL MAP 61 × 21");
        mapTitle.setTextSize(20);
        mapTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        mapTitle.setGravity(Gravity.CENTER);
        mapTitle.setPadding(4, 12, 4, 8);
        root.addView(mapTitle);

        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        ScrollView vertical = new ScrollView(this);
        TableLayout table = new TableLayout(this);
        table.setStretchAllColumns(false);

        TableRow header = new TableRow(this);
        header.addView(createHeader("RPM/TPS"));

        for (String tps : tpsAxis) {
            header.addView(createHeader(tps));
        }
        table.addView(header);

        for (int r = 0; r < RPM_ROWS; r++) {
            TableRow row = new TableRow(this);
            row.addView(createHeader(rpmAxis[r]));

            for (int c = 0; c < TPS_COLS; c++) {
                EditText cell = new EditText(this);
                cell.setText("100");
                cell.setTextSize(11);
                cell.setGravity(Gravity.CENTER);
                cell.setSingleLine(true);
                cell.setInputType(InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
                        | InputType.TYPE_NUMBER_FLAG_SIGNED);

                TableRow.LayoutParams p = new TableRow.LayoutParams(78, 54);
                p.setMargins(1, 1, 1, 1);
                cell.setLayoutParams(p);

                final int rr = r;
                final int cc = c;
                cell.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        selectedCell = cell;
                        selectedRow = rr;
                        selectedCol = cc;
                        status.setText("SELECTED: RPM " + rpmAxis[rr]
                                + " / TPS " + tpsAxis[cc] + "%");
                        status.setTextColor(Color.DKGRAY);
                    }
                });

                fuelCells[r][c] = cell;
                row.addView(cell);
            }
            table.addView(row);
        }

        vertical.addView(table);
        horizontal.addView(vertical);

        root.addView(horizontal, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        Button apply = new Button(this);
        apply.setText("APPLY CORRECTION TO SELECTED CELL");
        apply.setOnClickListener(v -> applyCorrection());
        root.addView(apply);

        Button reset = new Button(this);
        reset.setText("RESET MAP TO 100");
        reset.setOnClickListener(v -> resetMap());
        root.addView(reset);

        setContentView(root);
    }

    private void loadPairedBluetooth() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 501);
            return;
        }
        if (!ecuTransport.isAvailable()) {
            btStatus.setText("ECU BLUETOOTH: NOT AVAILABLE");
            return;
        }
        ArrayList<String> names = new ArrayList<>();
        for (BluetoothDevice d : ecuTransport.pairedDevices()) {
            names.add(d.getName() == null ? d.getAddress() : d.getName());
        }
        if (names.isEmpty()) names.add("Tidak ada perangkat paired");
        btSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
    }

    private void connectSelectedBluetooth() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 501);
            return;
        }
        java.util.List<BluetoothDevice> devices = ecuTransport.pairedDevices();
        int pos = btSpinner.getSelectedItemPosition();
        if (pos < 0 || pos >= devices.size()) {
            Toast.makeText(this, "Pair modem Bluetooth ECU terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }
        btStatus.setText("ECU BLUETOOTH: CONNECTING...");
        ecuTransport.connect(devices.get(pos));
    }

    private void toggleCapture() {
        if (protocolAnalyzer.isRecording()) {
            protocolAnalyzer.stop();
            recordButton.setText("START CAPTURE");
        } else {
            protocolAnalyzer.start();
            recordButton.setText("STOP CAPTURE");
        }
        updateAnalyzerStatus();
    }

    private void updateAnalyzerStatus() {
        if (analyzerStatus != null) {
            analyzerStatus.setText((protocolAnalyzer.isRecording() ? "CAPTURE: ON | " : "CAPTURE: OFF | ")
                    + protocolAnalyzer.summary());
        }
    }

    private void showAnalysis() {
        TextView view = new TextView(this);
        view.setText(FrameAnalysis.analyze(FrameAnalysis.snapshot(protocolAnalyzer)));
        view.setTextSize(12);
        view.setPadding(20, 20, 20, 20);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(view);
        new android.app.AlertDialog.Builder(this)
                .setTitle("ECU Frame Analysis")
                .setView(scroll)
                .setPositiveButton("TUTUP", null)
                .show();
    }

    private void showCapture() {
        TextView view = new TextView(this);
        view.setText(protocolAnalyzer.exportText().isEmpty()
                ? "Belum ada frame."
                : protocolAnalyzer.exportText());
        view.setTextSize(11);
        view.setPadding(20, 20, 20, 20);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(view);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Captured ECU Frames")
                .setView(scroll)
                .setPositiveButton("TUTUP", null)
                .show();
    }

    @Override protected void onDestroy() {
        if (ecuTransport != null) ecuTransport.disconnect();
        super.onDestroy();
    }

    private void buildAxes() {
        // Original-style 61 RPM rows: 1000..16000 in 250-RPM steps.
        for (int i = 0; i < RPM_ROWS; i++) {
            int rpm = EcuProtocol.rpmForRow(i);
            rpmAxis[i] = String.valueOf(rpm);
        }

        // Original-style 21 TPS/load breakpoints.
        for (int i = 0; i < TPS_COLS; i++) {
            tpsAxis[i] = String.valueOf(EcuProtocol.TPS_BREAKPOINTS[i]);
        }
    }

    private EditText createInput(String hint, String value) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setText(value);
        edit.setTextSize(16);
        edit.setSingleLine(true);
        edit.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);
        edit.setPadding(10, 2, 10, 2);
        return edit;
    }

    private TextView createHeader(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(10);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(Color.WHITE);
        view.setBackgroundColor(Color.DKGRAY);
        view.setPadding(3, 3, 3, 3);

        TableRow.LayoutParams p = new TableRow.LayoutParams(78, 54);
        p.setMargins(1, 1, 1, 1);
        view.setLayoutParams(p);
        return view;
    }

    private void sendCommand(String command) {
        if (ecuTransport == null) return;
        ecuTransport.writeRaw(command.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    private void sendLiveStart() {
        sendCommand(EcuProtocol.LIVE_START);
        status.setText("STATUS: LIVE STREAM STARTED");
    }

    private void sendLiveStop() {
        sendCommand(EcuProtocol.LIVE_STOP);
        status.setText("STATUS: LIVE STREAM STOPPED");
    }

    private void consumeEcuText(byte[] data, int length) {
        String text = new String(data, 0, length, java.nio.charset.StandardCharsets.US_ASCII);
        synchronized (ecuTextBuffer) {
            ecuTextBuffer.append(text);
            int nl;
            while ((nl = ecuTextBuffer.indexOf("\n")) >= 0) {
                String line = ecuTextBuffer.substring(0, nl).replace("\r", "").trim();
                ecuTextBuffer.delete(0, nl + 1);
                if (!line.isEmpty()) handleProtocolLine(line);
            }
            if (ecuTextBuffer.length() > 8192) ecuTextBuffer.delete(0, ecuTextBuffer.length() - 4096);
        }
    }

    private void handleProtocolLine(String line) {
        EcuProtocol.LiveData live = EcuProtocol.parseLiveLine(line);
        if (live == null) return;
        liveTelemetry.setText(String.format(Locale.US,
                "LIVE ECU  RPM %d | TPS %d%% | AFR %.2f\nBAT %.2fV | EOT %.1f°C | IAT %.1f°C\nBASE %.2f | FUEL CORR %.1f%% | IT %.1f | IG %.1f°",
                live.rpm, live.tps, live.afr, live.battery, live.exhaustTemp,
                live.intakeTemp, live.baseMap, live.fuelCorrection,
                live.injectorTiming, live.ignitionTiming));
        rpmInput.setText(String.valueOf(live.rpm));
        tpsInput.setText(String.valueOf(live.tps));
        afrInput.setText(String.format(Locale.US, "%.2f", live.afr));
        if (autoTuneRunning && live.afr > 0f) {
            try {
                double target = Double.parseDouble(targetInput.getText().toString());
                correction = AutoTuneEngine.correctionPercent(live.afr, target);

                int row = EcuProtocol.rowForRpm(live.rpm);
                int col = EcuProtocol.colForTps(live.tps);
                selectedRow = row;
                selectedCol = col;
                selectedCell = fuelCells[row][col];

                double current = Double.parseDouble(selectedCell.getText().toString());
                double learned = AutoTuneEngine.learnedCorrection(correction, 0.25);
                double next = AutoTuneEngine.applyCorrection(current, learned);
                selectedCell.setText(String.format(Locale.US, "%.2f", next));

                correctionText.setText(String.format(Locale.US,
                        "LIVE CORR %.1f%% | CELL RPM %d / TPS %d%% | MAP %.2f",
                        learned, EcuProtocol.rpmForRow(row), EcuProtocol.TPS_BREAKPOINTS[col], next));
                status.setText("STATUS: AUTO TUNE → ACTIVE CELL UPDATED");
            } catch (Exception ignored) {}
        }
    }
    private int nearestRpmRow(int rpm) {
        return EcuProtocol.rowForRpm(rpm);
    }

    private int nearestTpsCol(int tps) {
        return EcuProtocol.colForTps(tps);
    }

    private void calculateCorrection() {
        try {
            double actual = Double.parseDouble(afrInput.getText().toString());
            double target = Double.parseDouble(targetInput.getText().toString());

            correction = AutoTuneEngine.correctionPercent(actual, target);

            correctionText.setText(String.format(Locale.US,
                    "Fuel Correction: %.1f %%", correction));

            status.setText("STATUS: CORRECTION CALCULATED");
            status.setTextColor(Color.DKGRAY);
        } catch (Exception e) {
            Toast.makeText(this, "Periksa nilai AFR", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyCorrection() {
        if (selectedCell == null) {
            Toast.makeText(this, "Pilih cell fuel map terlebih dahulu",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double current = Double.parseDouble(selectedCell.getText().toString());
            double newValue = AutoTuneEngine.applyCorrection(current, correction);

            selectedCell.setText(AutoTuneEngine.format(newValue));

            status.setText("UPDATED: RPM " + rpmAxis[selectedRow]
                    + " / TPS " + tpsAxis[selectedCol]
                    + "% -> " + AutoTuneEngine.format(newValue));

        } catch (Exception e) {
            Toast.makeText(this, "Nilai cell tidak valid",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void resetMap() {
        for (int r = 0; r < RPM_ROWS; r++) {
            for (int c = 0; c < TPS_COLS; c++) {
                fuelCells[r][c].setText("100");
            }
        }

        selectedCell = null;
        selectedRow = -1;
        selectedCol = -1;
        status.setText("STATUS: MAP RESET");
        status.setTextColor(Color.DKGRAY);
    }
}

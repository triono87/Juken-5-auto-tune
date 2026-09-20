package com.triono.juken5autotune;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.util.Locale;

public class MainActivity extends Activity {

    private TextView status;
    private TextView correctionText;

    private EditText rpmInput;
    private EditText afrInput;
    private EditText targetInput;
    private EditText tpsInput;

    private EditText selectedCell = null;

    private final String[] RPM = {
            "1500","2000","2500","3000",
            "3500","4000","4500","5000",
            "5500","6000","6500","7000",
            "7500","8000","8500","9000"
    };

    private final String[] TPS = {
            "0","5","10","15",
            "20","25","30","40",
            "50","60","70","80",
            "90","100","110","120"
    };

    private EditText[][] fuelCells = new EditText[16][16];

    private double correction = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16,16,16,16);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("Juken 5 Auto Tune");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.DKGRAY);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("16 × 16 Fuel Map");
        subtitle.setTextSize(18);
        subtitle.setTextColor(Color.GRAY);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        status = new TextView(this);
        status.setText("STATUS: READY");
        status.setTextSize(20);
        status.setGravity(Gravity.CENTER);
        status.setPadding(5,15,5,15);
        root.addView(status);

        LinearLayout inputPanel = new LinearLayout(this);
        inputPanel.setOrientation(LinearLayout.VERTICAL);

        rpmInput = createInput("RPM", "3000");
        afrInput = createInput("Actual AFR", "14.7");
        targetInput = createInput("Target AFR", "13.2");
        tpsInput = createInput("TPS %", "20");

        inputPanel.addView(rpmInput);
        inputPanel.addView(afrInput);
        inputPanel.addView(targetInput);
        inputPanel.addView(tpsInput);

        root.addView(inputPanel);

        Button calculate = new Button(this);
        calculate.setText("CALCULATE FUEL CORRECTION");
        calculate.setTextSize(16);

        calculate.setOnClickListener(v -> calculateCorrection());

        root.addView(calculate);

        correctionText = new TextView(this);
        correctionText.setText("Fuel Correction: 0.0 %");
        correctionText.setTextSize(22);
        correctionText.setGravity(Gravity.CENTER);
        correctionText.setPadding(5,10,5,10);

        root.addView(correctionText);

        Button start = new Button(this);
        start.setText("START AUTO TUNE");

        start.setOnClickListener(v -> {
            status.setText("STATUS: AUTO TUNE RUNNING");
            status.setTextColor(Color.rgb(0,120,0));
        });

        root.addView(start);

        Button stop = new Button(this);
        stop.setText("STOP");

        stop.setOnClickListener(v -> {
            status.setText("STATUS: STOPPED");
            status.setTextColor(Color.RED);
        });

        root.addView(stop);

        TextView mapTitle = new TextView(this);
        mapTitle.setText("FUEL MAP 16 × 16");
        mapTitle.setTextSize(22);
        mapTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        mapTitle.setGravity(Gravity.CENTER);
        mapTitle.setPadding(5,20,5,10);

        root.addView(mapTitle);

        /*
         * HORIZONTAL SCROLL
         */
        HorizontalScrollView horizontal = new HorizontalScrollView(this);

        /*
         * VERTICAL SCROLL
         */
        ScrollView vertical = new ScrollView(this);

        TableLayout table = new TableLayout(this);
        table.setStretchAllColumns(false);

        /*
         * HEADER
         */
        TableRow header = new TableRow(this);

        TextView corner = createHeader("RPM/TPS");
        header.addView(corner);

        for (String tps : TPS) {
            TextView h = createHeader(tps);
            header.addView(h);
        }

        table.addView(header);

        /*
         * MAP CELLS
         */
        for (int r = 0; r < 16; r++) {

            TableRow row = new TableRow(this);

            TextView rpmLabel = createHeader(RPM[r]);
            row.addView(rpmLabel);

            for (int c = 0; c < 16; c++) {

                EditText cell = new EditText(this);

                cell.setText("100");
                cell.setTextSize(13);
                cell.setGravity(Gravity.CENTER);
                cell.setSingleLine(true);

                cell.setInputType(
                        InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL |
                        InputType.TYPE_NUMBER_FLAG_SIGNED
                );

                TableRow.LayoutParams params =
                        new TableRow.LayoutParams(85,60);

                params.setMargins(1,1,1,1);

                cell.setLayoutParams(params);

                final int rr = r;
                final int cc = c;

                cell.setOnFocusChangeListener((v, hasFocus) -> {

                    if (hasFocus) {

                        selectedCell = cell;

                        status.setText(
                                "SELECTED: RPM " + RPM[rr] +
                                " / TPS " + TPS[cc] + "%"
                        );
                    }
                });

                fuelCells[r][c] = cell;

                row.addView(cell);
            }

            table.addView(row);
        }

        vertical.addView(table);
        horizontal.addView(vertical);

        root.addView(
                horizontal,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        /*
         * APPLY CORRECTION
         */
        Button apply = new Button(this);
        apply.setText("APPLY CORRECTION TO SELECTED CELL");

        apply.setOnClickListener(v -> applyCorrection());

        root.addView(apply);

        /*
         * RESET
         */
        Button reset = new Button(this);
        reset.setText("RESET MAP TO 100");

        reset.setOnClickListener(v -> resetMap());

        root.addView(reset);

        setContentView(root);
    }

    private EditText createInput(String hint, String value) {

        EditText edit = new EditText(this);

        edit.setHint(hint);
        edit.setText(value);
        edit.setTextSize(17);
        edit.setSingleLine(true);

        edit.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_FLAG_DECIMAL |
                InputType.TYPE_NUMBER_FLAG_SIGNED
        );

        edit.setPadding(10,5,10,5);

        return edit;
    }

    private TextView createHeader(String text) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextSize(12);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(Color.WHITE);
        view.setBackgroundColor(Color.DKGRAY);
        view.setPadding(5,5,5,5);

        TableRow.LayoutParams params =
                new TableRow.LayoutParams(85,60);

        params.setMargins(1,1,1,1);

        view.setLayoutParams(params);

        return view;
    }

    private void calculateCorrection() {

        try {

            double actual =
                    Double.parseDouble(
                            afrInput.getText().toString()
                    );

            double target =
                    Double.parseDouble(
                            targetInput.getText().toString()
                    );

            /*
             * AFR correction formula
             *
             * correction =
             * (actual / target - 1) × 100
             */
            correction =
                    ((actual / target) - 1.0) * 100.0;

            /*
             * Limit correction
             * to prevent extreme values.
             */
            if (correction > 30)
                correction = 30;

            if (correction < -30)
                correction = -30;

            correctionText.setText(
                    String.format(
                            Locale.US,
                            "Fuel Correction: %.1f %%",
                            correction
                    )
            );

            status.setText("STATUS: CORRECTION CALCULATED");

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Periksa nilai AFR",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void applyCorrection() {

        if (selectedCell == null) {

            Toast.makeText(
                    this,
                    "Pilih cell fuel map terlebih dahulu",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        try {

            double current =
                    Double.parseDouble(
                            selectedCell
                                    .getText()
                                    .toString()
                    );

            double newValue =
                    current * (1.0 + correction / 100.0);

            /*
             * Safety limit.
             */
            if (newValue < 20)
                newValue = 20;

            if (newValue > 200)
                newValue = 200;

            selectedCell.setText(
                    String.format(
                            Locale.US,
                            "%.1f",
                            newValue
                    )
            );

            status.setText(
                    "STATUS: CELL UPDATED"
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Nilai cell tidak valid",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void resetMap() {

        for (int r = 0; r < 16; r++) {

            for (int c = 0; c < 16; c++) {

                fuelCells[r][c].setText("100");
            }
        }

        status.setText("STATUS: MAP RESET");

        Toast.makeText(
                this,
                "Fuel map kembali ke 100",
                Toast.LENGTH_SHORT
        ).show();
    }
}

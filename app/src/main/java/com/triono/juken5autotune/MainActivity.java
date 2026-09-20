package com.triono.juken5autotune;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {

    TextView status;
    TextView correction;

    EditText rpmInput;
    EditText afrInput;
    EditText targetInput;
    EditText tpsInput;

    EditText selectedCell = null;

    final String[] RPM = {
        "1000","1500","2000","2500",
        "3000","3500","4000","4500",
        "5000","5500","6000","6500",
        "7000","7500","8000","8500"
    };

    final String[] TPS = {
        "0","5","10","15",
        "20","25","30","35",
        "40","50","60","70",
        "80","90","95","100"
    };

    EditText[][] fuelMap = new EditText[16][16];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20,20,20,20);

        TextView title = new TextView(this);
        title.setText("Juken 5 Auto Tune");
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("16 × 16 FUEL MAP");
        subtitle.setTextSize(18);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        status = new TextView(this);
        status.setText("STATUS: READY");
        status.setTextSize(17);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0,15,0,15);
        root.addView(status);

        rpmInput = field("RPM", "3000");
        root.addView(rpmInput);

        tpsInput = field("TPS (%)", "20");
        root.addView(tpsInput);

        afrInput = field("AFR ACTUAL", "14.7");
        root.addView(afrInput);

        targetInput = field("AFR TARGET", "13.2");
        root.addView(targetInput);

        Button calculate = new Button(this);
        calculate.setText("CALCULATE CORRECTION");
        root.addView(calculate);

        correction = new TextView(this);
        correction.setText("Correction: 0.0 %");
        correction.setTextSize(20);
        correction.setGravity(Gravity.CENTER);
        correction.setPadding(0,10,0,10);
        root.addView(correction);

        Button apply = new Button(this);
        apply.setText("APPLY TO SELECTED CELL");
        root.addView(apply);

        Button reset = new Button(this);
        reset.setText("RESET MAP");
        root.addView(reset);

        TextView mapTitle = new TextView(this);
        mapTitle.setText("FUEL CORRECTION MAP (%)");
        mapTitle.setTextSize(18);
        mapTitle.setTypeface(null, Typeface.BOLD);
        mapTitle.setPadding(0,15,0,10);
        root.addView(mapTitle);

        /*
         * Horizontal scroll agar 16 kolom
         * bisa digeser pada layar HP.
         */
        HorizontalScrollView horizontal =
                new HorizontalScrollView(this);

        /*
         * Vertical scroll agar 16 baris
         * bisa digeser ke bawah.
         */
        ScrollView vertical = new ScrollView(this);

        TableLayout table = new TableLayout(this);
        table.setStretchAllColumns(false);

        /*
         * HEADER
         */
        TableRow header = new TableRow(this);

        TextView corner = headerCell("RPM / TPS");
        header.addView(corner);

        for (int c = 0; c < 16; c++) {
            TextView h = headerCell(TPS[c]);
            header.addView(h);
        }

        table.addView(header);

        /*
         * 16 BARIS RPM × 16 KOLOM TPS
         */
        for (int r = 0; r < 16; r++) {

            TableRow row = new TableRow(this);

            TextView rpm = headerCell(RPM[r]);
            row.addView(rpm);

            for (int c = 0; c < 16; c++) {

                EditText cell = new EditText(this);

                cell.setText("0.0");
                cell.setTextSize(13);
                cell.setGravity(Gravity.CENTER);
                cell.setSingleLine(true);

                cell.setInputType(
                    InputType.TYPE_CLASS_NUMBER |
                    InputType.TYPE_NUMBER_FLAG_DECIMAL |
                    InputType.TYPE_NUMBER_FLAG_SIGNED
                );

                cell.setSelectAllOnFocus(true);

                /*
                 * Ukuran cell.
                 */
                TableRow.LayoutParams params =
                        new TableRow.LayoutParams(95, 75);

                params.setMargins(2,2,2,2);
                cell.setLayoutParams(params);

                final int rr = r;
                final int cc = c;

                cell.setOnFocusChangeListener(
                    (v, hasFocus) -> {

                        if (hasFocus) {

                            selectedCell =
                                (EditText) v;

                            status.setText(
                                "CELL: RPM " + RPM[rr] +
                                " / TPS " + TPS[cc]
                            );
                        }
                    }
                );

                fuelMap[r][c] = cell;
                row.addView(cell);
            }

            table.addView(row);
        }

        vertical.addView(table);
        horizontal.addView(vertical);

        LinearLayout.LayoutParams mapParams =
                new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1
                );

        root.addView(horizontal, mapParams);

        setContentView(root);

        /*
         * HITUNG KOREKSI AFR
         */
        calculate.setOnClickListener(v ->
                calculateCorrection()
        );

        /*
         * MASUKKAN KOREKSI KE CELL TERPILIH
         */
        apply.setOnClickListener(v -> {

            if (selectedCell == null) {

                status.setText(
                    "PILIH CELL MAP TERLEBIH DAHULU"
                );

                return;
            }

            String value =
                    correction.getText().toString()
                    .replace("Correction: ", "")
                    .replace(" %", "")
                    .trim();

            selectedCell.setText(value);

            status.setText(
                "KOREKSI DITERAPKAN KE CELL"
            );
        });

        /*
         * RESET SEMUA CELL KE 0.0
         */
        reset.setOnClickListener(v -> {

            for (int r = 0; r < 16; r++) {

                for (int c = 0; c < 16; c++) {

                    fuelMap[r][c].setText("0.0");
                }
            }

            status.setText("MAP DIRESET");
        });
    }

    private EditText field(
            String hint,
            String value) {

        EditText edit =
                new EditText(this);

        edit.setHint(hint);
        edit.setText(value);
        edit.setTextSize(18);
        edit.setSingleLine(true);

        edit.setInputType(
            InputType.TYPE_CLASS_NUMBER |
            InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        return edit;
    }

    private TextView headerCell(String text) {

        TextView cell =
                new TextView(this);

        cell.setText(text);
        cell.setTextSize(13);
        cell.setTypeface(null, Typeface.BOLD);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(5,5,5,5);

        TableRow.LayoutParams params =
                new TableRow.LayoutParams(95,75);

        params.setMargins(2,2,2,2);

        cell.setLayoutParams(params);

        return cell;
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

            if (target <= 0) {

                status.setText(
                    "AFR TARGET TIDAK VALID"
                );

                return;
            }

            /*
             * Jika AFR aktual lebih tinggi
             * dari target, campuran lebih miskin
             * sehingga fuel perlu ditambah.
             *
             * Jika AFR aktual lebih rendah
             * dari target, fuel dikurangi.
             */

            double value =
                    ((actual / target) - 1.0) * 100.0;

            /*
             * Batas keamanan koreksi.
             */
            if (value > 30)
                value = 30;

            if (value < -30)
                value = -30;

            correction.setText(
                String.format(
                    "Correction: %+.1f %%",
                    value
                )
            );

            status.setText(
                "AFR CORRECTION CALCULATED"
            );

        } catch (Exception e) {

            status.setText(
                "ERROR: DATA AFR TIDAK VALID"
            );

            correction.setText(
                "Correction: 0.0 %"
            );
        }
    }
}

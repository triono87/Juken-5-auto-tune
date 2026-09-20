package com.triono.juken5autotune;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30, 30, 30, 30);

        TextView title = new TextView(this);
        title.setText("Juken 5 Auto Tune");
        title.setTextSize(28);
        title.setTextColor(Color.DKGRAY);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("AFR Fuel Correction Dashboard");
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        status = new TextView(this);
        status.setText("STATUS: READY");
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 25, 0, 25);
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
        calculate.setText("CALCULATE FUEL CORRECTION");
        root.addView(calculate);

        correction = new TextView(this);
        correction.setText("Fuel Correction: 0.0 %");
        correction.setTextSize(22);
        correction.setGravity(Gravity.CENTER);
        correction.setPadding(0, 25, 0, 25);
        root.addView(correction);

        Button autoTune = new Button(this);
        autoTune.setText("START AUTO TUNE");
        root.addView(autoTune);

        Button stop = new Button(this);
        stop.setText("STOP");
        root.addView(stop);

        calculate.setOnClickListener(v -> calculateCorrection());

        autoTune.setOnClickListener(v -> {
            status.setText("STATUS: AUTO TUNE RUNNING");
            status.setTextColor(Color.rgb(0, 130, 0));
        });

        stop.setOnClickListener(v -> {
            status.setText("STATUS: STOPPED");
            status.setTextColor(Color.RED);
        });

        setContentView(root);
    }

    private EditText field(String label, String value) {
        EditText edit = new EditText(this);
        edit.setHint(label);
        edit.setText(value);
        edit.setTextSize(18);
        edit.setInputType(2);
        edit.setPadding(10, 15, 10, 15);
        return edit;
    }

    private void calculateCorrection() {

        try {
            double actual = Double.parseDouble(
                    afrInput.getText().toString()
            );

            double target = Double.parseDouble(
                    targetInput.getText().toString()
            );

            /*
             * AFR lebih tinggi dari target =
             * campuran terlalu miskin,
             * sehingga fuel perlu ditambah.
             *
             * AFR lebih rendah dari target =
             * campuran terlalu kaya,
             * sehingga fuel perlu dikurangi.
             */

            double correctionValue =
                    ((actual / target) - 1.0) * 100.0;

            // Batasi koreksi agar tidak ekstrem.
            if (correctionValue > 30) {
                correctionValue = 30;
            }

            if (correctionValue < -30) {
                correctionValue = -30;
            }

            correction.setText(
                    String.format(
                            "Fuel Correction: %+.1f %%",
                            correctionValue
                    )
            );

            status.setText("STATUS: AFR DATA CALCULATED");

        } catch (Exception e) {

            status.setText("ERROR: PERIKSA DATA AFR");
            correction.setText("Fuel Correction: --");
        }
    }
}

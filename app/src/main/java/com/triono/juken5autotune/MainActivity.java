package com.triono.juken5autotune;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);
        textView.setText("Juken 5 Auto Tune");
        textView.setTextSize(24);
        textView.setPadding(32, 32, 32, 32);

        setContentView(textView);
    }
}

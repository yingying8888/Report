package com.test.report;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    TextView shareTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        shareTv = findViewById(R.id.shareTv);
        shareTv.setOnClickListener(this);
    }


/*    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.shareTv:
                shareReport();
                break;
        }
    }*/

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.shareTv:
                break;
        }
    }

    private void shareReport() {
    }
}
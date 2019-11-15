package com.pangomicro.zpfeng.cylinder_qrcode;

import android.content.Intent;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText et;
    private RadioButton rb;
    private CheckBox cb;
    Intent i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btnStartTutorial).setOnClickListener(this);
        findViewById(R.id.btnStartColorDetect).setOnClickListener(this);
        findViewById(R.id.btnStartGrayScale).setOnClickListener(this);
        findViewById(R.id.btnStartAdaptiveThresh).setOnClickListener(this);
        findViewById(R.id.btnStartOpenClose).setOnClickListener(this);
        findViewById(R.id.btnBinaryThresh).setOnClickListener(this);
        findViewById(R.id.btnQrContourExtract).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnStartTutorial:
                startActivity(new Intent(MainActivity.this, Tutorial1Activity.class));
                break;
            case R.id.btnStartColorDetect:
                startActivity(new Intent(MainActivity.this, ColorBlobDetectionActivity.class));
                break;
            case R.id.btnStartGrayScale:
                startActivity(new Intent(MainActivity.this, GrayScaleActivity.class));
                break;
            case R.id.btnStartAdaptiveThresh:
                i = new Intent(MainActivity.this, AdaptiveThreshActivity.class);
                et = (EditText) findViewById(R.id.etAdaptiveThresh);
                if (et.length() != 0) {
                    int bs = Integer.parseInt(et.getText().toString());
                    if (bs > 9) {
                        i.putExtra(AdaptiveThreshActivity.BS,
                                (bs % 2 == 0 ? bs + 1 : bs));
                    }
                    else {
                        i.putExtra(AdaptiveThreshActivity.BS, 49);
                    }
                    et = (EditText) findViewById(R.id.etAdaptiveDelta);
                    if (et.length() != 0) {
                        bs = Integer.parseInt(et.getText().toString());
                        if (bs >= 0)
                            i.putExtra(AdaptiveThreshActivity.DL, bs);
                        else
                            i.putExtra(AdaptiveThreshActivity.DL, 10);
                    }
                }
                cb = (CheckBox) findViewById(R.id.cbGetContours);
                if (cb != null) {
                    if (cb.isChecked())
                        i.putExtra(AdaptiveThreshActivity.GC, true);
                    else
                        i.putExtra(AdaptiveThreshActivity.GC, false);
                }
                startActivity(i);
                break;
            case R.id.btnStartOpenClose:
                i = new Intent(MainActivity.this, OpenCloseActivity.class);
                rb = (RadioButton) findViewById(R.id.rbDoOpen);
                if (rb.isChecked())
                    i.putExtra(OpenCloseActivity.OPENCLOSE, true);
                else
                    i.putExtra(OpenCloseActivity.OPENCLOSE, false);
                rb = (RadioButton) findViewById(R.id.rbType1);
                if (rb.isChecked())
                    i.putExtra(OpenCloseActivity.TYPE, OpenCloseActivity.MORPH_RECT);
                else {
                    rb = (RadioButton) findViewById(R.id.rbType2);
                    if (rb.isChecked())
                        i.putExtra(OpenCloseActivity.TYPE, OpenCloseActivity.MORPH_CROSS);
                    else
                        i.putExtra(OpenCloseActivity.TYPE, OpenCloseActivity.MORPH_ELLIPSE);
                }
                et = (EditText)findViewById(R.id.etKernelSize);
                if (et.length() != 0) {
                    int ks = Integer.parseInt(et.getText().toString());
                    if (ks >= 3)
                        i.putExtra(OpenCloseActivity.KERNELSIZE, ks);
                    else
                        i.putExtra(OpenCloseActivity.KERNELSIZE, 3);
                }
                et = (EditText) findViewById(R.id.etAdaptiveThresh);
                if (et.length() != 0) {
                    int bs = Integer.parseInt(et.getText().toString());
                    if (bs > 9) {
                        i.putExtra(OpenCloseActivity.BS,
                                (bs % 2 == 0 ? bs + 1 : bs));
                    }
                    else {
                        i.putExtra(OpenCloseActivity.BS, 49);
                    }
                    et = (EditText) findViewById(R.id.etAdaptiveDelta);
                    if (et.length() != 0) {
                        bs = Integer.parseInt(et.getText().toString());
                        if (bs >= 0)
                            i.putExtra(OpenCloseActivity.DL, bs);
                        else
                            i.putExtra(OpenCloseActivity.DL, 10);
                    }
                }
                startActivity(i);
                break;
            case R.id.btnBinaryThresh:
                et = (EditText) findViewById(R.id.etBinaryThresh);
                int thresh = Integer.parseInt(et.getText().toString());
                i = new Intent(MainActivity.this, BinaryThreshActivity.class);
                i.putExtra(BinaryThreshActivity.THRESH, thresh);
                startActivity(i);
                break;
            case R.id.btnQrContourExtract:
                i = new Intent(MainActivity.this, QrActivity.class);
                startActivity(i);
                break;
        }
    }
}

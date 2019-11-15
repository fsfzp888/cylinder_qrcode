package com.pangomicro.zpfeng.cylinder_qrcode;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class BinaryThreshActivity extends Activity implements CvCameraViewListener2 {

    private CameraBridgeViewBase mOpenCvBaseCameraView;
    public static final String THRESH = "Thresh";
    private static final String TAG = "OCVSample::Activity";
    private int thresh;
    Mat binMat;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvBaseCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Call onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//must be extends Activity
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_binary_thresh);
        mOpenCvBaseCameraView = (CameraBridgeViewBase) findViewById(R.id.binaryThreshCameraView);
        mOpenCvBaseCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvBaseCameraView.setCvCameraViewListener(this);
        Intent intent = getIntent();
        thresh = intent.getIntExtra(THRESH, 127);
        if (thresh > 220)
            thresh = 220;
        else if (thresh < 25)
            thresh = 25;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvBaseCameraView != null)
            mOpenCvBaseCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvBaseCameraView != null)
            mOpenCvBaseCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        binMat = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Imgproc.threshold(inputFrame.gray(), binMat, thresh, 255, Imgproc.THRESH_BINARY);
        return binMat;
    }
}

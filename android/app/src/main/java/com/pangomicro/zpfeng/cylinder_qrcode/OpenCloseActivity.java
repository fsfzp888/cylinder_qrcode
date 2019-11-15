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
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class OpenCloseActivity extends Activity implements CvCameraViewListener2 {

    private static final String TAG = "OCVSample::Activity";
    public static final String TYPE = "MORPH";
    public static final String OPENCLOSE = "OpenClose";
    public static final String KERNELSIZE = "KernelSize";
    public static final String BS = "AdaptiveThreshBlockSize";
    public static final String DL = "AdaptiveThreshDeltaSize";
    public static final int MORPH_RECT = 0;
    public static final int MORPH_CROSS = 1;
    public static final int MORPH_ELLIPSE = 2;
    private boolean isOpen = true;
    private int type = 1;
    private int kernelSize;
    private int blockSize;
    private int delta;
    private Mat mPic;
    private CameraBridgeViewBase mOpenCvBaseCameraView;
//    private List<MatOfPoint> mContours;
//    private Mat mHierarchy;

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

    public OpenCloseActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//must be extends Activity
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_open_close);
        mOpenCvBaseCameraView = (CameraBridgeViewBase)findViewById(R.id.openCloseCameraView);
        mOpenCvBaseCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvBaseCameraView.setCvCameraViewListener(this);
        Intent i = getIntent();
        isOpen = i.getBooleanExtra(OPENCLOSE, true);
        type = i.getIntExtra(TYPE, MORPH_RECT);
        kernelSize = i.getIntExtra(KERNELSIZE, 9);
        blockSize = i.getIntExtra(BS, 49);
        delta = i.getIntExtra(DL, 10);
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
        mPic = new Mat(height, width, CvType.CV_32SC4);
//        mContours = new ArrayList<>();
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Imgproc.adaptiveThreshold(inputFrame.gray(), mPic, 255,
                Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY,
                blockSize, delta);
        if (isOpen) {
            Imgproc.morphologyEx(mPic, mPic, Imgproc.MORPH_OPEN,
                    Imgproc.getStructuringElement(type, new Size(kernelSize, kernelSize)));
            /*mContours.clear();
            if (mHierarchy == null)
                mHierarchy = new Mat();
            else {
                mHierarchy.release();
                mHierarchy = new Mat();
            }
            Imgproc.findContours(mPic, mContours, mHierarchy, Imgproc.RETR_EXTERNAL,
                    Imgproc.CHAIN_APPROX_SIMPLE);
            for (int j = 0; j < mContours.size(); j++) {
                MatOfPoint2f mp = new MatOfPoint2f(mContours.get(j).toArray());
                double area = Imgproc.contourArea(mContours.get(j));
                RotatedRect rotRect = Imgproc.minAreaRect(mp);
                double width = rotRect.size.width, height = rotRect.size.height;
                double max = Math.max(width, height), min = Math.min(width,height);
                if (area*1.5 < height*width || max/min > 2)
                    mContours.remove(j);
            }
            Imgproc.drawContours(mPic, mContours, -1, new Scalar(255, 0, 0), 2);*/
        } else {
            Imgproc.morphologyEx(mPic, mPic, Imgproc.MORPH_CLOSE,
                    Imgproc.getStructuringElement(type, new Size(kernelSize, kernelSize)));
        }
        return mPic;
    }
}

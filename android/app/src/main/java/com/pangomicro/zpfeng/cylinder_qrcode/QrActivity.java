package com.pangomicro.zpfeng.cylinder_qrcode;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

public class QrActivity extends Activity implements CvCameraViewListener2,
        View.OnClickListener {
    private static final String TAG = "OCVSample::Activity";
    private CameraBridgeViewBase mOpenCvBaseCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvBaseCameraView.enableView();
//                    mOpenCvBaseCameraView.setOnTouchListener(QrActivity.this);
                    mOpenCvBaseCameraView.setOnClickListener(QrActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public QrActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//must be extends Activity
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_qr);
        mOpenCvBaseCameraView = (CameraBridgeViewBase)findViewById(R.id.QRCameraView);
        mOpenCvBaseCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvBaseCameraView.setCvCameraViewListener(this);
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
        i = 0;
//        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        if (i < 8) {
            i++;
            return inputFrame.rgba();
        }
//        Log.i(TAG, "CameraFrame");
//        isProcess = true;
        Mat outImg = inputFrame.rgba();
//        isProcess = false;
        if (qr.process(outImg)) {
            for (int i = 0; i < 4; ++i)
                if (qr.get4Pts()[i] == null)
                    return outImg;
            MatOfPoint mop = new MatOfPoint(qr.get4Pts());
            Rect rect = Imgproc.boundingRect(mop);
            int height = rect.height;
            int width = rect.width;
            height *=1.8;
            width *= 1.8;
            double x = rect.tl().x - rect.width*0.4;
            double y = rect.tl().y - rect.height*0.4;
            if (x < 0)
                x = 0;
            if ((x+width) >= outImg.cols())
                width = outImg.cols()-(int)x-1;
            if (y < 0)
                y = 0;
            if ((y+height) >= outImg.rows())
                height = outImg.rows()-(int)y-1;
            Rect rect1 = new Rect((int)x, (int)y, width, height);
//            mRgba = new Mat(rect.height, rect.width, CvType.CV_8UC4);
            mRgba = outImg.submat(rect1).clone();
//            isProcess = true;
        }
        qr.drawArcs(outImg);
        qr.drawFps(outImg);
//        mRgba = inputFrame.rgba().clone();
//        isProcess = false;
        return outImg;
    }

    private Qr qr = new Qr();
    private Mat mRgba;
    private int i;
//    private boolean isProcess;

//    @Override
//    public boolean onTouch(View v, MotionEvent event) {
////        if (isProcess)
////            return false;
//        if (mRgba == null)
//            return false;
////        MatOfPoint mop = new MatOfPoint(resPts);
////        Rect rect = Imgproc.boundingRect(mop);
////        Mat roim = mRgba.submat(rect);
//        Bitmap bmp;
//        bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(mRgba, bmp);
//        Intent intent=new Intent(QrActivity.this,QrRectifyActivity.class);
//        intent.putExtra("bitmap", bmp);
//        startActivity(intent);
//        return true;
//    }

    @Override
    public void onClick(View v) {
//        if (!isProcess)
//            return;
//        Bitmap bmp;
//        bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(mRgba, bmp);
        if (mRgba == null)
            return;
        IntentMat.intentMat = mRgba;
        Intent intent=new Intent(QrActivity.this, QrRectifyActivity.class);
//        intent.putExtra("bitmap",  new Parcelable[]{bmp});
        mOpenCvBaseCameraView.disableView();
        startActivity(intent);
        mOpenCvBaseCameraView.enableView();
    }
}

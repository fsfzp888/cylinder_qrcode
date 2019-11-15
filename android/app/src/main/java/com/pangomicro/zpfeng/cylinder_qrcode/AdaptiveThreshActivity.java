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
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class AdaptiveThreshActivity extends Activity implements CvCameraViewListener2{

    public static final String BS = "blockSize";
    public static final String DL = "delta";
    public static final String GC = "getContours";
    private static final String TAG = "OCVSample::Activity";
    private Mat mThresh;
    private CameraBridgeViewBase mOpenCvBaseCameraView;
    private int blockSize = 0;
    private int delta = 0;
    private List<MatOfPoint> mContours;
    private List<MatOfPoint> mFinderPattern;
    private List<Point> mFinderPatternPoints;
    private Scalar  CONTOUR_COLOR;
    private Mat mHierarchy;
    private boolean isShowContours;

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

    public AdaptiveThreshActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//must be extends Activity
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_adaptive_thresh);
        mOpenCvBaseCameraView = (CameraBridgeViewBase)findViewById(R.id.adaptiveThreshCameraView);
        mOpenCvBaseCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvBaseCameraView.setCvCameraViewListener(this);
        Intent i = getIntent();
        blockSize = i.getIntExtra(BS, 49);
        delta = i.getIntExtra(DL, 10);
        isShowContours = i.getBooleanExtra(GC, false);
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
        mThresh = new Mat(height, width, CvType.CV_8UC1);
        CONTOUR_COLOR = new Scalar(255, 0, 0, 0);
        mContours = new ArrayList<>();
        mFinderPattern = new ArrayList<>();
        mFinderPatternPoints = new ArrayList<>();
    }

    @Override
    public void onCameraViewStopped() {

    }

    private Qr qr = new Qr();

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//        Mat outImg = inputFrame.rgba();
//        qr.process(outImg);
//        qr.drawFps(outImg);
//        return outImg;
        Imgproc.adaptiveThreshold(inputFrame.gray(), mThresh, 255,
                Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, blockSize, delta);
        int bs = (blockSize/10);
        if (bs < 5)
            bs = 5;
        bs = (bs%2==0) ? bs+1:bs;
        Imgproc.medianBlur(mThresh, mThresh, bs);
        if (isShowContours) {
            mContours.clear();
            if (mHierarchy == null)
                mHierarchy = new Mat();
            else {
                mHierarchy.release();
                mHierarchy = new Mat();
            }
            Imgproc.findContours(mThresh, mContours, mHierarchy, Imgproc.RETR_TREE,
                    Imgproc.CHAIN_APPROX_SIMPLE);
            /*
            * The Hierarchy Mat has one row and many cols
            * each pixel is a vector contains 4 double values
            * the following is the test code
            * 0: next 1: previous 2: child 3: parent
            * */
            /*int rows = mHierarchy.rows();
            int cols = mHierarchy.cols();
            Log.d(TAG, "mHierarchy ROWS: " + rows);
            Log.d(TAG, "mHierarchy COLS: " + cols);
            int i, j;
            double[] data;
            for (i = 0; i < rows; ++i) {
                for (j = 0; j < cols; ++j) {
                    data = mHierarchy.get(i, j);
                    Log.d(TAG, "mHierarchy data length: " + data.length);
                }
            }*/
            int size = mHierarchy.cols();
            int i;
            int c_index, cc_index;
            double[] data, c_data;
            double[] cc_data;
            mFinderPattern.clear();
            mFinderPatternPoints.clear();
            for (i = 0; i < size; ++i) {
                data = mHierarchy.get(0, i);
                c_index = (int) data[2];
                if (c_index != -1) {
                    c_data = mHierarchy.get(0, c_index);
                    cc_index = (int) c_data[2];
                    if (cc_index != -1) {
                        cc_data = mHierarchy.get(0, cc_index);
                        if (((int)c_data[0]) == -1
                                && ((int)c_data[1]) == -1
                                && ((int)cc_data[0]) == -1
                                && ((int)cc_data[1]) == -1) {
                            double area, area_c, area_cc;
                            area = Imgproc.contourArea(mContours.get(i));
                            area_c = Imgproc.contourArea(mContours.get(c_index));
                            area_cc = Imgproc.contourArea(mContours.get(cc_index));
                            //if (area < area_c*2.8 && area_c < area_cc*2.8)
                            if (area < area_c * 4 && area > area_c*1.5
                                    && area_c < area_cc*4 && area_c > area_cc*1.5)
                                mFinderPattern.add(mContours.get(i));
                            //if (mFinderPattern.size() == 3)
                                //break;
                        }
                    }
                }
            }
            Mat mContourPic = inputFrame.rgba();
            if (mFinderPattern.size() >= 1) {
                for (int j = 0; j < mFinderPattern.size(); j++) {
                    List<Point> lp = mFinderPattern.get(j).toList();
                    mFinderPatternPoints.addAll(lp);
                }
                Point[] point = new Point[mFinderPatternPoints.size()];
                for (int j = 0; j < mFinderPatternPoints.size(); j++) {
                    point[j] = mFinderPatternPoints.get(j);
                }
                /*
                MatOfPoint mp = new MatOfPoint(point);
                Rect rect = Imgproc.boundingRect(mp);
                Imgproc.rectangle(mContourPic, rect.tl(), rect.br(), new Scalar(0, 255, 0), 2);
                */
                MatOfPoint2f mp = new MatOfPoint2f(point);
                RotatedRect rotRect = Imgproc.minAreaRect(mp);
                Point vertices[] = new Point[4];
                rotRect.points(vertices);
                for (int p = 0; p < 4; p++) {
                    Imgproc.line(mContourPic, vertices[p], vertices[(p+1)%4], new Scalar(0, 255, 0),
                            2);
                }
                Imgproc.drawContours(mContourPic, mFinderPattern, -1, CONTOUR_COLOR, 2);
//                if (mFinderPattern.size() == 3) {
//                    Mat mHoughLine = new Mat(inputFrame.rgba().height(), inputFrame.rgba().width(),
//                            inputFrame.rgba().type());
//                }

                /*
                * convexHull计算凸包，
                * 点的index存储在一个MatOfInt中，它的列是1， 行是顺时针存储的凸包点的index，有多行
                * 每个元素数据长度为1的double数组，存储一个index值
                * 并不合适，用在这里，因为无法限制凸包点最大个数
                * 而MatOfPoint也是只有一列，并且有多行。没一行有一个数据，这个数据是2单位长度的double数组
                * 存储点坐标，看来MatOfxxx都是这种形式，用在JAVA里也是为了和C++/C接口兼容，使用JNI就是这样？
                * 这个不太好
                * */
//                MatOfPoint mh = new MatOfPoint(point);
//                int cols = mh.cols();
//                int rows = mh.rows();
//                Log.d(TAG, "MatOfPoint cols: " + cols);
//                Log.d(TAG, "MatOfPoint rows: " + rows);
//                double[] dataHull;
//                for (i = 0; i < rows; ++i) {
//                    for (int j = 0; j < cols; ++j) {
//                        dataHull = mh.get(i, j);
//                        Log.d(TAG, "mHull data length: " + dataHull.length);
//                        for (int k = 0; k < dataHull.length; k++) {
//                            Log.d(TAG, "mHull data " + k + " : " + dataHull[k]);
//                        }
//                    }
//                }
//                MatOfInt hull = new MatOfInt();
//                Imgproc.convexHull(mh, hull, true);
//                int cols = hull.cols();
//                int rows = hull.rows();
//                Log.d(TAG, "hull cols: " + cols);
//                Log.d(TAG, "hull rows: " + rows);
//                double[] dataHull;
//                for (i = 0; i < rows; ++i) {
//                    for (int j = 0; j < cols; ++j) {
//                        dataHull = hull.get(i, j);
//                        Log.d(TAG, "mHull data length: " + dataHull.length);
//                        for (int k = 0; k < dataHull.length; k++) {
//                            Log.d(TAG, "mHull data " + k + " : " + dataHull[k]);
//                        }
//                    }
//                }

            }

            return mContourPic;
        }
        return mThresh;
    }
}

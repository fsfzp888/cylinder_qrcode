package com.pangomicro.zpfeng.cylinder_qrcode;

import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zczx1 on 2016/4/26.
 */
public class Qr {
    public Qr() {

    }

    public boolean process(Mat srcImg) {
        COLS = srcImg.cols();
        ROWS = srcImg.rows();
        binImg = new Mat(ROWS, COLS, CvType.CV_8UC1);
        grayImg = new Mat(ROWS, COLS, CvType.CV_8UC1);
        Imgproc.cvtColor(srcImg, grayImg, Imgproc.COLOR_BGR2GRAY);
        Imgproc.adaptiveThreshold(grayImg, binImg, 255,
                Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, block_size, delta);
        int bs = (block_size/10);
        if (bs < 5)
            bs = 5;
        bs = (bs%2==0) ? bs+1:bs;
        blur_size = bs;
        Imgproc.medianBlur(binImg, binImg, bs);
        mContours.clear();
        mFinderPattern.clear();
        Mat mHierarchy = new Mat();
        Imgproc.findContours(binImg, mContours, mHierarchy, Imgproc.RETR_TREE,
                Imgproc.CHAIN_APPROX_SIMPLE);
        int size = mHierarchy.cols();
        int i;
        int c_index, cc_index;
        double[] data, c_data;
        double[] cc_data;
        mFinderPattern.clear();
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
        if (mFinderPattern.size() == 3) {
            if (fp_manager.setFps(mFinderPattern.get(0).toList(),
                    mFinderPattern.get(1).toList(),
                    mFinderPattern.get(2).toList(), srcImg.cols(), srcImg.rows()))
                is3FP = true;
//            fp_manager.rectify4Corners(grayImg);
//            fp_manager.drawArcs(srcImg);

        } else {
            is3FP = false;
//            mFinderPatternPoints.clear();
//            for (int j = 0; j < mFinderPattern.size(); j++) {
//                List<Point> lp = mFinderPattern.get(j).toList();
//                mFinderPatternPoints.addAll(lp);
//            }
//            Point[] point = new Point[mFinderPatternPoints.size()];
//            for (int j = 0; j < mFinderPatternPoints.size(); j++) {
//                point[j] = mFinderPatternPoints.get(j);
//            }
//            Imgproc.drawContours(srcImg, mFinderPattern, -1, new Scalar(0, 0, 255), 2);
            return false;
        }
        return true;
    }

    public boolean rectifyParabola(Mat grayImg) {
        return fp_manager.rectify4Corners(grayImg);
    }

    public void drawArcs(Mat srcImg) {
        if (is3FP) {
            fp_manager.drawArcs(srcImg);
        } else {
            mFinderPatternPoints.clear();
            for (int j = 0; j < mFinderPattern.size(); j++) {
                List<Point> lp = mFinderPattern.get(j).toList();
                mFinderPatternPoints.addAll(lp);
            }
            Point[] point = new Point[mFinderPatternPoints.size()];
            for (int j = 0; j < mFinderPatternPoints.size(); j++) {
                point[j] = mFinderPatternPoints.get(j);
            }
            Imgproc.drawContours(srcImg, mFinderPattern, -1, new Scalar(255, 0, 0), 2);
        }
    }

    public void drawFps(Mat srcImg) {
        if (is3FP) {
            Imgproc.circle(srcImg, fp_manager.getCp(), 3, new Scalar(255, 0, 0), -1);
            Imgproc.circle(srcImg, fp_manager.getLp(), 3, new Scalar(255, 0, 0), -1);
            Imgproc.circle(srcImg, fp_manager.getRp(), 3, new Scalar(255, 0, 0), -1);
            fp_manager.drawAllFps(srcImg);
        } else {
            mFinderPatternPoints.clear();
            for (int j = 0; j < mFinderPattern.size(); j++) {
                List<Point> lp = mFinderPattern.get(j).toList();
                mFinderPatternPoints.addAll(lp);
            }
            Point[] point = new Point[mFinderPatternPoints.size()];
            for (int j = 0; j < mFinderPatternPoints.size(); j++) {
                point[j] = mFinderPatternPoints.get(j);
            }
            Imgproc.drawContours(srcImg, mFinderPattern, -1, new Scalar(255, 0, 0), 2);
        }
    }

//
//    public int getBlur_size() {
//        return blur_size;
//    }
//
//    public void setBlur_size(int blur_size) {
//        this.blur_size = blur_size;
//    }

    public Mat performPerspectiveTransform(Mat srcImg) {
        srcPts[0] = fp_manager.get4PtsForPerspectiveTransform(0);
        srcPts[1] = fp_manager.get4PtsForPerspectiveTransform(1);
        srcPts[2] = fp_manager.get4PtsForPerspectiveTransform(2);
        srcPts[3] = fp_manager.get4PtsForPerspectiveTransform(3);
        boxSize = fp_manager.getSizeOfBox();

        dstPts[0] = new Point((boxSize)/5, (boxSize)/5);
        dstPts[1] = new Point((boxSize)*4/5, (boxSize)/5);
        dstPts[2] = new Point((boxSize)/5, (boxSize)*4/5);
        dstPts[3] = new Point((boxSize)*4/5, (boxSize)*4/5);
        Mat src_mat=new Mat(4,1,CvType.CV_32FC2);
        Mat dst_mat=new Mat(4,1,CvType.CV_32FC2);
        src_mat.put(0, 0, srcPts[0].x, srcPts[0].y, srcPts[1].x, srcPts[1].y,
                srcPts[2].x, srcPts[2].y, srcPts[3].x, srcPts[3].y);
        dst_mat.put(0, 0, dstPts[0].x, dstPts[0].y, dstPts[1].x, dstPts[1].y,
                dstPts[2].x, dstPts[2].y, dstPts[3].x, dstPts[3].y);
        Mat warpMat = Imgproc.getPerspectiveTransform(src_mat, dst_mat);
        Mat res = new Mat(boxSize, boxSize, srcImg.type(), new Scalar(0, 0, 0));
        Imgproc.warpPerspective(srcImg, res, warpMat, res.size());
        resImg = res;
        Mat mask = new Mat(srcImg.cols(), srcImg.rows(), CvType.CV_8UC1, new Scalar(0));
        fp_manager.drawWhiteArcs(mask);
        maskImg = new Mat(boxSize, boxSize, CvType.CV_8UC1, new Scalar(0));
        Imgproc.warpPerspective(mask, maskImg, warpMat, maskImg.size());
        return res;
    }

    public Mat breakUpAndRecombine(Mat srcImg) {
        if (resImg == null)
            performPerspectiveTransform(srcImg);
        boxSize = fp_manager.getSizeOfBox();
        List<Point> up_pts, down_pts;
        up_pts = new ArrayList<>();
        down_pts = new ArrayList<>();
        double[] data;
//        double[] data1;
        Point tmp = null;
        int x, y, i;
        for (x = boxSize/5; x < boxSize*4/5; ++x) {
            for (y = 0; y < boxSize; ++y) {
                data = maskImg.get(y, x);
                if (data[0] >= 1) {
                    tmp = new Point(x, y);
//                    while (data[0] >= 1) {
//                        ++y;
//                        data1 = data;
//                        data = maskImg.get(y, x);
//                        if (data1[0] < data[0])
//                            tmp = new Point(x, y);
//                    }

                    break;
                }
            }
            if (tmp != null) {
                up_pts.add(tmp);
                tmp = null;
            }
            tmp = null;
            for (y = boxSize-1; y >= 0; --y) {
                data = maskImg.get(y, x);
                if (data[0] >= 1) {
                    tmp = new Point(x, y);
//                    while (data[0] >= 1) {
//                        --y;
//                        data1 = data;
//                        data = maskImg.get(y, x);
//                        if (data1[0] < data[0])
//                            tmp = new Point(x, y);
//                    }
                    break;
                }
            }
            if (tmp != null) {
                down_pts.add(tmp);
                tmp = null;
            }
        }
        double up_arc_length = 0;
        double down_arc_length = 0;
        double t1, t2;
        int rec = boxSize/100;
        for (i = 0; i < up_pts.size()-1; ++i) {
            t1 = up_pts.get(i+1).x - up_pts.get(i).x;
            t2 = up_pts.get(i+1).y - up_pts.get(i).y;
            up_arc_length += Math.sqrt(t1*t1+t2*t2);
        }
        for (i = 0; i < down_pts.size()-1; ++i) {
            t1 = down_pts.get(i+1).x - down_pts.get(i).x;
            t2 = down_pts.get(i+1).y - down_pts.get(i).y;
            down_arc_length += Math.sqrt(t1*t1+t2*t2);
        }
        up_arc_length += rec*2;
        down_arc_length += rec*2;
        double single_arc_len = (up_arc_length+down_arc_length)/MASK_CUT_SIZE;
        List<Point> up_pers_pts, down_pers_pts;
        up_pers_pts = new ArrayList<>();
        down_pers_pts = new ArrayList<>();
        up_pers_pts.add(new Point(boxSize/5, boxSize/5));
        down_pers_pts.add(new Point(boxSize/5, boxSize*4/5));
        double len_sum = 0, old_sum;
        int j , ss = up_pts.size();
        int cur_index = 0;
        for (i = 1; i < MASK_CUT_SIZE; ++i) {
            for (j = cur_index; j < ss-1; ++j) {
                if (j == 0)
                    len_sum += rec*2;
                old_sum = len_sum;
                t1 = up_pts.get(j+1).x - up_pts.get(j).x;
                t2 = up_pts.get(j+1).y - up_pts.get(j).y;
                len_sum += Math.sqrt(t1*t1+t2*t2);
                t1 = down_pts.get(j+1).x - down_pts.get(j).x;
                t2 = down_pts.get(j+1).y - down_pts.get(j).y;
                len_sum += Math.sqrt(t1*t1+t2*t2);
                if (len_sum >= single_arc_len) {
                    if ((single_arc_len-old_sum) > (len_sum-single_arc_len)) {
                        up_pers_pts.add(up_pts.get(j+1));
                        down_pers_pts.add(down_pts.get(j+1));
                        cur_index = j+1;
                    } else {
                        up_pers_pts.add(up_pts.get(j));
                        down_pers_pts.add(down_pts.get(j));
                        cur_index = j;
                    }
                    len_sum = 0;
                    break;
                }
            }
        }
        up_pers_pts.add(new Point(boxSize*4/5, boxSize/5));
        down_pers_pts.add(new Point(boxSize*4/5, boxSize*4/5));
        List<Mat> bMat;
        bMat = new ArrayList<>();
        int m = (boxSize*3/5)/MASK_CUT_SIZE * MASK_CUT_SIZE;
        int n = m/MASK_CUT_SIZE;
        Point[] spts = new Point[4];
        Point[] dpts = new Point[4];
        dpts[0] = new Point(0, 0);
        dpts[1] = new Point(0, m);
        dpts[2] = new Point(n, 0);
        dpts[3] = new Point(n, m);
        Mat src_mat=new Mat(4,1,CvType.CV_32FC2);
        Mat dst_mat=new Mat(4,1,CvType.CV_32FC2);
        dst_mat.put(0, 0, dpts[0].x, dpts[0].y, dpts[1].x, dpts[1].y,
                dpts[2].x, dpts[2].y, dpts[3].x, dpts[3].y);
        for (i = 0; i < MASK_CUT_SIZE; ++i) {
            Mat img = new Mat(m, n, CvType.CV_8UC1, new Scalar(0));
            spts[0] = up_pers_pts.get(i);
            spts[1] = down_pers_pts.get(i);
            spts[2] = up_pers_pts.get(i+1);
            spts[3] = down_pers_pts.get(i+1);
            src_mat.put(0, 0, spts[0].x, spts[0].y, spts[1].x, spts[1].y,
                    spts[2].x, spts[2].y, spts[3].x, spts[3].y);
            Mat warpMat = Imgproc.getPerspectiveTransform(src_mat, dst_mat);
            Imgproc.warpPerspective(resImg, img, warpMat, img.size());
            bMat.add(img);
        }
        Mat res = new Mat(m, m, CvType.CV_8UC1, new Scalar(0));
        for (j = 0; j < bMat.size(); ++j) {
            for (int p = 0; p < m; ++p) {
                for (int q = 0; q < n; ++q) {
                    res.put(p, q+n*j, bMat.get(j).get(p, q));
                }
            }
        }
        Imgproc.adaptiveThreshold(res, res, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C,
                Imgproc.THRESH_BINARY, block_size*2+1, delta);
        Imgproc.equalizeHist(res, res);
        Imgproc.medianBlur(res, res, blur_size);
//        Imgproc.morphologyEx(res, res, Imgproc.MORPH_OPEN,
//                Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
//        Imgproc.threshold(res, res, 50, 255, CvType.CV_8UC1);
//
        return res;
    }

    public int getBlock_size() {
        return block_size;
    }

    public void setBlock_size(int block_size) {
        this.block_size = block_size;
    }

    public int getDelta() {
        return delta;
    }

    public void setDelta(int delta) {
        this.delta = delta;
    }

    public Point[] get4Pts() {
        return fp_manager.getUpdownLR();
    }

    public int MASK_CUT_SIZE = 16;
    private int block_size = 49;
    private int delta = 0;
    private int blur_size = 9;
    private Point srcPts[] = new Point[4];
    private Point dstPts[] = new Point[4];
    private List<MatOfPoint> mContours = new ArrayList<>();
    private List<MatOfPoint> mFinderPattern = new ArrayList<>();
    private List<Point> mFinderPatternPoints = new ArrayList<>();
    private FpManager fp_manager = new FpManager();
    private Mat grayImg, binImg, resImg, maskImg;
    private boolean is3FP = false;
    private int boxSize;
    private int COLS, ROWS;
}

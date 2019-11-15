package com.pangomicro.zpfeng.cylinder_qrcode;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by zczx1 on 2016/4/23.
 */
public class FpManager {
    private static final String TAG = "Qr::FPManager";
    public FpManager() {
        left = new FinderPattern();
        center = new FinderPattern();
        right = new FinderPattern();
        arcUp = new Arc();
        arcDown = new Arc();
        arcLeft = new Arc();
        arcRight = new Arc();
    }

    public boolean setFps(List<Point> fp1, List<Point> fp2, List<Point> fp3,
                       int cols, int rows) {
        COLS = cols;
        ROWS = rows;
        if (!left.setFps(fp1))
            return false;
        if (!center.setFps(fp2))
            return false;
        if (!right.setFps(fp3))
            return false;
        FinderPattern tmp[] = {left, center, right};
        Point tps[] = new Point[3];
        tps[0] = left.getCenterPoint();
        tps[1] = center.getCenterPoint();
        tps[2] = right.getCenterPoint();
        double length[] = new double[3];
        int i;
        for (i = 0; i < 3; ++i) {
            length[i] = Math.sqrt(Math.pow(tps[i].y - tps[(i+1)%3].y, 2) +
                    Math.pow(tps[i].x - tps[(i+1)%3].x, 2));
        }
        int center_index, left_index, right_index, max_index = 0;
        for (i = 1; i < 3; ++i) {
            if (length[i] > length[max_index])
                max_index = i;
        }
        center_index = (max_index + 2) % 3;
        Point centerAll = new Point();
        centerAll.x = 0;
        centerAll.y = 0;
        for (i = 0; i < 3; ++i) {
            centerAll.x += tps[i].x;
            centerAll.y += tps[i].y;
        }
        centerAll.x /= 3;
        centerAll.y /= 3;
        Point vecC, vec;
        vecC = new Point();
        vec = new Point();
        vecC.x = tps[center_index].x - centerAll.x;
        vecC.y = tps[center_index].y - centerAll.y;
        vec.x = tps[(center_index+1)%3].x - centerAll.x;
        vec.y = tps[(center_index+1)%3].y - centerAll.y;
        double ans = vecC.x*vec.y-vecC.y*vec.x;//计算二维向量叉积
        if (ans < 0) {
            left_index = (center_index+1)%3;
            right_index = (center_index+2)%3;
        } else {
            left_index = (center_index+2)%3;
            right_index = (center_index+1)%3;
        }
        center = tmp[center_index];
        left = tmp[left_index];
        right = tmp[right_index];
        L = new Point();
        R = new Point();
        C = new Point();
        C = tps[center_index];
        L = tps[left_index];
        R = tps[right_index];
        if (vecC.x <= 0 && vecC.y < 0) {
            fpType = finder_pattern_type._LL;
        } else if (vecC.x > 0 && vecC.y <=0) {
            fpType = finder_pattern_type._ML;
        } else if (vecC.x >= 0 && vecC.y > 0) {
            fpType = finder_pattern_type._MM;
        } else {
            fpType = finder_pattern_type._LM;
        }

        //在这里需要对FinderPattern的上下四边找到一种方法来重新矫正错误的边分类方法
        //......
        Point vecCL = new Point();
        Point vecCR = new Point();
        vecCL.x = L.x - C.x;
        vecCL.y = L.y - C.y;
        vecCR.x = R.x - C.x;
        vecCR.y = R.y - C.y;
        switch (fpType) {
            case _LL:
            case _MM:
                if (!center.isArcsTypeCorrect(vecCL, true))
                    center.swapArcs();
                if (!left.isArcsTypeCorrect(vecCL, true))
                    left.swapArcs();
                if (!right.isArcsTypeCorrect(vecCR, false))
                    right.swapArcs();
                break;
            case _ML:
            case _LM:
                if (!center.isArcsTypeCorrect(vecCL, false))
                    center.swapArcs();
                if (!left.isArcsTypeCorrect(vecCL, false))
                    left.swapArcs();
                if (!right.isArcsTypeCorrect(vecCR, true))
                    right.swapArcs();
                break;
        }

        List<Point> vecUp, vecDown, vecLeft, vecRight;
        vecUp = new ArrayList<>();
        vecDown = new ArrayList<>();
        vecLeft = new ArrayList<>();
        vecRight = new ArrayList<>();
        //如果边分类错误，下边就是错误的！！！
        switch (fpType) {
            case _LL:
                center.pushArcPtsTo(0, vecUp);
                right.pushArcPtsTo(0, vecUp);
                left.pushArcPtsTo(1, vecDown);
                center.pushArcPtsTo(2, vecLeft);
                left.pushArcPtsTo(2, vecLeft);
                right.pushArcPtsTo(3, vecRight);

                arcUp.setHas_two(true);
                arcDown.setHas_two(false);
                arcLeft.setHas_two(true);
                arcRight.setHas_two(false);
                break;
            case _ML:
                left.pushArcPtsTo(0, vecUp);
                center.pushArcPtsTo(0, vecUp);
                right.pushArcPtsTo(1, vecDown);
                left.pushArcPtsTo(2, vecLeft);
                center.pushArcPtsTo(3, vecRight);
                right.pushArcPtsTo(3, vecRight);

                arcUp.setHas_two(true);
                arcDown.setHas_two(false);
                arcLeft.setHas_two(false);
                arcRight.setHas_two(true);
                break;
            case _MM:
                left.pushArcPtsTo(0, vecUp);
                right.pushArcPtsTo(1, vecDown);
                center.pushArcPtsTo(1, vecDown);
                right.pushArcPtsTo(2, vecLeft);
                left.pushArcPtsTo(3, vecRight);
                center.pushArcPtsTo(3, vecRight);

                arcUp.setHas_two(false);
                arcDown.setHas_two(true);
                arcLeft.setHas_two(false);
                arcRight.setHas_two(true);
                break;
            case _LM:
                right.pushArcPtsTo(0, vecUp);
                center.pushArcPtsTo(1, vecDown);
                left.pushArcPtsTo(1, vecDown);
                right.pushArcPtsTo(2, vecLeft);
                center.pushArcPtsTo(2, vecLeft);
                left.pushArcPtsTo(3, vecRight);

                arcUp.setHas_two(false);
                arcDown.setHas_two(true);
                arcLeft.setHas_two(true);
                arcRight.setHas_two(false);
                break;
        }
        if (!arcUp.setPts(vecUp, false, true))
            return false;
        if (!arcDown.setPts(vecDown, false, true))
            return false;
        if (!arcLeft.setPts(vecLeft, false, false))
            return false;
        if (!arcRight.setPts(vecRight, false, false))
            return false;

        RotatedRect ur, dr, lr, rr;
        MatOfPoint2f urm, drm, lrm, rrm;
        urm = new MatOfPoint2f();
        urm.fromList(arcUp.getPts());
        drm = new MatOfPoint2f();
        drm.fromList(arcDown.getPts());
        lrm = new MatOfPoint2f();
        lrm.fromList(arcLeft.getPts());
        rrm = new MatOfPoint2f();
        rrm.fromList(arcRight.getPts());
        ur = Imgproc.minAreaRect(urm);
        dr = Imgproc.minAreaRect(drm);
        lr = Imgproc.minAreaRect(lrm);
        rr = Imgproc.minAreaRect(rrm);
        double up_down_s, left_right_s;
        up_down_s = Math.min(ur.size.height, ur.size.width)+
                Math.min(dr.size.height, dr.size.width);
        left_right_s = Math.min(lr.size.height, lr.size.width)+
                Math.min(rr.size.height, rr.size.width);
        //这里第一次就是需要矫正的！
//        Point recPt = new Point();
//        recPt.x = vecCL.x + vecCR.x;
//        recPt.y = vecCL.y + vecCR.y;
//        Log.i(TAG, String.valueOf(up_down_s));
//        Log.i(TAG, String.valueOf(left_right_s));
//        if (up_down_s < 5 && left_right_s < 5) {
//            pbType = parabola_type._parabola_null;
//            resPara[0] = arcUp.lineFittingY();
//            resPara[1] = arcDown.lineFittingY();
//            resPara[2] = arcLeft.lineFittingX();
//            resPara[3] = arcRight.lineFittingX();
//        }else
        if (up_down_s >= left_right_s) {
            pbType = parabola_type._parabola_up_down;
            try {
                resPara[0] = arcUp.parabolaFittingY(false, new Point());
                resPara[1] = arcDown.parabolaFittingY(false, new Point());
                resPara[2] = arcLeft.lineFittingX();
                resPara[3] = arcRight.lineFittingX();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Matrix Operations Error");
                return false;
            }
//            switch (fpType) {
//                case _LL:
//                    resPara[0] = arcUp.parabolaFittingY(false, new Point());
//                    resPara[1] = arcDown.parabolaFittingY(true, recPt);
//                    break;
//                case _ML:
//                    resPara[0] = arcUp.parabolaFittingY(false, new Point());
//                    resPara[1] = arcDown.parabolaFittingY(true, recPt);
////                    Log.i(TAG, String.valueOf(resPara[1].getA()));
//                    break;
//                case _MM:
//                    resPara[0] = arcUp.parabolaFittingY(true, recPt);
//                    resPara[1] = arcDown.parabolaFittingY(false, new Point());
//                    break;
//                case _LM:
//                    resPara[0] = arcUp.parabolaFittingY(true, recPt);
//                    resPara[1] = arcDown.parabolaFittingY(false, new Point());
////                    Log.i(TAG, String.valueOf(resPara[0].getA()));
//                    break;
//            }
        } else {
            pbType = parabola_type._parabola_left_right;
            try {
                resPara[0] = arcUp.lineFittingY();
                resPara[1] = arcDown.lineFittingY();
                resPara[2] = arcLeft.parabolaFittingX(false, new Point());
                resPara[3] = arcRight.parabolaFittingX(false, new Point());
            } catch(Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Matrix Operations Error");
                return false;
            }
//            switch (fpType) {
//                case _LM:
//                    resPara[2] = arcLeft.parabolaFittingX(false, new Point());
//                    resPara[3] = arcRight.parabolaFittingX(true, recPt);
//                    break;
//                case _LL:
//                    resPara[2] = arcLeft.parabolaFittingX(false, new Point());
//                    resPara[3] = arcRight.parabolaFittingX(true, recPt);
////                    Log.i(TAG, String.valueOf(resPara[3].getA()));
//                    break;
//                case _ML:
//                    resPara[2] = arcLeft.parabolaFittingX(true, recPt);
//                    resPara[3] = arcRight.parabolaFittingX(false, new Point());
//                    break;
//                case _MM:
//                    resPara[2] = arcLeft.parabolaFittingX(true, recPt);
//                    resPara[3] = arcRight.parabolaFittingX(false, new Point());
////                    Log.i(TAG, String.valueOf(resPara[2].getA()));
//                    break;
//            }
        }
        switch (fpType) {
            case _LL:
                updownLR[0] = arcUp.getStartEndXY(0);
                updownLR[1] = arcUp.getStartEndXY(1);
                updownLR[2] = arcDown.getStartEndXY(0);
                switch (pbType) {
                    case _parabola_up_down:
                    case _parabola_null:
                        updownLR[3] = calParabolaY2LineX(resPara[1], resPara[3]);
                        rectifyParaIndex = 1;
                        rsI = 2;
                        is_rectifyY = true;
                        break;
                    case _parabola_left_right:
                        updownLR[3] = calParabolaX2LineY(resPara[3], resPara[1]);
                        rectifyParaIndex = 3;
                        rsI = 1;
                        is_rectifyY = false;
                        break;
                }
                if (!checkBounds(updownLR[3])) {
                    Log.e(TAG, "updownLR[3] out-of-bounds: "
                            + String.valueOf(updownLR[3].x) + " ," +
                            String.valueOf(updownLR[3].y));
                }
                reI = 3;
                break;
            case _ML:
                updownLR[0] = arcUp.getStartEndXY(0);
                updownLR[1] = arcUp.getStartEndXY(1);
                updownLR[3] = arcDown.getStartEndXY(1);
                switch (pbType) {
                    case _parabola_up_down:
                    case _parabola_null:
                        updownLR[2] = calParabolaY2LineX(resPara[1], resPara[2]);
                        rectifyParaIndex = 1;
                        rsI = 3;
                        is_rectifyY = true;
                        break;
                    case _parabola_left_right:
                        updownLR[2] = calParabolaX2LineY(resPara[2], resPara[1]);
                        rectifyParaIndex = 2;
                        rsI = 0;
                        is_rectifyY = false;
                        break;
                }
                if (!checkBounds(updownLR[2])) {
                    Log.e(TAG, "updownLR[2] out-of-bounds: "
                            + String.valueOf(updownLR[2].x) + " ," +
                            String.valueOf(updownLR[2].y));
                    return false;
                }
                reI = 2;
                break;
            case _MM:
                updownLR[1] = arcUp.getStartEndXY(1);
                updownLR[2] = arcDown.getStartEndXY(0);
                updownLR[3] = arcDown.getStartEndXY(1);
                switch (pbType) {
                    case _parabola_up_down:
                    case _parabola_null:
                        updownLR[0] = calParabolaY2LineX(resPara[0], resPara[2]);
                        rectifyParaIndex = 0;
                        rsI = 1;
                        is_rectifyY = true;
                        break;
                    case _parabola_left_right:
                        updownLR[0] = calParabolaX2LineY(resPara[2], resPara[0]);
                        rectifyParaIndex = 2;
                        rsI = 2;
                        is_rectifyY = false;
                        break;
                }
                if (!checkBounds(updownLR[0])) {
                    Log.e(TAG, "updownLR[0] out-of-bounds: "
                            + String.valueOf(updownLR[0].x) + " ," +
                            String.valueOf(updownLR[0].y));
                    return false;
                }
                reI = 0;
                break;
            case _LM:
                updownLR[0] = arcUp.getStartEndXY(0);
                updownLR[2] = arcDown.getStartEndXY(0);
                updownLR[3] = arcDown.getStartEndXY(1);
                switch (pbType) {
                    case _parabola_up_down:
                    case _parabola_null:
                        updownLR[1] = calParabolaY2LineX(resPara[0], resPara[3]);
                        rectifyParaIndex = 0;
                        rsI = 0;
                        is_rectifyY = true;
                        break;
                    case _parabola_left_right:
                        updownLR[1] = calParabolaX2LineY(resPara[3], resPara[0]);
                        rectifyParaIndex = 3;
                        rsI = 3;
                        is_rectifyY = false;
                        break;
                }
                if (!checkBounds(updownLR[1])) {
                    Log.e(TAG, "updownLR[1] out-of-bounds: "
                            + String.valueOf(updownLR[1].x) + " ," +
                            String.valueOf(updownLR[1].y));
                    return false;
                }
                reI = 1;
                break;
        }
        return true;
    }

    private boolean checkBounds(Point pt) {
        return !(pt.x < 0 || pt.y < 0 || pt.x >= COLS || pt.y >= ROWS);
    }

    private Arc getArcs(int i) {
        int ti = i%4;
        switch (ti) {
            case 0:
                return arcUp;
            case 1:
                return arcDown;
            case 2:
                return arcLeft;
            default:
                return arcRight;
        }
    }

    public static double RATIO_LOW = 6;
    public static double RATIO_HIGH = 12;
    public static int RATIO_STEP = 1;
    public boolean rectify4Corners(Mat srcImg) {
        int count = 0;
        double ratio = calRatio(srcImg);
        if (ratio >= RATIO_LOW && ratio <= RATIO_HIGH) {
            if (is_rectifyY) {
                updownLR[0] = calParabolaY2LineX(resPara[0], resPara[2]);
                updownLR[1] = calParabolaY2LineX(resPara[0], resPara[3]);
                updownLR[2] = calParabolaY2LineX(resPara[1], resPara[2]);
                updownLR[3] = calParabolaY2LineX(resPara[1], resPara[3]);
            } else {
                updownLR[0] = calParabolaX2LineY(resPara[2], resPara[0]);
                updownLR[1] = calParabolaX2LineY(resPara[3], resPara[0]);
                updownLR[2] = calParabolaX2LineY(resPara[2], resPara[1]);
                updownLR[3] = calParabolaX2LineY(resPara[3], resPara[1]);
            }
            return true;
        }
        Point rp = updownLR[reI];
        Point rp1;
        Vec3d para;
        int step=10;
        if (ratio < RATIO_LOW) {
            step = RATIO_STEP;
        } else if (ratio > RATIO_HIGH) {
            step = -RATIO_STEP;
        }
        rp1 = getNP(step, rp);
        if (is_rectifyY) {
            //说明是Y
            try {
                para = getArcs(rectifyParaIndex).parabolaFittingY(true, rp1);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Matrix Operations Error");
                return false;
            }
            ratio = calImgBlackPtsRatio(srcImg, para, updownLR[rsI], rp1, true);
            if (step > 0)
                while (ratio < RATIO_LOW) {
                    rp = rp1;
                    rp1 = getNP(step, rp);
                    try {
                        para = getArcs(rectifyParaIndex).parabolaFittingY(true, rp1);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "Matrix Operations Error");
                        return false;
                    }
                    ratio = calImgBlackPtsRatio(srcImg, para, updownLR[rsI], rp1, true);
                    ++count;
                    if (count > 200)
                        break;
                }
            else
                while (ratio > RATIO_HIGH) {
                    rp = rp1;
                    rp1 = getNP(step, rp);
                    try {
                        para = getArcs(rectifyParaIndex).parabolaFittingY(true, rp1);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "Matrix Operations Error");
                        return false;
                    }
                    ratio = calImgBlackPtsRatio(srcImg, para, updownLR[rsI], rp1, true);
                    ++count;
                    if (count > 200)
                        break;
                }
        } else {
            try {
                para = getArcs(rectifyParaIndex).parabolaFittingX(true, rp1);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Matrix Operations Error");
                return false;
            }
            ratio = calImgBlackPtsRatio(srcImg, para, updownLR[rsI], rp1, false);
            if (step > 0)
                while (ratio < RATIO_LOW) {
                    rp = rp1;
                    rp1 = getNP(step, rp);
                    try {
                        para = getArcs(rectifyParaIndex).parabolaFittingX(true, rp1);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "Matrix Operations Error");
                        return false;
                    }
                    ratio = calImgBlackPtsRatio(srcImg, para, updownLR[rsI], rp1, false);
                    ++count;
                    if (count > 200)
                        break;
                }
            else
                while (ratio > RATIO_HIGH) {
                    rp = rp1;
                    rp1 = getNP(step, rp);
                    try {
                        para = getArcs(rectifyParaIndex).parabolaFittingX(true, rp1);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "Matrix Operations Error");
                        return false;
                    }
                    ratio = calImgBlackPtsRatio(srcImg, para, updownLR[rsI], rp1, false);
                    ++count;
                    if (count > 200)
                        break;
                }
        }
        updownLR[reI] = rp1;
        resPara[rectifyParaIndex] = para;
        if (count <= 200)
            if (is_rectifyY) {
                updownLR[0] = calParabolaY2LineX(resPara[0], resPara[2]);
                updownLR[1] = calParabolaY2LineX(resPara[0], resPara[3]);
                updownLR[2] = calParabolaY2LineX(resPara[1], resPara[2]);
                updownLR[3] = calParabolaY2LineX(resPara[1], resPara[3]);
            } else {
                updownLR[0] = calParabolaX2LineY(resPara[2], resPara[0]);
                updownLR[1] = calParabolaX2LineY(resPara[3], resPara[0]);
                updownLR[2] = calParabolaX2LineY(resPara[2], resPara[1]);
                updownLR[3] = calParabolaX2LineY(resPara[3], resPara[1]);
            }
        return true;
    }

    public void drawArcs(Mat drawPic) {
        Imgproc.circle(drawPic, updownLR[0], 3, new Scalar(0, 0, 255), -1);
        Imgproc.circle(drawPic, updownLR[1], 3, new Scalar(0, 255, 0), -1);
        Imgproc.circle(drawPic, updownLR[2], 3, new Scalar(255, 0, 0), -1);
        Imgproc.circle(drawPic, updownLR[3], 3, new Scalar(255, 255, 0), -1);

        arcUp.drawParabola(drawPic, resPara[0], new Scalar(255, 0, 0), new Size(COLS, ROWS),
                true, updownLR[0], updownLR[1]);
        arcDown.drawParabola(drawPic, resPara[1], new Scalar(255, 0, 0), new Size(COLS, ROWS),
                true, updownLR[2], updownLR[3]);
        arcLeft.drawParabola(drawPic, resPara[2], new Scalar(255, 0, 0), new Size(COLS, ROWS),
                false, updownLR[0], updownLR[2]);
        arcRight.drawParabola(drawPic, resPara[3], new Scalar(255, 0, 0), new Size(COLS, ROWS),
                false, updownLR[1], updownLR[3]);
    }

    public void drawWhiteArcs(Mat drawPic) {
//        Imgproc.circle(drawPic, updownLR[0], 3, new Scalar(255, 255, 255), -1);
//        Imgproc.circle(drawPic, updownLR[1], 3, new Scalar(255, 255, 255), -1);
//        Imgproc.circle(drawPic, updownLR[2], 3, new Scalar(255, 255, 255), -1);
//        Imgproc.circle(drawPic, updownLR[3], 3, new Scalar(255, 255, 255), -1);

        arcUp.drawParabola(drawPic, resPara[0], new Scalar(255, 255, 255), new Size(COLS, ROWS),
                true, updownLR[0], updownLR[1]);
        arcDown.drawParabola(drawPic, resPara[1], new Scalar(255, 255, 255), new Size(COLS, ROWS),
                true, updownLR[2], updownLR[3]);
        arcLeft.drawParabola(drawPic, resPara[2], new Scalar(255, 255, 255), new Size(COLS, ROWS),
                false, updownLR[0], updownLR[2]);
        arcRight.drawParabola(drawPic, resPara[3], new Scalar(255, 255, 255), new Size(COLS, ROWS),
                false, updownLR[1], updownLR[3]);
    }

    public void drawAllFps(Mat drawPic) {
        left.drawFp(drawPic);
        center.drawFp(drawPic);
        right.drawFp(drawPic);
    }

    public int getSizeOfBox() {
        List<Point> tlist = new ArrayList<>();
        tlist.add(updownLR[0]);
        tlist.add(updownLR[1]);
        tlist.add(updownLR[2]);
        tlist.add(updownLR[3]);
        MatOfPoint mop = new MatOfPoint();
        mop.fromList(tlist);
        Rect rect = Imgproc.boundingRect(mop);
        return Math.max(rect.height, rect.width)*2;
    }

    public Point get4PtsForPerspectiveTransform(int i) {
        int ti = i%4;
        if (!is_rectifyY) {
            switch (ti) {
                case 0:
                    ti = 2;
                    break;
                case 1:
                    ti = 0;
                    break;
                case 2:
                    ti = 3;
                    break;
                case 3:
                    ti = 1;
                    break;
            }
        }
        return updownLR[ti];
    }

    public Point calParabolaX2LineY(Vec3d para, Vec3d line) {
        Point res = new Point(-1, -1);
        if (Math.abs(line.getB()) <= 0.00000001) {
            res.y = line.getC();
            res.x = para.getA()*res.y*res.y + para.getB()*res.y + para.getC();
        } else {
            double b2 = 1.0/line.getB();
            double c2 = -line.getC()/line.getB();
            double a = para.getA();
            double b = para.getB() - b2;
            double c = para.getC() - c2;
            double delta = b*b - 4*a*c;
            if (delta < 0)
                return res;
            delta = Math.sqrt(delta);
            double y = (-b+delta)/(2*a);
            double x = para.getA()*y*y + para.getB()*y + para.getC();
            if (y > 0 && y < ROWS && x >0 && x < COLS) {
                res.x = x;
                res.y = y;
            } else {
                y = (-b-delta)/(2*a);
                x = para.getA()*y*y + para.getB()*y + para.getC();
                res.x = x;
                res.y = y;
            }
        }
        return res;
    }

    public Point calParabolaY2LineX(Vec3d para, Vec3d line) {
        Point res = new Point(-1, -1);
        if (Math.abs(line.getB()) <= 0.00000001) {
            res.x = line.getC();
            res.y = para.getA()*res.x*res.x + para.getB()*res.x + para.getC();
        } else {
            double b2 = 1.0/line.getB();
            double c2 = -line.getC()/line.getB();
            double a = para.getA();
            double b = para.getB() - b2;
            double c = para.getC() - c2;
            double delta = b*b - 4*a*c;
            if (delta < 0)
                return res;
            delta = Math.sqrt(delta);
            double x = (-b+delta)/(2*a);
            double y = para.getA()*x*x + para.getB()*x + para.getC();
            if (y > 0 && y < ROWS && x >0 && x < COLS) {
                res.x = x;
                res.y = y;
            } else {
                x = (-b-delta)/(2*a);
                y = para.getA()*x*x + para.getB()*x + para.getC();
                res.x = x;
                res.y = y;
            }
        }
        return res;
    }
    private static double black_thresh = 50;
    private double calImgBlackPtsRatio(Mat srcImg, Vec3d para, Point sp, Point ep, boolean is_Y) {
        /*
        *
        * */
        if (sp.x < 0 || sp.y < 0
                || ep.y < 0 || ep.x < 0
                || sp.x > COLS || sp.y > ROWS
                || ep.x > COLS || ep.y > ROWS) {
            return 200;
        }
        double[] data;
        Point ssp = sp;
        Point eep = ep;
        double x, y;
        int lessPix=0, allpts = 0;
        if (is_Y) {
            if (ssp.x > eep.x) {
                Point tmp = eep;
                eep = ssp;
                ssp = tmp;
            }
            for (x = ssp.x; x <= eep.x; ++x) {
                y = para.getA()*x*x + para.getB()*x + para.getC();
                data = srcImg.get((int)y, (int)x);
                if (data[0] < black_thresh)
                    ++lessPix;
                ++allpts;
            }
        } else {
            if (sp.y > ep.y) {
                Point tmp = ep;
                ep = sp;
                sp = tmp;
            }
            for (y = sp.y; y <= ep.y; ++y) {
                x = para.getA()*y*y + para.getB()*y +para.getC();
                data = srcImg.get((int)y, (int)x);
                if (data[0] < black_thresh)
                    ++lessPix;
                ++allpts;
            }
        }
        return ((double)(allpts-lessPix))/((double)(lessPix));
    }

    private double calRatio(Mat srcImg) {
        double ratio = 0;
        switch (fpType) {
            case _LL:
                if (pbType == parabola_type._parabola_up_down)
                    ratio = calImgBlackPtsRatio(srcImg, resPara[1], updownLR[2], updownLR[3], true);
                else
                    ratio = calImgBlackPtsRatio(srcImg, resPara[3], updownLR[1], updownLR[3], false);
                break;
            case _ML:
                if (pbType == parabola_type._parabola_up_down)
                    ratio = calImgBlackPtsRatio(srcImg, resPara[1], updownLR[2], updownLR[3], true);
                else
                    ratio = calImgBlackPtsRatio(srcImg, resPara[2], updownLR[0], updownLR[2], false);
                break;
            case _MM:
                if (pbType == parabola_type._parabola_up_down)
                    ratio = calImgBlackPtsRatio(srcImg, resPara[0], updownLR[0], updownLR[1], true);
                else
                    ratio = calImgBlackPtsRatio(srcImg, resPara[2], updownLR[0], updownLR[2], false);
                break;
            case _LM:
                if (pbType == parabola_type._parabola_up_down)
                    ratio = calImgBlackPtsRatio(srcImg, resPara[0], updownLR[0], updownLR[1], true);
                else
                    ratio = calImgBlackPtsRatio(srcImg, resPara[3], updownLR[1], updownLR[3], false);
                break;
        }
        Log.i(TAG, "Ratio: "+String.valueOf(ratio));
        return ratio;
    }

    private Point getNP(int direction, Point p) {
        Point res = new Point(-1, -1);
        Vec3d para = new Vec3d(0, 0, 0);
        int dir = 0;
        boolean is_Y=true;
        switch (fpType) {
            case _LL:
                switch (pbType) {
                    case _parabola_up_down:
                        para = resPara[3];
                        is_Y = false;
                        dir = 1;
                        break;
                    case _parabola_left_right:
                        para = resPara[1];
                        is_Y = true;
                        dir = 1;
                        break;
                }
                break;
            case _ML:
                switch (pbType) {
                    case _parabola_up_down:
                        para = resPara[2];
                        is_Y = false;
                        dir = 1;
                        break;
                    case _parabola_left_right:
                        para = resPara[1];
                        is_Y = true;
                        dir = -1;
                        break;
                }
                break;
            case _MM:
                switch (pbType) {
                    case _parabola_up_down:
                        para = resPara[2];
                        is_Y = false;
                        dir = -1;
                        break;
                    case _parabola_left_right:
                        para = resPara[0];
                        is_Y = true;
                        dir = -1;
                        break;
                }
                break;
            case _LM:
                switch (pbType) {
                    case _parabola_up_down:
                        para = resPara[3];
                        is_Y = false;
                        dir = -1;
                        break;
                    case _parabola_left_right:
                        para = resPara[0];
                        is_Y = true;
                        dir = 1;
                        break;
                }
                break;
        }
        dir *= direction;
        double x, y;
        if (!is_Y) {
            y = p.y;
            y += dir;
            if (y <= 0)
                y = 1;
            if (y >= ROWS)
                y = ROWS-1;
            x = para.getB()*y + para.getC();
        } else {
            x = p.x;
            x += dir;
            if (x <= 0)
                x = 1;
            if (x >= COLS)
                x = COLS-1;
            y = para.getB()*x + para.getC();
        }
        if (x > 0 && x < COLS && y > 0 && y < ROWS) {
            res.x = x;
            res.y = y;
        }
        return res;
    }

    private FinderPattern left, center, right;


    public Point getLp() {
        return L;
    }

    public Point getCp() {
        return C;
    }

    public Point getRp() {
        return R;
    }

    private Point L;
    private Point C;
    private Point R;
    private Arc arcUp, arcDown, arcLeft, arcRight;
    private finder_pattern_type fpType;
    private parabola_type pbType;
    private Vec3d resPara[] = new Vec3d[4];

    public Point[] getUpdownLR() {
        return updownLR;
    }

    private Point updownLR[] = new Point[4];
    private int rectifyParaIndex=0;
    private int rsI=0, reI=0;
    private boolean is_rectifyY = true;
    private int COLS=-1, ROWS=-1;

}

enum finder_pattern_type{_LL, _ML, _MM, _LM}
enum parabola_type{_parabola_up_down, _parabola_left_right, _parabola_null}

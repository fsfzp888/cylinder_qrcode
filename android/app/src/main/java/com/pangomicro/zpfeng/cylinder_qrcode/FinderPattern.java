package com.pangomicro.zpfeng.cylinder_qrcode;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Jama.Matrix;

/**
 * Created by zczx1 on 2016/4/23.
 */
public class FinderPattern {
    public FinderPattern() {
        up = new Arc();
        down = new Arc();
        left = new Arc();
        right = new Arc();
        center_pts = new Point(-1, -1);
    }

    public boolean setFps(List<Point> pts) {
        if (pts.size() < 8)
            return false;
        corners = new ArrayList<>();
        orig_pts = new ArrayList<>();
        orig_pts.addAll(pts);
        find4Corners(orig_pts, corners);
        if (corners.size() != 4)
            return false;
        return resortPtsAndSetArc();
    }

    public void drawFp(Mat drawPic) {
        Imgproc.circle(drawPic, corners.get(0), 3, new Scalar(255, 0, 0), -1);
        Imgproc.circle(drawPic, corners.get(1), 3, new Scalar(0, 255, 0), -1);
        Imgproc.circle(drawPic, corners.get(2), 3, new Scalar(0, 0, 255), -1);
        Imgproc.circle(drawPic, corners.get(3), 3, new Scalar(255, 255, 0), -1);

        up.drawArc(drawPic, new Scalar(255, 0, 0));
        down.drawArc(drawPic, new Scalar(0, 255, 0));
        left.drawArc(drawPic, new Scalar(0, 0, 255));
        right.drawArc(drawPic, new Scalar(255, 255, 0));
    }

    public void pushArcPtsTo(int arc_num, List<Point> dst) {
        int i = arc_num%4;
        switch (i) {
            case 0:
                up.copyPtsTo(dst);
                break;
            case 1:
                down.copyPtsTo(dst);
                break;
            case 2:
                left.copyPtsTo(dst);
                break;
            case 3:
                right.copyPtsTo(dst);
                break;
        }
    }

    public void swapArcs() {
        Arc t1, t2;
        t1 = up;
        t2 = down;
        up = left;
        down = right;
        left = t1;
        right = t2;
        if (up.getCenter_point().y > down.getCenter_point().y) {
            t1 = up;
            up = down;
            down = t1;
        }
        if (left.getCenter_point().x > right.getCenter_point().x) {
            t1 = left;
            left = right;
            right = t1;
        }
    }

    public boolean isArcsTypeCorrect(Point vecP, boolean isVerticalX) {
        double res1, res2;
        res1 = calAbsCosValue(vecP, vecUp);
        res1 += calAbsCosValue(vecP, vecDown);
        res2 = calAbsCosValue(vecP, vecLeft);
        res2 += calAbsCosValue(vecP, vecRight);
        if (isVerticalX) {
            if (res2 <= res1)
                return false;
        } else {
            if (res2 >= res1)
                return false;
        }
        return true;
    }

    private double calAbsCosValue(Point vec1, Point vec2) {
        double res;
        res = (vec1.x*vec2.x + vec1.y*vec2.y)/(
                Math.sqrt(vec1.x*vec1.x + vec1.y*vec1.y)*Math.sqrt(vec2.x*vec2.x + vec2.y*vec2.y));
        return Math.abs(res);
    }

    public Point getCenterPoint() {
        return center_pts;
    }

    private boolean resortPtsAndSetArc() {
        Collections.sort(corners, new sortLessY());
        List<Point> upPts, downPts;
        upPts = new ArrayList<>();
        downPts = new ArrayList<>();
        upPts.add(corners.get(0));
        upPts.add(corners.get(1));
        downPts.add(corners.get(2));
        downPts.add(corners.get(3));
        Collections.sort(upPts, new sortLessX());
        Collections.sort(downPts, new sortLessX());
        corners.clear();
        //按照up_left, up_right, down_right, down_left顺序存储角点
        corners.add(upPts.get(0));
        corners.add(upPts.get(1));
        corners.add(downPts.get(1));
        corners.add(downPts.get(0));

        vecUp = new Point(-1, -1);
        vecDown = new Point(-1, -1);
        vecLeft = new Point(-1, -1);
        vecRight = new Point(-1, -1);
        vecUp.x = corners.get(1).x - corners.get(0).x;
        vecUp.y = corners.get(1).y - corners.get(0).y;
        vecDown.x = corners.get(2).x - corners.get(3).x;
        vecDown.y = corners.get(2).y - corners.get(3).y;
        vecLeft.x = corners.get(3).x - corners.get(0).x;
        vecLeft.y = corners.get(3).y - corners.get(0).y;
        vecRight.x = corners.get(1).x - corners.get(2).x;
        vecRight.y = corners.get(1).y - corners.get(2).y;

        Vec3d line1, line2, center_vec3d;
        double a, b, c;
        a = corners.get(0).y - corners.get(2).y;
        b = corners.get(2).x - corners.get(0).x;
        c = corners.get(0).x*corners.get(2).y - corners.get(0).y*corners.get(2).x;
        line1 = new Vec3d(a, b, c);
        a = corners.get(1).y - corners.get(3).y;
        b = corners.get(3).x - corners.get(1).x;
        c = corners.get(1).x*corners.get(3).y - corners.get(1).y*corners.get(3).x;
        line2 = new Vec3d(a, b, c);
        a = line1.getB()*line2.getC() - line2.getB()*line1.getC();
        b = line2.getA()*line1.getC() - line1.getA()*line2.getC();
        c = line1.getA()*line2.getB() - line2.getA()*line1.getB();
        center_vec3d = new Vec3d(a, b, c);
        Point center = new Point(-1, -1);
        center.x = center_vec3d.getA()/center_vec3d.getC();
        center.y = center_vec3d.getB()/center_vec3d.getC();
        center_pts.x = center.x;
        center_pts.y = center.y;
        Point vecX, vecY, vecP;
        vecX = new Point(-1, -1);
        vecY = new Point(-1, -1);
        vecP = new Point(-1, -1);
        vecX.x = upPts.get(0).x - center.x;
        vecX.y = upPts.get(0).y - center.y;
        vecY.x = upPts.get(1).x - center.x;
        vecY.y = upPts.get(1).y - center.y;
        List<Point> UP, DOWN, LEFT, RIGHT;
        UP = new ArrayList<>();
        DOWN = new ArrayList<>();
        LEFT = new ArrayList<>();
        RIGHT = new ArrayList<>();
        for (int i = 0; i < orig_pts.size(); ++i) {
            vecP.x = orig_pts.get(i).x - center.x;
            vecP.y = orig_pts.get(i).y - center.y;
            Point vecRes = calVecPara(vecX, vecY, vecP);
            if (vecRes.x > 0 && vecRes.y > 0)
                UP.add(orig_pts.get(i));
            else if (vecRes.x > 0 && vecRes.y < 0)
                LEFT.add(orig_pts.get(i));
            else if (vecRes.x < 0 && vecRes.y > 0)
                RIGHT.add(orig_pts.get(i));
            else if (vecRes.x < 0 && vecRes.y < 0)
                DOWN.add(orig_pts.get(i));
        }

        if (!up.setPts(UP, true, true))
            return false;
        if (!down.setPts(DOWN, true, true))
            return false;
        if (!left.setPts(LEFT, true, false))
            return false;
        if (!right.setPts(RIGHT, true, false))
            return false;
        return true;
    }

    private void find4Corners(List<Point> src_pts, List<Point> dst_pts) {
        MatOfPoint mop;
        mop = new MatOfPoint();
        mop.fromList(src_pts);
        Rect rect = Imgproc.boundingRect(mop);
        Point basePt = rect.tl();
        int width = rect.width + rect.width/2;
        int height = rect.height + rect.height/2;
        basePt.x -= rect.width/4;
        basePt.y -= rect.height/4;
        if (basePt.x < 0)
            basePt.x = 0;
        if (basePt.y < 0)
            basePt.y = 0;
        Mat tmp = new Mat(height, width, CvType.CV_8UC1, new Scalar(0));
        Point p1, p2;
        for (int i = 0; i < src_pts.size(); ++i) {
            p1 = src_pts.get(i).clone();
            p2 = src_pts.get((i+1)%src_pts.size()).clone();
            p1.x -= basePt.x;
            p1.y -= basePt.y;
            p2.x -= basePt.x;
            p2.y -= basePt.y;
            Imgproc.line(tmp, p1, p2, new Scalar(255));
        }
        double qualityLevel = 0.01;
        double minDistance = 8;
        int blockSize = 9;
        double k = 0.04;
        mop  = new MatOfPoint();
        Imgproc.goodFeaturesToTrack(tmp, mop,
                4, qualityLevel, minDistance,
                new Mat(), blockSize, false, k);
        Point[] pts = mop.toArray();
        for (int i = 0; i < 4; ++i) {
            Point tp = new Point(-1, -1);
            tp.x = pts[i].x;
            tp.y = pts[i].y;
            tp.x += basePt.x;
            tp.y += basePt.y;
            dst_pts.add(tp);
        }

    }

    private Point calVecPara(Point vecX, Point vecY, Point vecP) {
        Point res = new Point(-1, -1);
        Matrix A, x, b;
        A = new Matrix(2, 2);
        A.set(0, 0, vecX.x);
        A.set(1, 0, vecX.y);
        A.set(0, 1, vecY.x);
        A.set(1, 1, vecY.y);
        b = new Matrix(2, 1);
        b.set(0, 0, vecP.x);
        b.set(1, 0, vecP.y);
        x = A.solve(b);
        res.x = x.get(0, 0);
        res.y = x.get(1, 0);
        return res;
    }

    private Arc up, down, left, right;
    private List<Point> orig_pts;
    private List<Point> corners;
    private Point center_pts;
    Point vecUp, vecDown, vecLeft, vecRight;
}

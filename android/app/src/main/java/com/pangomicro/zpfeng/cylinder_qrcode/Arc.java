package com.pangomicro.zpfeng.cylinder_qrcode;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;
import java.util.Set;

import Jama.*;
/**
 * Created by zczx1 on 2016/4/11.
 */
public class Arc {
    public Arc() {
        pts = new ArrayList<>();
    }

    public boolean setPts(List<Point> srcPts, boolean is_filled_between, boolean sort_X_or_Y) {
        if (srcPts.size() < 2)
            return false;
        if (sort_X_or_Y)
            Collections.sort(srcPts, new sortLessX());
        else
            Collections.sort(srcPts, new sortLessY());
        pts = new ArrayList<>();
        if (is_filled_between) {
            if (srcPts.size() == 1)
                pts.add(srcPts.get(0));
            else
                for (int i = 0; i < srcPts.size()-1; ++i) {
                    insertBetween(pts, srcPts.get(i), srcPts.get(i+1));
                }
        } else {
            pts.addAll(srcPts);
        }
        if (sort_X_or_Y) {
            startX = pts.get(0);
            endX = pts.get(pts.size()-1);
            double ys = pts.get(0).y, ye = pts.get(0).y;
            int ysi = 0, yei = 0;
            for (int i = 1; i < pts.size(); ++i) {
                if (ys > pts.get(i).y) {
                    ysi = i;
                    ys = pts.get(i).y;
                }
                if (ye < pts.get(i).y) {
                    yei = i;
                    ye = pts.get(i).y;
                }
            }
            startY = pts.get(ysi);
            endY = pts.get(yei);
        } else {
            startY = pts.get(0);
            endY = pts.get(pts.size()-1);
            double xs = pts.get(0).x, xe = pts.get(0).x;
            int xsi = 0, xei = 0;
            for (int i = 1; i < pts.size(); ++i) {
                if (xs > pts.get(i).x) {
                    xsi = i;
                    xs = pts.get(i).x;
                }
                if (xe < pts.get(i).x) {
                    xei = i;
                    xe = pts.get(i).x;
                }
            }
            startX = pts.get(xsi);
            endX = pts.get(xei);
        }
        center_point = new Point(0, 0);
        center_point.x += startX.x;
        center_point.x += endX.x;
        center_point.x += startY.x;
        center_point.x += endY.x;
        center_point.y += startX.y;
        center_point.y += endX.y;
        center_point.y += startY.y;
        center_point.y += endY.y;
        center_point.x /= 4;
        center_point.y /= 4;
        return true;
    }

    private void insertBetween(List<Point> insertPts, Point pt1, Point pt2) {
//        if (pt1 == pt2) {
//            insertPts.add(pt1);
//            return;
//        }
        Set<Point> hashSet = new HashSet<>();
        List<Point> newList = new ArrayList<>();
        if (hashSet.add(pt1))
            newList.add(pt1);
        Point tmp = new Point(-1, -1);
        Point ntmp;
        if (pt1.x == pt2.x) {
            tmp.x = pt1.x;
            if (pt1.y > pt2.y) {
                for (int i = (int)(pt1.y-1); i > pt2.y; --i) {
                    tmp.y = i;
                    ntmp = new Point(tmp.x, tmp.y);
                    if (hashSet.add(ntmp))
                        newList.add(ntmp);
                }
            } else if (pt1.y < pt2.y) {
                for (int i = (int)(pt1.y+1); i < pt2.y; ++i) {
                    tmp.y = i;
                    ntmp = new Point(tmp.x, tmp.y);
                    if (hashSet.add(ntmp))
                        newList.add(ntmp);
                }
            }
        } else {
            double a = (pt1.y-pt2.y)/(pt1.x-pt2.x);
            double b = pt1.y-pt1.x*a;
            if (pt1.x > pt2.x) {
                for (int i = (int)(pt1.x-1); i > pt2.x; --i) {
                    tmp.x = i;
                    tmp.y = a*i+b;
                    ntmp = new Point(tmp.x, tmp.y);
                    if (hashSet.add(ntmp))
                        newList.add(ntmp);
                }
            } else if (pt1.x < pt2.x) {
                for (int i = (int)(pt1.x+1); i < pt2.x; ++i) {
                    tmp.x = i;
                    tmp.y = a*i+b;
                    ntmp = new Point(tmp.x, tmp.y);
                    if (hashSet.add(ntmp))
                        newList.add(ntmp);
                }
            }
        }
        if (hashSet.add(pt2))
            newList.add(pt2);
        for (int i = 0; i < newList.size(); ++i)
            insertPts.add(newList.get(i));
    }

    public void copyPtsTo(List<Point> dst) {
        if (pts.isEmpty())
            return;
        for (int i = 0; i < pts.size(); ++i)
            dst.add(pts.get(i));
    }

    public List<Point> getPts() {
        return pts;
    }

    public void drawArc(Mat drawPic, Scalar color) {
        drawAllPts(drawPic, color);
    }

    public void drawAllPts(Mat drawPic, Scalar color) {
        for (int i = 0; i < pts.size(); ++i) {
            Imgproc.circle(drawPic, pts.get(i), 2, color);
        }
    }

    public void drawParabola(Mat drawPic, Vec3d para, Scalar color, Size s,
                             boolean is_Y, Point sp, Point ep) {
        Point p1, p2, s1, s2;
        p1 = new Point(-1, -1);
        p2 = new Point(-1, -1);
        s1 = sp.clone();
        s2 = ep.clone();
        if (is_Y) {
            if (s1.x > s2.x) {
                Point tmp;
                tmp = s1;
                s1 = s2;
                s2 = tmp;
            }
            p1.x = s1.x;
            p1.y = para.getA()*s1.x*s1.x+para.getB()*s1.x+para.getC();
            double ty;
            for (double x = p1.x; x <= s2.x; ++x) {
                ty = para.getA()*x*x+para.getB()*x+para.getC();
                p2.x = x;
                p2.y = ty;
                if (ty >= 0 && ty < s.height && p1.y >=0 && p1.y < s.height) {
                    Imgproc.line(drawPic, p1, p2, color, 3);
                }
                p1.x = p2.x;
                p1.y = p2.y;
            }
        } else {
            if (s1.y > s2.y) {
                Point tmp;
                tmp = s1;
                s1 = s2;
                s2 = tmp;
            }
            p1.y = s1.y;
            p1.x = para.getA()*s1.y*s1.y+para.getB()*s1.y+para.getC();
            double tx;
            for (double y = p1.y; y <= s2.y; ++y) {
                tx = para.getA()*y*y+para.getB()*y+para.getC();
                p2.x = tx;
                p2.y = y;
                if (tx >= 0 && tx < s.width && p1.x >=0 && p1.x < s.width) {
                    Imgproc.line(drawPic, p1, p2, color, 3);
                }
                p1.x = p2.x;
                p1.y = p2.y;
            }
        }
    }

    public Vec3d lineFittingX() throws Exception{
        return lineFitting(this.pts, false);
    }

    public Vec3d lineFittingY() throws Exception{
        return lineFitting(this.pts, true);
    }

    public Vec3d lineFitting(List<Point> srcPts, boolean is_Y) throws Exception{
        Matrix A, At, b;
        A = new Matrix(srcPts.size(), 2);
        b = new Matrix(srcPts.size(), 1);
        if (is_Y) {
            for (int i = 0; i < srcPts.size(); ++i) {
                A.set(i, 0, srcPts.get(i).x);
                A.set(i, 1, 1);
                b.set(i, 0, srcPts.get(i).y);
            }
        } else {
            for (int i = 0; i < srcPts.size(); ++i) {
                A.set(i, 0, srcPts.get(i).y);
                A.set(i, 1, 1);
                b.set(i, 0, srcPts.get(i).x);
            }
        }
        At = A.transpose();
        Matrix As = At.times(A);
        Matrix invA;
//        try {
            invA = As.inverse();
//        } catch (RuntimeException e) {
//            invA = new Matrix(2, 2, 0);
//        }
        Matrix res;
        res = invA.times(At);
        res = res.times(b);
        return new Vec3d(0, res.get(0, 0), res.get(1, 0));
    }

    public Vec3d parabolaFittingX(boolean is_Rectify, Point rectifyPoint)  throws Exception{
        return parabolaFitting(this.pts, false, is_Rectify, rectifyPoint);
    }

    public Vec3d parabolaFittingY(boolean is_Rectify, Point rectifyPoint)  throws Exception{
        return parabolaFitting(this.pts, true, is_Rectify, rectifyPoint);
    }

    public Vec3d parabolaFitting(List<Point> srcPts, boolean is_Y,
                                       boolean is_Rectify, Point rectifyPoint)  throws Exception{
        Matrix A, At, b;
        if (is_Rectify) {
            A = new Matrix(srcPts.size()+1, 3);
            b = new Matrix(srcPts.size()+1, 1);
        } else {
            A = new Matrix(srcPts.size(), 3);
            b = new Matrix(srcPts.size(), 1);
        }
        if (is_Y) {
            for (int i = 0; i < srcPts.size(); ++i) {
                A.set(i, 0, (srcPts.get(i).x)*(srcPts.get(i).x));
                A.set(i, 1, srcPts.get(i).x);
                A.set(i, 2, 1);
                b.set(i, 0, srcPts.get(i).y);
            }
            if (is_Rectify) {
                A.set(srcPts.size(), 0, rectifyPoint.x*rectifyPoint.x);
                A.set(srcPts.size(), 1, rectifyPoint.x);
                A.set(srcPts.size(), 2, 1);
                b.set(srcPts.size(), 0, rectifyPoint.y);
            }
        } else {
            for (int i = 0; i < srcPts.size(); ++i) {
                A.set(i, 0, (srcPts.get(i).y)*(srcPts.get(i).y));
                A.set(i, 1, srcPts.get(i).y);
                A.set(i, 2, 1);
                b.set(i, 0, srcPts.get(i).x);
            }
            if (is_Rectify) {
                A.set(srcPts.size(), 0, rectifyPoint.y*rectifyPoint.y);
                A.set(srcPts.size(), 1, rectifyPoint.y);
                A.set(srcPts.size(), 2, 1);
                b.set(srcPts.size(), 0, rectifyPoint.x);
            }
        }
        At = A.transpose();
        Matrix As = At.times(A);
        Matrix invA;
//        try {
            invA = As.inverse();
//        } catch (RuntimeException e) {
//            invA = new Matrix(3, 3, 0);
//        }
        Matrix res;
        res = invA.times(At);
        res = res.times(b);
        return new Vec3d(res.get(0, 0), res.get(1, 0), res.get(2, 0));
    }

    public Point getStartEndXY(int i) {
        int r = i%4;
        Point res = new Point(-1, -1);
        switch (r) {
            case 0:
                res = startX;
                break;
            case 1:
                res = endX;
                break;
            case 2:
                res = startY;
                break;
            case 3:
                res = endY;
                break;
        }
        return res;
    }

    private List<Point> pts;

    public boolean isHas_two() {
        return has_two;
    }

    public void setHas_two(boolean has_two) {
        this.has_two = has_two;
    }

    private boolean has_two;
    private Point startX, endX, startY, endY;

    public Point getCenter_point() {
        return center_point;
    }

    private Point center_point;
}

class sortLessX implements Comparator<Point> {

    @Override
    public int compare(Point lhs, Point rhs) {
        if (lhs.x < rhs.x)
            return -1;
        else if (lhs.x > rhs.x)
            return 1;
        return 0;
    }
}

class sortLessY implements Comparator<Point> {

    @Override
    public int compare(Point lhs, Point rhs) {
        if (lhs.y < rhs.y)
            return -1;
        else if (lhs.y > rhs.y)
            return 1;
        return 0;
    }
}

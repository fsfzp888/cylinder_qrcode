#include "arc.h"
#include <math.h>

const double ePI = 3.14159265358979323846264338327950288419716939937510;

bool qr_cylinder::LessX(const Point &lp, const Point &rp)
{
    return lp.x < rp.x;
}

bool qr_cylinder::LessY(const Point &lp, const Point &rp)
{
    return lp.y < rp.y;
}

qr_cylinder::Arc::Arc()
    :_has_two(false)
{
}

/* sort_X_or_Y
 * true: sort(pts.begin(), pts.end(), lessX);
 * false: sort(pts.begin(), pts.end(), lessY);
 */
void qr_cylinder::Arc::SetPts(vector<Point> & arc_pts, bool is_filled_between, bool sort_x_or_y)
{
    if (arc_pts.empty())
        return;
    if (sort_x_or_y)
        sort(arc_pts.begin(), arc_pts.end(), LessX);
    else
        sort(arc_pts.begin(), arc_pts.end(), LessY);
    if (!_pts.empty())
        _pts.clear();
    if (is_filled_between)
        for (size_t i = 0; i < (arc_pts.size()-1); ++i) {
            InsertBetween(arc_pts[i], arc_pts[i+1]);
        }
    else {
        _pts.assign(arc_pts.begin(), arc_pts.end());
    }
    if (sort_x_or_y) {
        _start_x = _pts[0];
        _end_x = _pts[_pts.size()-1];
        double ys = _pts[0].y, ye = _pts[0].y;
        int ysi = 0, yei = 0;
        for (size_t i = 1; i < _pts.size(); ++i) {
            if (ys > _pts[i].y) {
                ysi = i;
                ys = _pts[i].y;
            }
            if (ye < _pts[i].y) {
                yei = i;
                ye = _pts[i].y;
            }
        }
        _start_y = _pts[ysi];
        _end_y = _pts[yei];
    } else {
        _start_y = _pts[0];
        _end_y = _pts[_pts.size()-1];
        double xs = _pts[0].x, xe = _pts[0].x;
        int xsi = 0, xei = 0;
        for (size_t i = 0; i < _pts.size(); ++i) {
            if (xs > _pts[i].x) {
                xsi = i;
                xs = _pts[i].x;
            }
            if (xe < _pts[i].x) {
                xei = i;
                xe = _pts[i].x;
            }
        }
        _start_x = _pts[xsi];
        _end_x = _pts[xei];
    }
}

const vector<Point>& qr_cylinder::Arc::GetPts()
{
    return _pts;
}

Point qr_cylinder::Arc::GetStartEndXY(int i)
{
    int r = i%4;
    Point res(-1, -1);
    switch (r) {
    case 0:
        res = _start_x;
        break;
    case 1:
        res = _end_x;
        break;
    case 2:
        res = _start_y;
        break;
    case 3:
        res = _end_y;
        break;
    }
    return res;
}

void qr_cylinder::Arc::SetTwo(bool b)
{
    _has_two = b;
}

bool qr_cylinder::Arc::HasTwo()
{
    return _has_two;
}

void qr_cylinder::Arc::DrawAllPts(Mat &src, const Scalar &color)
{
    for (size_t i = 0; i < _pts.size(); ++i) {
        circle(src, _pts[i], 1, color);
    }
}

void qr_cylinder::Arc::InsertBetween(const Point &pt1, const Point &pt2)
{
    _pts.push_back(pt1);

    //push points between them to pts
    Point tmp;
    if (pt1.x == pt2.x) {
        tmp.x = pt1.x;
        if (pt1.y > pt2.y) {
            for (int i = (pt1.y-1); i > pt2.y; --i) {
                tmp.y = i;
                _pts.push_back(tmp);
            }
        } else if (pt1.y < pt2.y) {
            for (int i = (pt1.y+1); i < pt2.y; ++i) {
                tmp.y = i;
                _pts.push_back(tmp);
            }
        }
    } else {
        double a = (pt1.y-pt2.y)/(pt1.x-pt2.x);
        double b = pt1.y-pt1.x*a;
        if (pt1.x > pt2.x) {
            for (int i = (pt1.x-1); i > pt2.x; --i) {
                tmp.x = i;
                tmp.y = a*i+b;
                _pts.push_back(tmp);
            }
        } else if (pt1.x < pt2.x) {
            for (int i = (pt1.x+1); i < pt2.x; ++i) {
                tmp.x = i;
                tmp.y = a*i+b;
                _pts.push_back(tmp);
            }
        }
    }

    _pts.push_back(pt2);
    //删除重复的点
    _pts.erase(unique(_pts.begin(), _pts.end()), _pts.end());
}

void qr_cylinder::Arc::CopyPtsTo(vector<Point> &dst)
{
    if (_pts.empty())
        return;
    for (size_t i = 0; i < _pts.size(); ++i)
        dst.push_back(_pts[i]);
}

void qr_cylinder::Arc::DrawArc(Mat &src, const Scalar &color)
{
    DrawAllPts(src, color);
}

void qr_cylinder::Arc::DrawParabola(Mat &draw_pic, const Vec3d &para, const Scalar &color, const Size& s,
                       bool is_y,
                       const Point &start_point, const Point &stop_point)
{
    Point p1, p2;
    Point s1 = start_point;
    Point s2 = stop_point;
    if (is_y) {
        if (s1.x > s2.x) {
            Point tmp = s1;
            s1 = s2;
            s2 = tmp;
        }
        p1.x = s1.x;
        p1.y = para[0]*p1.x*p1.x + para[1]*p1.x + para[2];
        double ty;
        for (int x = s1.x; x <= s2.x; ++x) {
            ty = para[0]*x*x + para[1]*x + para[2];
            p2.x = x;
            p2.y = ty;
            if (ty >= 0 && ty < s.height && p1.y >=0 && p1.y < s.height) {
                line(draw_pic, p1, p2, color);
            }
            p1.x = p2.x;
            p1.y = p2.y;
        }
    } else {
        if (s1.y > s2.y) {
            Point tmp = s1;
            s1 = s2;
            s2 = tmp;
        }
        p1.y = s1.y;
        p1.x = para[0]*p1.y*p1.y + para[1]*p1.y + para[2];
        double tx;
        for (int y = s1.y; y <= s2.y; ++y) {
            tx = para[0]*y*y + para[1]*y + para[2];
            p2.x = tx;
            p2.y = y;
            if (tx >= 0 && tx < s.width && p1.x >=0 && p1.x < s.width) {
                line(draw_pic, p1, p2, color);
            }
            p1.x = p2.x;
            p1.y = p2.y;
        }
    }

}

Vec3d qr_cylinder::Arc::ParabolaFittingY(bool is_rectify, Point rectify_point)
{
    return ParabolaFitting(_pts, true, is_rectify, rectify_point);
}


Vec3d qr_cylinder::Arc::ParabolaFittingX(bool is_rectify, Point rectify_point)
{
    return ParabolaFitting(_pts, false, is_rectify, rectify_point);
}

/*
 * 最小二乘抛物线拟合有两种情况
 * 1. 用y=a*x^2+b*x+c来拟合，求[a b c]三个参数,此时is_Y = true;
 * 2. 用x=a*y^2+b*y+c来拟合，求[a b c]三个参数，此时is_Y = false;
 */
Vec3d qr_cylinder::Arc::ParabolaFitting(const vector<Point> &src_pts, bool is_y, bool is_rectify, Point rp)
{
    Mat A, A_transpose;
    Mat b;
    if (!is_rectify) {
        A.create(src_pts.size(), 3, CV_64FC1);
        b.create(src_pts.size(), 1, CV_64FC1);
    } else {
        A.create(src_pts.size()+1, 3, CV_64FC1);
        b.create(src_pts.size()+1, 1, CV_64FC1);
    }
    if (is_y) {
        for (size_t i = 0; i < src_pts.size(); ++i) {
            A.at<double>(i, 0) = src_pts[i].x*src_pts[i].x;
            A.at<double>(i, 1) = src_pts[i].x;
            A.at<double>(i, 2) = 1;
            b.at<double>(i, 0) = src_pts[i].y;
        }
        if (is_rectify) {
            A.at<double>(src_pts.size(), 0) = rp.x*rp.x;
            A.at<double>(src_pts.size(), 1) = rp.x;
            A.at<double>(src_pts.size(), 2) = 1;
            b.at<double>(src_pts.size(), 0) = rp.y;
        }
    } else {
        for (size_t i = 0; i < src_pts.size(); ++i) {
            A.at<double>(i, 0) = src_pts[i].y*src_pts[i].y;
            A.at<double>(i, 1) = src_pts[i].y;
            A.at<double>(i, 2) = 1;
            b.at<double>(i, 0) = src_pts[i].x;
        }
        if (is_rectify) {
            A.at<double>(src_pts.size(), 0) = rp.y*rp.y;
            A.at<double>(src_pts.size(), 1) = rp.y;
            A.at<double>(src_pts.size(), 2) = 1;
            b.at<double>(src_pts.size(), 0) = rp.x;
        }
    }
    transpose(A, A_transpose);
    Mat A_square = A_transpose*A;
    Mat A_square_inverse;
    invert(A_square, A_square_inverse);
    Mat res = A_square_inverse*A_transpose*b;
    Vec3d r;
    r[0] = res.at<double>(0, 0);
    r[1] = res.at<double>(1, 0);
    r[2] = res.at<double>(2, 0);
    return r;
}

//为了和抛物线拟合兼容，也是使用Vec3d作为返回值
//只是此时二次项为0
Vec3d qr_cylinder::Arc::LineFittingY()
{
    return LineFitting(_pts, true);
}

Vec3d qr_cylinder::Arc::LineFittingX()
{
    return LineFitting(_pts, false);
}

/*
 * 最小二乘直线拟合有两种情况
 * 1. 用y=a*x+b来拟合，求[0 a b]中两个参数,此时is_Y = true;
 * 2. 用x=a*y+b来拟合，求[0 a b]中两个参数，此时is_Y = false;
 */
Vec3d qr_cylinder::Arc::LineFitting(const vector<Point> &src_pts, bool is_y)
{
    Mat A, A_transpose;
    A.create(src_pts.size(), 2, CV_64FC1);
    Mat b;
    b.create(src_pts.size(), 1, CV_64FC1);
    if (is_y) {
        for (size_t i = 0; i < src_pts.size(); ++i) {
            A.at<double>(i, 0) = src_pts[i].x;
            A.at<double>(i, 1) = 1;
            b.at<double>(i, 0) = src_pts[i].y;
        }
    } else {
        for (size_t i = 0; i < src_pts.size(); ++i) {
            A.at<double>(i, 0) = src_pts[i].y;
            A.at<double>(i, 1) = 1;
            b.at<double>(i, 0) = src_pts[i].x;
        }
    }
    transpose(A, A_transpose);
    Mat A_square = A_transpose*A;
    Mat A_square_inverse;
    invert(A_square, A_square_inverse);
    Mat res = A_square_inverse*A_transpose*b;
    Vec3d r;
    r[0] = 0;
    r[1] = res.at<double>(0, 0);
    r[2] = res.at<double>(1, 0);
    return r;
}

double qr_cylinder::Arc::CalParabolaFittingErrorX(const Vec3d &para)
{
    return CalParabolaFittingError(para, _pts, false);
}

double qr_cylinder::Arc::CalParabolaFittingErrorY(const Vec3d &para)
{
    return CalParabolaFittingError(para, _pts, true);
}

/*
 * para: Vec3d [a b c]
 * is_Y
 *     true: y-a*x^2-b*x-c
 */
double qr_cylinder::Arc::CalParabolaFittingError(const Vec3d &para, const vector<Point> &src_pts, bool is_y)
{
    double ans = 0;
    if (is_y)
        for (size_t i = 0; i < src_pts.size(); ++i)
            ans += (src_pts[i].y-(src_pts[i].x*src_pts[i].x*para[0]
                    +src_pts[i].x*para[1]+para[2]));
    else
        for (size_t i = 0; i < src_pts.size(); ++i)
            ans += (src_pts[i].x-(src_pts[i].y*src_pts[i].y*para[0]
                    +src_pts[i].y*para[1]+para[2]));
    return ans;
}

double qr_cylinder::Arc::CalLineFittingErrorY(const Vec2d &para)
{
    return CalLineFittingError(para, _pts, true);
}

double qr_cylinder::Arc::CalLineFittingErrorX(const Vec2d &para)
{
    return CalLineFittingError(para, _pts, false);
}

double qr_cylinder::Arc::CalLineFittingError(const Vec2d &para, const vector<Point> &src_pts, bool is_y)
{
    double ans = 0;
    if (is_y)
        for (size_t i = 0; i < _pts.size(); ++i) {
            ans += (src_pts[i].y-(src_pts[i].x*para[0]+para[1]));
        }
    else
        for (size_t i = 0; i < _pts.size(); ++i) {
            ans += (src_pts[i].x-(src_pts[i].y*para[0]+para[1]));
        }
    return ans;
}

void getMeanXY(const vector<Point> &pts, double &mean_x, double &mean_y)
{
    mean_x = 0;
    mean_y = 0;
    for (size_t i = 0; i < pts.size(); ++i) {
        mean_x += (static_cast<double>(pts[i].x)/pts.size());
        mean_y += (static_cast<double>(pts[i].y)/pts.size());
    }
}

void getCenterXY(const vector<Point> &pts, double &sx, double &sy)
{
    int max_x, min_x, max_y, min_y;
    max_x = pts[0].x;
    min_x = pts[0].x;
    max_y = pts[0].y;
    min_y = pts[0].y;
    for (size_t i = 1; i < pts.size(); ++i) {
        if (max_x < pts[i].x)
            max_x = pts[i].x;
        if (min_x > pts[i].x)
            min_x = pts[i].x;
        if (max_y < pts[i].y)
            max_y = pts[i].y;
        if (min_y > pts[i].y)
            min_y = pts[i].y;
    }
    sx = (max_x-min_x)/2.0;
    sy = (max_y-min_y)/2.0;
}



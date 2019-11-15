#include "finder_pattern.h"

qr_cylinder::FinderPattern::FinderPattern()
{
    _up = new Arc();
    _down = new Arc();
    _left = new Arc();
    _right = new Arc();
}

void qr_cylinder::FinderPattern::SetPts(const vector<Point> &pts, int cols, int rows)
{
    _cols = cols;
    _rows = rows;
    if (!_orig_pts.empty())
        _orig_pts.clear();
    _orig_pts.assign(pts.begin(), pts.end());
    ResortPtsAndSetArc();
}

qr_cylinder::FinderPattern::~FinderPattern()
{
    delete _up;
    delete _down;
    delete _left;
    delete _right;
}

Point2f qr_cylinder::CalVecPara(const Point2f &vec_x, const Point2f &vec_y, const Point2f &vec_p)
{
    Point2f res;
    //Using solve function to get the answer
    Mat A, x, b;
    A.create(2, 2, CV_32FC1);
    A.at<float>(0, 0) = vec_x.x;
    A.at<float>(1, 0) = vec_x.y;
    A.at<float>(0, 1) = vec_y.x;
    A.at<float>(1, 1) = vec_y.y;
    b.create(2, 1, CV_32FC1);
    b.at<float>(0, 0) = vec_p.x;
    b.at<float>(1, 0) = vec_p.y;
    solve(A, b, x);
    res.x = x.at<float>(0, 0);
    res.y = x.at<float>(1, 0);
    return res;
}

void qr_cylinder::FinderPattern::Find4Corners(const vector<Point> &src_pts, vector<Point2f> &output_corners)
{
    if (!output_corners.empty())
        output_corners.clear();
    Rect rect = boundingRect(src_pts);
    Point base_pt = rect.tl();
    int width = rect.width + rect.width/2;
    int height = rect.height + rect.height/2;
    base_pt.x -= rect.width/4;
    base_pt.y -= rect.height/4;
    if (base_pt.x < 0)
        base_pt.x = 0;
    if (base_pt.y < 0)
        base_pt.y = 0;
    Mat tmp;
    tmp.create(height, width, CV_8UC1);
    tmp = Scalar::all(0);
    Point p1, p2;
    for (size_t i = 0; i < src_pts.size(); ++i) {
        p1 = src_pts[i];
        p2 = src_pts[(i+1)%src_pts.size()];
        p1.x -= base_pt.x;
        p1.y -= base_pt.y;
        p2.x -= base_pt.x;
        p2.y -= base_pt.y;
        line(tmp, p1, p2, Scalar(255, 255, 255));
    }
    double qualityLevel = 0.01;
    double minDistance = 8;
    int blockSize = 9;
    double k = 0.04;
    goodFeaturesToTrack(tmp, output_corners,
                        4, qualityLevel, minDistance,
                        Mat(), blockSize, false, k);
    for (size_t i = 0; i < output_corners.size(); ++i) {
        output_corners[i].x += base_pt.x;
        output_corners[i].y += base_pt.y;
    }
}

void qr_cylinder::FinderPattern::ResortPtsAndSetArc()
{
    Find4Corners(_orig_pts, _corners);

    // 找到四点的方位信息，UP-LEFT UP-RIGHT DOWN-LEFT DOWN-RIGHT
    // 并且找到他们的中心点CENTER, 以中心点到上的两个点作两个向量，
    // 计算各个点由它们表示的新坐标值的正负来判断他们是属于那一条边
    // UP DOWN LEFT RIGHT并且赋值给arc类
    // 以下这种方法并不是好的方法，是只考虑一个FP的情形所用到的方法
    // 所以需要找到一种方法使得
    // 上一层fp_manager确定方位型后重新来调整这个FP的上下左右
    sort(_corners.begin(), _corners.end(), LessY);
    vector<Point2f> up_pts, down_pts;
    up_pts.push_back(_corners[0]);
    up_pts.push_back(_corners[1]);
    down_pts.push_back(_corners[2]);
    down_pts.push_back(_corners[3]);
    sort(up_pts.begin(), up_pts.end(), LessX);
    sort(down_pts.begin(), down_pts.end(), LessX);
    if (!_corners.empty())
        _corners.clear();
    //按照up_left, up_right, down_right, down_left顺序存储角点
    _corners.push_back(up_pts[0]);
    _corners.push_back(up_pts[1]);
    _corners.push_back(down_pts[1]);
    _corners.push_back(down_pts[0]);

    Vec3d line1, line2, center_vec3d;
    line1[0] = _corners[0].y - _corners[2].y;
    line1[1] = _corners[2].x - _corners[0].x;
    line1[2] = _corners[0].x*_corners[2].y - _corners[0].y*_corners[2].x;
    line2[0] = _corners[1].y - _corners[3].y;
    line2[1] = _corners[3].x - _corners[1].x;
    line2[2] = _corners[1].x*_corners[3].y - _corners[1].y*_corners[3].x;
    center_vec3d[0] = line1[1]*line2[2] - line2[1]*line1[2];
    center_vec3d[1] = line2[0]*line1[2] - line1[0]*line2[2];
    center_vec3d[2] = line1[0]*line2[1] - line1[1]*line2[0];

    Point2f center;
    center.x = center_vec3d[0]/center_vec3d[2];
    center.y = center_vec3d[1]/center_vec3d[2];

    _center_point = center;

    Point2f vecX, vecY, vecP;
    vecX.x = up_pts[0].x - center.x;
    vecX.y = up_pts[0].y - center.y;
    vecY.x = up_pts[1].x - center.x;
    vecY.y = up_pts[1].y - center.y;
    vector<Point> UP, LEFT, RIGHT, DOWN;
    for (size_t i = 0; i < _orig_pts.size(); ++i) {
        vecP.x = _orig_pts[i].x - center.x;
        vecP.y = _orig_pts[i].y - center.y;
        Point2f vecRes = CalVecPara(vecX, vecY, vecP);
        if (vecRes.x > 0 && vecRes.y > 0)
            UP.push_back(_orig_pts[i]);
        else if (vecRes.x > 0 && vecRes.y < 0)
            LEFT.push_back(_orig_pts[i]);
        else if (vecRes.x < 0 && vecRes.y > 0)
            RIGHT.push_back(_orig_pts[i]);
        else if (vecRes.x < 0 && vecRes.y < 0)
            DOWN.push_back(_orig_pts[i]);
    }
    /*
     ** arc_num: 0~3
     ** 0: up arc
     ** 1: down arc
     ** 2: left arc
     ** 3: right arc
    */
    _up->SetPts(UP, true, true);
    _arcs[0] = _up;
    _down->SetPts(DOWN, true, true);
    _arcs[1] = _down;
    _left->SetPts(LEFT, true, false);
    _arcs[2] = _left;
    _right->SetPts(RIGHT, true, false);
    _arcs[3] = _right;
}

Point qr_cylinder::FinderPattern::GetCenterPoint()
{
    return _center_point;
}

void qr_cylinder::FinderPattern::DrawFp(Mat &draw_pic)
{
    _up->DrawArc(draw_pic, Scalar(255, 0, 0));
    _down->DrawArc(draw_pic, Scalar(0, 255, 0));
    _left->DrawArc(draw_pic, Scalar(0, 255, 255));
    _right->DrawArc(draw_pic, Scalar(255, 255, 0));
}

/*
 ** arc_num: 0~3
 ** 0: up arc
 ** 1: down arc
 ** 2: left arc
 ** 3: right arc
*/
void qr_cylinder::FinderPattern::PushArcPtsTo(unsigned int arc_num, vector<Point> &dst)
{
    _arcs[arc_num%4]->CopyPtsTo(dst);
}

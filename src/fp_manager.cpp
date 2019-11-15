#include "fp_manager.h"
#include <cmath>

qr_cylinder::FpManager::FpManager()
{
    _left = new FinderPattern();
    _center = new FinderPattern();
    _right = new FinderPattern();
    _arc_up = new Arc();
    _arc_down = new Arc();
    _arc_left = new Arc();
    _arc_right = new Arc();
}

qr_cylinder::FpManager::~FpManager()
{
    delete _left;
    delete _center;
    delete _right;
    delete _arc_up;
    delete _arc_down;
    delete _arc_left;
    delete _arc_right;
}

void qr_cylinder::FpManager::SetFps(const vector<Point>& fp1, const vector<Point>& fp2, const vector<Point>& fp3,
                         int cols, int rows)
{
    _cols = cols;
    _rows = rows;
    _left->SetPts(fp1, _cols, _rows);
    _center->SetPts(fp2, _cols, _rows);
    _right->SetPts(fp3, _cols, _rows);

    FinderPattern* tmp[3];
    tmp[0] = _left;
    tmp[1] = _center;
    tmp[2] = _right;
    Point tps[3];
    tps[0] = tmp[0]->GetCenterPoint();
    tps[1] = tmp[1]->GetCenterPoint();
    tps[2] = tmp[2]->GetCenterPoint();
    double length[3];
    int i;
    for (i = 0; i < 3; ++i) {
        length[i] = sqrt(static_cast<double>(pow(tps[i].y - tps[(i+1)%3].y, 2) +
                         pow(tps[i].x - tps[(i+1)%3].x, 2)));
        cout<<"Length "<<i<<": "<<length[i]<<endl;
    }
    int center_index, left_index, right_index, max_index = 0;
    for (i = 1; i < 3; ++i) {
        if (length[i] > length[max_index])
            max_index = i;
    }
    center_index = (max_index + 2) % 3;
    cout<<"center_index "<<center_index<<endl;

    Point centerAll;
    centerAll.x = 0;
    centerAll.y = 0;
    for (i = 0; i < 3; ++i) {
        centerAll.x += tps[i].x;
        centerAll.y += tps[i].y;
    }
    centerAll.x /= 3;
    centerAll.y /= 3;
    Point vecC, vec;
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
    _center = tmp[center_index];
    _left = tmp[left_index];
    _right = tmp[right_index];
    _point_center = tps[center_index];
    _point_left = tps[left_index];
    _point_right = tps[right_index];
    cout<<"left_index "<<left_index<<endl;
    cout<<"right_index "<<right_index<<endl;
    cout<<"vecC x: "<<vecC.x<<" vecC y: "<<vecC.y<<endl;

    //确定方位型
    if (vecC.x <= 0 && vecC.y < 0) {
        cout<<"Type I"<<endl;
        _fp_type = _LL;
    } else if (vecC.x > 0 && vecC.y <=0) {
        cout<<"Type II"<<endl;
        _fp_type = _ML;
    } else if (vecC.x > 0 && vecC.y > 0) {
        cout<<"Type III"<<endl;
        _fp_type = _MM;
    } else {
        cout<<"Type IV"<<endl;
        _fp_type = _LM;
    }

    //把三个FP的外边缘提取出来以提供后续拟合使用
    vector<Point> vec_up, vec_down, vec_left, vec_right;
    switch (_fp_type) {
    case _LL:
        _center->PushArcPtsTo(0, vec_up);
        _right->PushArcPtsTo(0, vec_up);
        _arc_up->SetTwo(true);
        _left->PushArcPtsTo(1, vec_down);
        _center->PushArcPtsTo(2, vec_left);
        _left->PushArcPtsTo(2, vec_left);
        _arc_left->SetTwo(true);
        _right->PushArcPtsTo(3, vec_right);
        break;
    case _ML:
        _left->PushArcPtsTo(0, vec_up);
        _center->PushArcPtsTo(0, vec_up);
        _arc_up->SetTwo(true);
        _right->PushArcPtsTo(1, vec_down);
        _left->PushArcPtsTo(2, vec_left);
        _center->PushArcPtsTo(3, vec_right);
        _right->PushArcPtsTo(3, vec_right);
        _arc_right->SetTwo(true);
        break;
    case _MM:
        _left->PushArcPtsTo(0, vec_up);
        _right->PushArcPtsTo(1, vec_down);
        _center->PushArcPtsTo(1, vec_down);
        _arc_down->SetTwo(true);
        _right->PushArcPtsTo(2, vec_left);
        _left->PushArcPtsTo(3, vec_right);
        _center->PushArcPtsTo(3, vec_right);
        _arc_right->SetTwo(true);
        break;
    case _LM:
        _right->PushArcPtsTo(0, vec_up);
        _center->PushArcPtsTo(1, vec_down);
        _left->PushArcPtsTo(1, vec_down);
        _arc_down->SetTwo(true);
        _right->PushArcPtsTo(2, vec_left);
        _center->PushArcPtsTo(2, vec_left);
        _arc_left->SetTwo(true);
        _left->PushArcPtsTo(3, vec_right);
        break;
    }

    _arc_up->SetPts(vec_up, false, true);
    _arc_down->SetPts(vec_down, false, true);
    _arc_left->SetPts(vec_left, false, false);
    _arc_right->SetPts(vec_right, false, false);
    _arcs[0] = _arc_up;
    _arcs[1] = _arc_down;
    _arcs[2] = _arc_left;
    _arcs[3] = _arc_right;

    //计算四条边使用旋转矩形包围时的短轴
    RotatedRect ur, dr, lr, rr;
    ur = minAreaRect(_arc_up->GetPts());
    dr = minAreaRect(_arc_down->GetPts());
    lr = minAreaRect(_arc_left->GetPts());
    rr = minAreaRect(_arc_right->GetPts());
    double up_down_s, left_right_s;
    up_down_s = min(ur.size.height, ur.size.width)+min(dr.size.height, dr.size.width);
    left_right_s = min(lr.size.height, lr.size.width)+min(rr.size.height, rr.size.width);

    //判断究竟是上下两边是曲边，还是左右两边是曲边
    if (up_down_s >= left_right_s) {
        _pb_type = _parabola_up_down;
        //此时上下两边是椭圆边，左右两边是直线边
        //y=a*x^2+b*x+c
        _res_para[0] = _arc_up->ParabolaFittingY();
        _res_para[1] = _arc_down->ParabolaFittingY();
        //x=a*y+b 为了避免可能出现的平行于y轴的情形
        _res_para[2] = _arc_left->LineFittingX();
        _res_para[3] = _arc_right->LineFittingX();
    } else {
        _pb_type = _parabola_left_right;
        //此时左右两边是椭圆边，上下两边是直线边
        //y=a*x+b 为了避免可能出现平行于x轴的情形
        _res_para[0] = _arc_up->LineFittingY();
        _res_para[1] = _arc_down->LineFittingY();
        _res_para[2] = _arc_left->ParabolaFittingX();
        _res_para[3] = _arc_right->ParabolaFittingX();
    }
    switch (_fp_type) {
    case _LL:
        _updown_lr[0] = _arc_up->GetStartEndXY(0);
        _updown_lr[1] = _arc_up->GetStartEndXY(1);
        _updown_lr[2] = _arc_down->GetStartEndXY(0);
        switch (_pb_type) {
        case _parabola_up_down:
            _updown_lr[3] = CalParabolaY2LineX(_res_para[1], _res_para[3]);
            _rectify_para_index = 1;
            _rsi = 2;
            _is_rectify_y = true;
            break;
        case _parabola_left_right:
            _updown_lr[3] = CalParabolaX2LineY(_res_para[3], _res_para[1]);
            _rectify_para_index = 3;
            _rsi = 1;
            _is_rectify_y = false;
            break;
        }
        _rei = 3;
        break;
    case _ML:
        _updown_lr[0] = _arc_up->GetStartEndXY(0);
        _updown_lr[1] = _arc_up->GetStartEndXY(1);
        _updown_lr[3] = _arc_down->GetStartEndXY(1);
        switch (_pb_type) {
        case _parabola_up_down:
            _updown_lr[2] = CalParabolaY2LineX(_res_para[1], _res_para[2]);
            _rectify_para_index = 1;
            _rsi = 3;
            _is_rectify_y = true;
            break;
        case _parabola_left_right:
            _updown_lr[2] = CalParabolaX2LineY(_res_para[2], _res_para[1]);
            _rectify_para_index = 2;
            _rsi = 0;
            _is_rectify_y = false;
            break;
        }
        _rei = 2;
        break;
    case _MM:
        _updown_lr[1] = _arc_up->GetStartEndXY(1);
        _updown_lr[2] = _arc_down->GetStartEndXY(0);
        _updown_lr[3] = _arc_down->GetStartEndXY(1);
        switch (_pb_type) {
        case _parabola_up_down:
            _updown_lr[0] = CalParabolaY2LineX(_res_para[0], _res_para[2]);
            _rectify_para_index = 0;
            _rsi = 1;
            _is_rectify_y = true;
            break;
        case _parabola_left_right:
            _updown_lr[0] = CalParabolaX2LineY(_res_para[2], _res_para[0]);
            _rectify_para_index = 2;
            _rsi = 2;
            _is_rectify_y = false;
            break;
        }
        _rei = 0;
        break;
    case _LM:
        _updown_lr[0] = _arc_up->GetStartEndXY(0);
        _updown_lr[2] = _arc_down->GetStartEndXY(0);
        _updown_lr[3] = _arc_down->GetStartEndXY(1);
        switch (_pb_type) {
        case _parabola_up_down:
            _updown_lr[1] = CalParabolaY2LineX(_res_para[0], _res_para[3]);
            _rectify_para_index = 0;
            _rsi = 0;
            _is_rectify_y = true;
            break;
        case _parabola_left_right:
            _updown_lr[1] = CalParabolaX2LineY(_res_para[3], _res_para[0]);
            _rectify_para_index = 3;
            _rsi = 3;
            _is_rectify_y = false;
            break;
        }
        _rei = 1;
        break;
    }
}

void qr_cylinder::FpManager::DrawArcs(Mat &draw_pic)
{

    circle(draw_pic, _updown_lr[0], 3, Scalar(0, 0, 255), -1);
    circle(draw_pic, _updown_lr[1], 3, Scalar(0, 255, 0), -1);
    circle(draw_pic, _updown_lr[2], 3, Scalar(255, 0, 0), -1);
    circle(draw_pic, _updown_lr[3], 3, Scalar(255, 255, 0), -1);

    _arc_up->DrawParabola(draw_pic, _res_para[0], Scalar(0, 0, 255), Size(_cols, _rows), true,
            _updown_lr[0], _updown_lr[1]);
    _arc_down->DrawParabola(draw_pic, _res_para[1], Scalar(0, 0, 255), Size(_cols, _rows), true,
            _updown_lr[2], _updown_lr[3]);
    _arc_left->DrawParabola(draw_pic, _res_para[2], Scalar(0, 0, 255), Size(_cols, _rows), false,
            _updown_lr[0], _updown_lr[2]);
    _arc_right->DrawParabola(draw_pic, _res_para[3], Scalar(0, 0, 255), Size(_cols, _rows),false,
            _updown_lr[1], _updown_lr[3]);

}

/*
 * 这个函数用于画出四条弧边，使用白色
 * 一般使用灰度图像来为后续处理生成Mask图像
 */
void qr_cylinder::FpManager::DrawWhiteArcs(Mat &draw_pic)
{

    circle(draw_pic, _updown_lr[0], 2, Scalar(255, 255, 255), -1);
    circle(draw_pic, _updown_lr[1], 2, Scalar(255, 255, 255), -1);
    circle(draw_pic, _updown_lr[2], 2, Scalar(255, 255, 255), -1);
    circle(draw_pic, _updown_lr[3], 2, Scalar(255, 255, 255), -1);

    _arc_up->DrawParabola(draw_pic, _res_para[0], Scalar(255, 255, 255), Size(_cols, _rows), true,
            _updown_lr[0], _updown_lr[1]);
    _arc_down->DrawParabola(draw_pic, _res_para[1], Scalar(255, 255, 255), Size(_cols, _rows), true,
            _updown_lr[2], _updown_lr[3]);
    _arc_left->DrawParabola(draw_pic, _res_para[2], Scalar(255, 255, 255), Size(_cols, _rows), false,
            _updown_lr[0], _updown_lr[2]);
    _arc_right->DrawParabola(draw_pic, _res_para[3], Scalar(255, 255, 255), Size(_cols, _rows),false,
            _updown_lr[1], _updown_lr[3]);
}

/*
 *
 * 这个函数假设para和line一定有交点！这在当前程序目的的条件下必然成立
 * para: x = a1*y^2 + b1*y + c1
 * line: y = b2*x + c2, a2=0 => x = b2'*y + c2'
 * return: intersection of para and line in area [COLS ROWS]
 */
Point qr_cylinder::FpManager::CalParabolaX2LineY(const Vec3d &para, const Vec3d &line)
{
    Point res(-1, -1);
    if (abs(line[1]) <= 0.000000001) {
        res.y = line[2];
        res.x = para[0]*res.y*res.y + para[1]*res.y + para[2];
    } else {
        double b2 = 1.0/line[1];
        double c2 = -line[2]/line[1];
        double a = para[0];
        double b = para[1]-b2;
        double c = para[2]-c2;
        double delta = b*b-4*a*c;
        if (delta < 0)
            return res;
        delta = sqrt(delta);
        double y = (-b+delta)/(2*a);
        double x = para[0]*y*y+para[1]*y+para[2];
        if (y > 0 && y < _rows && x >0 && x < _cols) {
            res.x = x;
            res.y = y;
        } else {
            y = (-b-delta)/(2*a);
            x = para[0]*y*y+para[1]*y+para[2];
            res.x = x;
            res.y = y;
        }
    }
    return res;
}

/*
 * 这个函数假设para和line一定有交点！这在当前程序目的的条件下必然成立
 * para: y = a1*x^2 + b1*x + c1
 * line: x = b2*y + c2, a2=0 => y = b2'*x + c2'
 * return: intersection of para and line in area [COLS ROWS]
 */
Point qr_cylinder::FpManager::CalParabolaY2LineX(const Vec3d &para, const Vec3d &line)
{
    Point res(-1, -1);
    if (abs(line[1]) <= 0.000000001) {
        res.x = line[2];
        res.y = para[0]*res.x*res.x + para[1]*res.x + para[2];
    } else {
        double b2 = 1.0/line[1];
        double c2 = -line[2]/line[1];
        double a = para[0];
        double b = para[1]-b2;
        double c = para[2]-c2;
        double delta = b*b-4*a*c;
        if (delta < 0)
            return res;
        delta = sqrt(delta);
        double x = (-b+delta)/(2*a);
        double y = para[0]*x*x+para[1]*x+para[2];
        if (y > 0 && y < _rows && x >0 && x < _cols) {
            res.x = x;
            res.y = y;
        } else {
            x = (-b-delta)/(2*a);
            y = para[0]*x*x+para[1]*x+para[2];
            res.x = x;
            res.y = y;
        }
    }
    return res;
}

/*
 * Vec3d para: [a b c]
 * is_Y
 * true: y = a*x^2 + b*x +c
 * false: x = a*y^2 + b*y +c
 * src_img: gray scale img 0~255 pixel value
 * 阈值考虑为50，小于50认为是黑色！这个是人眼对比灰度条上的结果
 * 如果白色/黑色 > 12，则认为是这条线不准确，在二维码区域以外
 * 如果白色/黑色区域 < 3，则认为这条线覆盖了二维码区域
 * 这些点start_point, end_point理论上来说必须在图像区域以内
 */
double qr_cylinder::FpManager::CalImgBlackPtsRatio(const Mat &src_img, const Vec3d &para,
                                       const Point &start_point, const Point &end_point, bool is_Y)
{
    if (start_point.x < 0 || start_point.y < 0
            || end_point.y < 0 || end_point.x < 0
            || start_point.x > _cols || start_point.y > _rows
            || end_point.x > _cols || end_point.y > _rows) {
        return 200;
    }
    uchar data=0/*, max_data=0, min_data=255*/;
    Point sp = start_point;
    Point ep = end_point;
    int x=0, y=0;
    int lessPix=0, allpts = 0;
    if (is_Y) {
        if (sp.x > ep.x) {
            Point tmp = ep;
            ep = sp;
            sp = tmp;
        }
        for (x = sp.x; x <= ep.x; ++x) {
            y = para[0]*x*x + para[1]*x + para[2];
            data = src_img.at<uchar>(y, x);
            if (data < 50)
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
            x = para[0]*y*y + para[1]*y +para[2];
            data = src_img.at<uchar>(y, x);
            if (data < 50)
                ++lessPix;
            ++allpts;
        }
    }
    double res = static_cast<double>(allpts-lessPix)/(lessPix);
    return res;
}

double qr_cylinder::FpManager::CalRatio(const Mat &src_img)
{
    double ratio = 0;
    switch (_fp_type) {
    case _LL:
        if (_pb_type == _parabola_up_down)
            ratio = CalImgBlackPtsRatio(src_img, _res_para[1], _updown_lr[2], _updown_lr[3], true);
        else
            ratio = CalImgBlackPtsRatio(src_img, _res_para[3], _updown_lr[1], _updown_lr[3], false);
        break;
    case _ML:
        if (_pb_type == _parabola_up_down)
            ratio = CalImgBlackPtsRatio(src_img, _res_para[1], _updown_lr[2], _updown_lr[3], true);
        else
            ratio = CalImgBlackPtsRatio(src_img, _res_para[2], _updown_lr[0], _updown_lr[2], false);
        break;
    case _MM:
        if (_pb_type == _parabola_up_down)
            ratio = CalImgBlackPtsRatio(src_img, _res_para[0], _updown_lr[0], _updown_lr[1], true);
        else
            ratio = CalImgBlackPtsRatio(src_img, _res_para[2], _updown_lr[0], _updown_lr[2], false);
        break;
    case _LM:
        if (_pb_type == _parabola_up_down)
            ratio = CalImgBlackPtsRatio(src_img, _res_para[0], _updown_lr[0], _updown_lr[1], true);
        else
            ratio = CalImgBlackPtsRatio(src_img, _res_para[3], _updown_lr[1], _updown_lr[3], false);
        break;
    }
    return ratio;
}

/*
 * direction > 0: 向线外移动，对于线覆盖二维码的情形
 * direction < 0: 向线内移动，对于线在二维码外的情形
 * 此时para是直线方程，故只有para[1]和para[2]有效
 */
Point qr_cylinder::FpManager::GetNP(int direction, const Point &p)
{
    Point res(-1, -1);
    Vec3d para;
    int dir = 0;
    bool is_Y=true;
    switch (_fp_type) {
    case _LL:
        switch (_pb_type) {
        case _parabola_up_down:
            para = _res_para[3];
            is_Y = false;
            dir = 1;
            break;
        case _parabola_left_right:
            para = _res_para[1];
            is_Y = true;
            dir = 1;
            break;
        }
        break;
    case _ML:
        switch (_pb_type) {
        case _parabola_up_down:
            para = _res_para[2];
            is_Y = false;
            dir = 1;
            break;
        case _parabola_left_right:
            para = _res_para[1];
            is_Y = true;
            dir = -1;
            break;
        }
        break;
    case _MM:
        switch (_pb_type) {
        case _parabola_up_down:
            para = _res_para[2];
            is_Y = false;
            dir = -1;
            break;
        case _parabola_left_right:
            para = _res_para[0];
            is_Y = true;
            dir = -1;
            break;
        }
        break;
    case _LM:
        switch (_pb_type) {
        case _parabola_up_down:
            para = _res_para[3];
            is_Y = false;
            dir = -1;
            break;
        case _parabola_left_right:
            para = _res_para[0];
            is_Y = true;
            dir = 1;
            break;
        }
        break;
    }
    dir *= direction;
    int x, y;
    if (!is_Y) {
        y = p.y;
        y += dir;
        if (y <= 0)
            y = 1;
        if (y >= _rows)
            y = _rows-1;
        x = para[1]*y + para[2];
    } else {
        x = p.x;
        x += dir;
        if (x <= 0)
            x = 1;
        if (x >= _cols)
            x = _cols-1;
        y = para[1]*x + para[2];
    }
    if (x > 0 && x < _cols && y > 0 && y < _rows) {
        res.x = x;
        res.y = y;
    }
    return res;
}
#define RATIO_LOW 6
#define RATIO_HIGH 12
#define RATIO_STEP 1
void qr_cylinder::FpManager::Rectify4Corners(const Mat &src_img)
{
    double ratio = CalRatio(src_img);
    if (ratio >=RATIO_LOW && ratio <= RATIO_HIGH) {
        if (_is_rectify_y) {
            _updown_lr[0] = CalParabolaY2LineX(_res_para[0], _res_para[2]);
            _updown_lr[1] = CalParabolaY2LineX(_res_para[0], _res_para[3]);
            _updown_lr[2] = CalParabolaY2LineX(_res_para[1], _res_para[2]);
            _updown_lr[3] = CalParabolaY2LineX(_res_para[1], _res_para[3]);
        } else {
            _updown_lr[0] = CalParabolaX2LineY(_res_para[2], _res_para[0]);
            _updown_lr[1] = CalParabolaX2LineY(_res_para[3], _res_para[0]);
            _updown_lr[2] = CalParabolaX2LineY(_res_para[2], _res_para[1]);
            _updown_lr[3] = CalParabolaX2LineY(_res_para[3], _res_para[1]);
        }
        return;
    }
    Point rp = _updown_lr[_rei];
    Point rp1;
    Vec3d para;
    int step=10;
    if (ratio < RATIO_LOW) {
        step = RATIO_STEP;
    } else if (ratio > RATIO_HIGH) {
        step = -RATIO_STEP;
    }
    rp1 = GetNP(step, rp);
    if (_is_rectify_y) {
        //说明是Y
        para = _arcs[_rectify_para_index]->ParabolaFittingY(true, rp1);
        ratio = CalImgBlackPtsRatio(src_img, para, _updown_lr[_rsi], rp1, true);
        if (step > 0)
            while (ratio < RATIO_LOW) {
                rp = rp1;
                rp1 = GetNP(step, rp);
                para = _arcs[_rectify_para_index]->ParabolaFittingY(true, rp1);
                ratio = CalImgBlackPtsRatio(src_img, para, _updown_lr[_rsi], rp1, true);
            }
        else
            while (ratio > RATIO_HIGH) {
                rp = rp1;
                rp1 = GetNP(step, rp);
                para = _arcs[_rectify_para_index]->ParabolaFittingY(true, rp1);
                ratio = CalImgBlackPtsRatio(src_img, para, _updown_lr[_rsi], rp1, true);
            }
    } else {
        para = _arcs[_rectify_para_index]->ParabolaFittingX(true, rp1);
        ratio = CalImgBlackPtsRatio(src_img, para, _updown_lr[_rsi], rp1, false);
        if (step > 0)
            while (ratio < RATIO_LOW) {
                rp = rp1;
                rp1 = GetNP(step, rp);
                para = _arcs[_rectify_para_index]->ParabolaFittingX(true, rp1);
                ratio = CalImgBlackPtsRatio(src_img, para, _updown_lr[_rsi], rp1, false);
            }
        else
            while (ratio > RATIO_HIGH) {
                rp = rp1;
                rp1 = GetNP(step, rp);
                para = _arcs[_rectify_para_index]->ParabolaFittingX(true, rp1);
                ratio = CalImgBlackPtsRatio(src_img, para, _updown_lr[_rsi], rp1, false);
            }
    }
    _updown_lr[_rei] = rp1;
    _res_para[_rectify_para_index] = para;

    if (_is_rectify_y) {
        _updown_lr[0] = CalParabolaY2LineX(_res_para[0], _res_para[2]);
        _updown_lr[1] = CalParabolaY2LineX(_res_para[0], _res_para[3]);
        _updown_lr[2] = CalParabolaY2LineX(_res_para[1], _res_para[2]);
        _updown_lr[3] = CalParabolaY2LineX(_res_para[1], _res_para[3]);
    } else {
        _updown_lr[0] = CalParabolaX2LineY(_res_para[2], _res_para[0]);
        _updown_lr[1] = CalParabolaX2LineY(_res_para[3], _res_para[0]);
        _updown_lr[2] = CalParabolaX2LineY(_res_para[2], _res_para[1]);
        _updown_lr[3] = CalParabolaX2LineY(_res_para[3], _res_para[1]);
    }
}

/*
 * 返回想要包含这个二维码区域的正方形应该具有的图形宽度(长度)
 */
int qr_cylinder::FpManager::GetSizeOfBox()
{
    vector<Point> pts;
    pts.push_back(_updown_lr[0]);
    pts.push_back(_updown_lr[1]);
    pts.push_back(_updown_lr[2]);
    pts.push_back(_updown_lr[3]);
    Rect rect = boundingRect(pts);
    return max(rect.height, rect.width)*2;
}

void qr_cylinder::FpManager::Get4PtsForPerspectiveTransform(Point2f &up_l, Point2f &up_r,
                                                             Point2f &down_l, Point2f &down_r)
{
    if (_is_rectify_y) {
        up_l.x = _updown_lr[0].x;
        up_l.y = _updown_lr[0].y;
        up_r.x = _updown_lr[1].x;
        up_r.y = _updown_lr[1].y;
        down_l.x = _updown_lr[2].x;
        down_l.y = _updown_lr[2].y;
        down_r.x = _updown_lr[3].x;
        down_r.y = _updown_lr[3].y;
    } else {
        up_l.x = _updown_lr[2].x;
        up_l.y = _updown_lr[2].y;
        up_r.x = _updown_lr[0].x;
        up_r.y = _updown_lr[0].y;
        down_l.x = _updown_lr[3].x;
        down_l.y = _updown_lr[3].y;
        down_r.x = _updown_lr[1].x;
        down_r.y = _updown_lr[1].y;
    }
}


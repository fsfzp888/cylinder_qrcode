#ifndef ARC_H
#define ARC_H
#include <opencv2/opencv.hpp>
#include <vector>
#include <set>
#include <iostream>
#include <algorithm>
using namespace std;
using namespace cv;

namespace qr_cylinder
{
    struct EllipseFitRes{
        double _x_center;
        double _y_center;
        double _ru;
        double _rv;
        double _thetarad;
        int _angle;
    };

    bool LessX(const Point &, const Point &);
    bool LessY(const Point &, const Point &);

    class Arc
    {
    public:
        explicit Arc();
        //The following funciont setPts will change the order of points in original points vector
        void SetPts(vector<Point> &, bool is_filled_between, bool sort_x_or_y);
        const vector<Point>& GetPts();
        void DrawArc(Mat &src, const Scalar &color);
        void DrawAllPts(Mat &src, const Scalar &color);

        //抛物线拟合
        Vec3d ParabolaFittingY(bool is_rectify=false, Point rectify_point=Point(-1, -1));
        Vec3d ParabolaFittingX(bool is_rectify=false, Point rectify_point=Point(-1, -1));
        Vec3d ParabolaFitting(const vector<Point> &src_pts, bool is_y=true,
                              bool is_rectify=false, Point rectify_point=Point(-1, -1));

        /* 直线拟合,需要注意，以下函数认为不会出现以下情形
         * 1、拟合y=ax+b时，平行于y轴的情形
         * 2、拟合x=ay+b时，平行于x轴的情形
         * 如果出现这种情况，结果不会正确！因为返回值理论上无穷大！
         * 所以如果拟合非常接近平行于y轴的，使用x=ay+b拟合
         * 如果拟合非常接近平行于x轴的，使用y=ax+b拟合
        */
        Vec3d LineFittingY();
        Vec3d LineFittingX();
        Vec3d LineFitting(const vector<Point> &src_pts, bool is_y=true);

        //画拟合出来的抛物线，现在必须事先知道开始点和结束点
        void DrawParabola(Mat &draw_pic, const Vec3d &para, const Scalar &color, const Size& s,
                          bool is_y,
                          const Point& start_point, const Point& stop_point);
        void CopyPtsTo(vector<Point>& dst);

        //直线拟合误差计算
        double CalLineFittingErrorX(const Vec2d &para);
        double CalLineFittingErrorY(const Vec2d &para);
        double CalLineFittingError(const Vec2d &para, const vector<Point> &src_pts, bool is_y);

        //抛物线拟合误差计算
        double CalParabolaFittingErrorX(const Vec3d &para);
        double CalParabolaFittingErrorY(const Vec3d &para);
        double CalParabolaFittingError(const Vec3d &para, const vector<Point> &src_pts, bool is_y);

        void SetTwo(bool b);
        bool HasTwo();

        Point GetStartEndXY(int i);

    private:
        void InsertBetween(const Point& pt1, const Point& pt2);
        Mat _eig_val, _eig_vec;
        vector<Point> _pts;
        bool _has_two;
        Point _start_x, _end_x, _start_y, _end_y;
    };
}

#endif // ARC_H

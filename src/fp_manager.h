#ifndef FP_MANAGER_H
#define FP_MANAGER_H
#include "finder_pattern.h"

namespace qr_cylinder
{
    enum finder_pattern_type{_LL=1, _ML=2, _MM=4, _LM=8};
    enum parabola_type{_parabola_up_down=1, _parabola_left_right=2};

    class FpManager
    {
    public:
        explicit FpManager();
        ~FpManager();
        void SetFps(const vector<Point>& fp1, const vector<Point>& fp2, const vector<Point>& fp3,
                     int cols, int rows);
        void Rectify4Corners(const Mat& src_img);
        void DrawArcs(Mat &draw_pic);
        void DrawWhiteArcs(Mat &draw_pic);

        int GetSizeOfBox();
        void Get4PtsForPerspectiveTransform(Point2f &up_l, Point2f &up_r, Point2f &down_l, Point2f &down_r);

        Point CalParabolaX2LineY(const Vec3d &para, const Vec3d &line);
        Point CalParabolaY2LineX(const Vec3d &para, const Vec3d &line);

    private:

        double CalImgBlackPtsRatio(const Mat& src_img, const Vec3d &para,
                                   const Point &start_point, const Point &end_point, bool is_y);
        double CalRatio(const Mat& src_img);
        Point GetNP(int direction, const Point &p);

        FinderPattern *_left, *_center, *_right;
        Point _point_left, _point_center, _point_right;
        Arc *_arc_up, *_arc_down, *_arc_left, *_arc_right;
        Arc *_arcs[4];

        finder_pattern_type _fp_type;
        parabola_type _pb_type;
        Vec3d _res_para[4];//0:up 1:down 2:left 3:right
        Point _updown_lr[4];//0:up_left 1:up_right 2:down_left 3:down_right

        int _rectify_para_index=0;
        int _rsi=0, _rei=0;
        bool _is_rectify_y = true;

        int _cols=-1, _rows=-1;
    };
}

#endif // FP_MANAGER_H

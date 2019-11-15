#ifndef FINDER_PATTERN_H
#define FINDER_PATTERN_H
#include "arc.h"

namespace qr_cylinder
{
    Point2f CalVecPara(const Point2f &vec_x, const Point2f &vec_y, const Point2f &vec_p);

    class FinderPattern
    {
    public:
        explicit FinderPattern();
        void SetPts(const vector<Point> &, int cols, int rows);
        void DrawFp(Mat &draw_pic);
        ~FinderPattern();
        Point GetCenterPoint();
        void PushArcPtsTo(unsigned int arc_num, vector<Point> &dst);
        void Find4Corners(const vector<Point> &src_pts, vector<Point2f> &output_corners);

    private:
        void ResortPtsAndSetArc();

        vector<Point> _orig_pts;
        vector<Point2f> _corners;
        Arc *_up, *_down, *_left, *_right;
        Arc *_arcs[4];//0: up 1: down 2: left 3: right
        int _cols, _rows;
        Point _center_point;
    };
}

#endif // FINDER_PATTERN_H

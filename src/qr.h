#ifndef QR_H
#define QR_H
#include "fp_manager.h"

namespace qr_cylinder
{
    class Qr
    {
    public:
        explicit Qr();
        ~Qr();
        bool Process(const Mat&);
        static const int MASK_CUT_SIZE = 32;//把柱面图像切成八份重组
        void PerformPerspectiveTransform(const Mat &src, Mat &dst,
                                         const Point2f (&src_pts)[4], const Point2f (&dst_pts)[4]);
        void SetThreshBlocksize(int bs);
        void SetThreshDelta(int dt);
        void SetBlurBlocksize(int bbs);
        const Mat& GetResImg();
        const Mat& GetRecImg();
        const Mat& GetGrayImg();
        const Mat& GetSrcImg();
    private:
        void Init(const Mat &src_img);
        void Release();
        void BreakupAndRecombine(const Mat &src, const Mat &mask, Mat &res, int box_size);

        static const int ADAPTIVE_THRESH_BLOCKSIZE = 39;
        static const int ADAPTIVE_THRESH_DELTA = 0;
        static const int MEDIAN_BLUR_BLOCKSIZE = 9;

        Mat _src_img, _gray_img, _bin_img, _mask_img, _rec_img, _recm_img, _res_img;
        int _box_size;
        Point2f _src_pts[4];
        Point2f _dst_pts[4];
        vector<vector<Point> > _contours, _finders;
        vector<Vec4i> _hierarchy;
        int _block_size, _delta, _median_blur_size;
        FpManager *_fpm;
    };
}

#endif // QR_H

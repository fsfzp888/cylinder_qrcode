#include "qr.h"
#include <opencv2/stitching.hpp>

qr_cylinder::Qr::Qr()
    : _block_size(ADAPTIVE_THRESH_BLOCKSIZE),
      _delta(ADAPTIVE_THRESH_DELTA),
      _median_blur_size(MEDIAN_BLUR_BLOCKSIZE)
{
     _fpm = new FpManager();
}

void qr_cylinder::Qr::SetThreshBlocksize(int bs)
{
    _block_size = bs;
}

void qr_cylinder::Qr::SetThreshDelta(int dt)
{
    _delta = dt;
}

void qr_cylinder::Qr::SetBlurBlocksize(int bbs)
{
    _median_blur_size = bbs;
}

const Mat& qr_cylinder::Qr::GetGrayImg()
{
    return _gray_img;
}

const Mat& qr_cylinder::Qr::GetRecImg()
{
    return _rec_img;
}

const Mat& qr_cylinder::Qr::GetResImg()
{
    return _res_img;
}

const Mat& qr_cylinder::Qr::GetSrcImg()
{
    return _src_img;
}

qr_cylinder::Qr::~Qr()
{
    delete _fpm;
    Release();
}

void qr_cylinder::Qr::Init(const Mat &src_img)
{
    cvtColor(src_img, _gray_img, COLOR_BGR2GRAY);
    _src_img = src_img.clone();
    _bin_img.create(_gray_img.rows, _gray_img.cols, _gray_img.type());
    _bin_img = Scalar::all(0);
    _mask_img.create(_gray_img.rows, _gray_img.cols, _gray_img.type());
    _mask_img = Scalar::all(0);
}

void qr_cylinder::Qr::Release()
{
    if (_gray_img.data)
      _gray_img.release();
    if (_bin_img.data)
      _bin_img.release();
}

bool qr_cylinder::Qr::Process(const Mat &src_img)
{
    Init(src_img);
    adaptiveThreshold(_gray_img, _bin_img, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, _block_size,
                      _delta);
    //medianBlur是性能瓶颈之一，应当避免使用。
    medianBlur(_bin_img, _bin_img, _median_blur_size);//必须根据图像大小调整！
    _contours.clear();
    _hierarchy.clear();
    _finders.clear();
    findContours(_bin_img, _contours, _hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE);
    int c_index, cc_index, fp_count = 0;
    size_t i;
    for (i = 0; i < _hierarchy.size(); ++i) {
            c_index = _hierarchy[i][2];
            if (c_index != -1) {
               cc_index = _hierarchy[c_index][2];
               if (cc_index != -1 && _hierarchy[c_index][0] == -1
                   && _hierarchy[c_index][1] == -1
                   && _hierarchy[cc_index][0] == -1
                   && _hierarchy[cc_index][1] == -1) {
                   double area = contourArea(_contours[i]);
                   double area_c = contourArea(_contours[c_index]);
                   double area_cc = contourArea(_contours[cc_index]);
                   if (area < area_c*4 && area > area_c * 1.5 && area_c < area_cc*4 && area_c > area_cc*1.5) {
                       _finders.push_back(_contours[i]);
                       ++fp_count;
                     }
                  }
              }
      }
    cout<<"finder pattern size: "<<fp_count<<endl;
    //以下这一部分后续还需要设计一个qr的包装类，负责把qr这个类包装一下给用户使用，下边代码是暂时的
    //毕竟，qr调用它的辅助实现类有一定的流程，为了避免用户错误使用类函数，这样做是必须的。
    if (fp_count == 3) {
        _fpm->SetFps(_finders[0], _finders[1], _finders[2], _gray_img.cols, _gray_img.rows);
        _fpm->Rectify4Corners(_gray_img);
        _fpm->DrawArcs(_src_img);
        _fpm->DrawWhiteArcs(_mask_img);
        _box_size = _fpm->GetSizeOfBox();
        _rec_img.create(_box_size, _box_size, _gray_img.type());
        _recm_img.create(_box_size, _box_size, _gray_img.type());
        _res_img.create(_box_size/2, _box_size/2, _gray_img.type());
        //如果不对图像做全零操作，会发现图像有很多雪花！！！
        _recm_img = Scalar::all(0);
        _rec_img = Scalar::all(0);
        _res_img = Scalar::all(0);
        _fpm->Get4PtsForPerspectiveTransform(_src_pts[0], _src_pts[1], _src_pts[2], _src_pts[3]);
        _dst_pts[0] = Point2f(static_cast<float>(_box_size)/5, static_cast<float>(_box_size)/5);
        _dst_pts[1] = Point2f(static_cast<float>(_box_size)*4/5, static_cast<float>(_box_size)/5);
        _dst_pts[2] = Point2f(static_cast<float>(_box_size)/5, static_cast<float>(_box_size)*4/5);
        _dst_pts[3] = Point2f(static_cast<float>(_box_size)*4/5, static_cast<float>(_box_size)*4/5);
        /*
         * 此时得到的g_recImg为两边基本垂直的二维码图像
         * g_recMImg为得到的外轮廓掩码图像
         */
        PerformPerspectiveTransform(_gray_img, _rec_img, _src_pts, _dst_pts);
        PerformPerspectiveTransform(_mask_img, _recm_img, _src_pts, _dst_pts);
        BreakupAndRecombine(_rec_img, _recm_img, _res_img, _box_size);
        return true;
    } else {
        for (size_t i = 0; i < _finders.size(); ++i) {
            drawContours(_src_img, _finders, i, Scalar(0, 0, 255));
        }
    }
    return false;
}

void qr_cylinder::Qr::PerformPerspectiveTransform(const Mat &src, Mat &dst,
                                     const Point2f (&src_pts)[4], const Point2f (&dst_pts)[4])
{
    Mat wrap_mat = getPerspectiveTransform(src_pts, dst_pts);
    warpPerspective(src, dst, wrap_mat, dst.size());
}

/*
 * 下边的方法还是不行，两边总是窄一点，导致错误无法解码！！！
 * 采用了一点点补偿的方法，虽然这个并不是很好！！！
 */
void qr_cylinder::Qr::BreakupAndRecombine(const Mat &src, const Mat &mask, Mat &res, int boxSize)
{
    vector<Point> up_arcs, down_arcs;
    int x, y, i;
    uchar data, data1;
    Point tmp;
    for (x = _box_size/5+2; x < _box_size*4/5-1; ++x) {
        for (y = 0; y < _box_size; ++y) {
            data = mask.at<uchar>(y, x);
            if (data >= 10) {
                tmp = Point(x, y);
                while (data >= 10) {
                    ++y;
                    data1 = data;
                    data = mask.at<uchar>(y, x);
                    if (data1 < data)
                        tmp = Point(x, y);
                }

                break;
            }
        }
        up_arcs.push_back(tmp);
        for (y = _box_size-1; y >= 0; --y) {
            data = mask.at<uchar>(y, x);
            if (data >= 10) {
                tmp = Point(x, y);
                while (data >= 10) {
                    --y;
                    data1 = data;
                    data = mask.at<uchar>(y, x);
                    if (data1 < data)
                        tmp = Point(x, y);
                }
                break;
            }
        }
        down_arcs.push_back(tmp);
    }
    cout<<"boxSize: "<<_box_size<<endl;
    double up_arc_length = 0;
    double down_arc_length = 0;
    double t1, t2;
    int rec = _box_size/200;
    cout<<"rec: "<<rec<<endl;
    for (size_t i = 0; i < up_arcs.size()-1; ++i) {
        t1 = up_arcs[i+1].x - up_arcs[i].x;
        t2 = up_arcs[i+1].y - up_arcs[i].y;
        up_arc_length += sqrt(t1*t1+t2*t2);
    }
    for (size_t i = 0; i < down_arcs.size()-1; ++i) {
        t1 = down_arcs[i+1].x - down_arcs[i].x;
        t2 = down_arcs[i+1].y - down_arcs[i].y;
        down_arc_length += sqrt(t1*t1+t2*t2);
    }
    up_arc_length += rec*2;
    down_arc_length += rec*2;
    double single_arc_len = (up_arc_length+down_arc_length)/MASK_CUT_SIZE;
    vector<Point2f> up_pers_pts, down_pers_pts;
    up_pers_pts.push_back(Point2f(static_cast<float>(boxSize)/5, static_cast<float>(boxSize)/5));
    down_pers_pts.push_back(Point2f(static_cast<float>(boxSize)/5, static_cast<float>(boxSize)*4/5));
    double len_sum = 0, old_sum;
    size_t j , ss = up_arcs.size();
    int cur_index = 0;
    for (i = 1; i < MASK_CUT_SIZE; ++i) {
        for (j = cur_index; j < ss-1; ++j) {
            if (j == 0)
                len_sum += rec*2;
            old_sum = len_sum;
            t1 = up_arcs[j+1].x - up_arcs[j].x;
            t2 = up_arcs[j+1].y - up_arcs[j].y;
            len_sum += sqrt(t1*t1+t2*t2);
            t1 = down_arcs[j+1].x - down_arcs[j].x;
            t2 = down_arcs[j+1].y - down_arcs[j].y;
            len_sum += sqrt(t1*t1+t2*t2);
            if (len_sum >= single_arc_len) {
                if ((single_arc_len-old_sum) > (len_sum-single_arc_len)) {
                    up_pers_pts.push_back(up_arcs[j+1]);
                    down_pers_pts.push_back(down_arcs[j+1]);
                    cur_index = j+1;
                } else {
                    up_pers_pts.push_back(up_arcs[j]);
                    down_pers_pts.push_back(down_arcs[j]);
                    cur_index = j;
                }
                len_sum = 0;
                break;
            }
        }
    }
    up_pers_pts.push_back(Point2f(static_cast<float>(boxSize)*4/5, static_cast<float>(boxSize)/5));
    down_pers_pts.push_back(Point2f(static_cast<float>(boxSize)*4/5, static_cast<float>(boxSize)*4/5));

    vector<Mat> bMat;
    //使得m,n是MASK_CUT_SIZE的整数倍
    int m = (_box_size*3/5)/MASK_CUT_SIZE * MASK_CUT_SIZE;
    int n = m/MASK_CUT_SIZE;
    Point2f spts[4];
    Point2f dpts[4];
    dpts[0] = Point2f(0, 0);
    dpts[1] = Point2f(0, m);
    dpts[2] = Point2f(n, 0);
    dpts[3] = Point2f(n, m);
    for (i = 0; i < MASK_CUT_SIZE; ++i) {
        Mat img;
        img.create(m, n, CV_8UC1);
        img = Scalar::all(0);
        spts[0] = up_pers_pts[i];
        spts[1] = down_pers_pts[i];
        spts[2] = up_pers_pts[i+1];
        spts[3] = down_pers_pts[i+1];

        PerformPerspectiveTransform(src, img, spts, dpts);
        bMat.push_back(img);
    }

    res.create(m, m, CV_8UC1);
    for (size_t j = 0; j < bMat.size(); ++j) {
        for (int p = 0; p < m; ++p) {
            for (int q = 0; q < n; ++q) {
                res.at<uchar>(p, q+n*j) = bMat[j].at<uchar>(p, q);
            }
        }
    }
    threshold(res, res, 50, 255, CV_8UC1);
    medianBlur(res, res, _median_blur_size);
}



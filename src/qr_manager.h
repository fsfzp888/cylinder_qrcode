#ifndef QR_MANAGER_H
#define QR_MANAGER_H

#include <string>
#include "qr.h"

namespace qr_cylinder {
    class QrManager
    {
    public:
        explicit QrManager(int bs=49, int dt=0, int bbs=9);
        ~QrManager();
        void SetParas(int bs, int dt, int bbs);
        void ProcessImg(std::string file_name);
    private:
        Mat _src_img;
        int _bs, _dt, _bbs;
        Qr *_qr;
    };
}


#endif // QR_MANAGER_H

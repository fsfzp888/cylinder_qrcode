#include "qr_manager.h"

qr_cylinder::QrManager::QrManager(int bs, int dt, int bbs)
    :_bs(bs), _dt(dt), _bbs(bbs)
{
    _qr = new Qr();
}

qr_cylinder::QrManager::~QrManager()
{
    delete _qr;
}

void qr_cylinder::QrManager::SetParas(int bs, int dt, int bbs)
{
    _bs = bs;
    _dt = dt;
    _bbs = bbs;
}

void qr_cylinder::QrManager::ProcessImg(std::string file_name)
{
    _src_img = imread(file_name);
    _qr->SetBlurBlocksize(_bbs);
    _qr->SetThreshBlocksize(_bs);
    _qr->SetThreshDelta(_dt);
    bool res = _qr->Process(_src_img);
    if (res) {
        imshow("rec", _qr->GetRecImg());
        imshow("res", _qr->GetResImg());
    } else {
        imshow("src", _qr->GetSrcImg());
    }
}

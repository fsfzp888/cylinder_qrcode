#ifndef CYLINDERQR_H
#define CYLINDERQR_H

#include <QMainWindow>
#include <QLabel>
#include <QSpinBox>
#include <QString>
#include <QLineEdit>
#include <QPushButton>
#include "qr_manager.h"
using namespace qr_cylinder;

class CylinderQr : public QMainWindow
{
    Q_OBJECT
public:
    explicit CylinderQr(QWidget *parent = 0);
    virtual ~CylinderQr();

signals:

public slots:
    void OnBrowseBtnClicked();
    void OnRunBtnClicked();
    void OnBlurSizeValChg(int);
    void OnThreshBlockValChg(int);
    void onThreshDeltaValChg(int);
private:
    QrManager *_qrm;
    int _block_size;
    int _thresh_block_size;
    int _thresh_block_delta;
    QString _file_name;

    QLabel *_image_file_lb;
    QLineEdit *_image_file_le;
    QPushButton *_image_file_btn;
    QLabel *_blur_block_size_lb;
    QSpinBox *_blur_block_size_sb;
    QLabel *_thresh_block_size_lb;
    QSpinBox *_thresh_block_size_sb;
    QLabel *_thresh_block_delta_lb;
    QSpinBox *_thresh_block_delta_sb;
    QPushButton *_run_btn;
};

#endif // CYLINDERQR_H

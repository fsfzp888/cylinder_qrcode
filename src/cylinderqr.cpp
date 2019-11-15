#include <QHBoxLayout>
#include <QVBoxLayout>
#include <QFileDialog>
#include "cylinderqr.h"

CylinderQr::CylinderQr(QWidget *parent) : QMainWindow(parent)
{
    _qrm = new QrManager;

    _image_file_lb = new QLabel("Src Qr Image");
    _image_file_le = new QLineEdit;
    _image_file_btn = new QPushButton("Browse");
    QHBoxLayout *image_hlayout = new QHBoxLayout;
    image_hlayout->addWidget(_image_file_lb);
    image_hlayout->addWidget(_image_file_le);
    image_hlayout->addWidget(_image_file_btn);

    _blur_block_size_lb = new QLabel("Blur Block Size");
    _blur_block_size_sb = new QSpinBox;
    _blur_block_size_sb->setSingleStep(2);
    _blur_block_size_sb->setRange(1, 99);
    _blur_block_size_sb->setValue(9);
    _block_size = 9;
    QHBoxLayout *blur_hlayout = new QHBoxLayout;
    blur_hlayout->addWidget(_blur_block_size_lb);
    blur_hlayout->addWidget(_blur_block_size_sb);

    _thresh_block_size_lb = new QLabel("Thresh Block Size");
    _thresh_block_size_sb = new QSpinBox;
    _thresh_block_size_sb->setSingleStep(2);
    _thresh_block_size_sb->setRange(1, 99);
    _thresh_block_size_sb->setValue(49);
    _thresh_block_size = 49;
    QHBoxLayout *threshb_hlayout = new QHBoxLayout;
    threshb_hlayout->addWidget(_thresh_block_size_lb);
    threshb_hlayout->addWidget(_thresh_block_size_sb);

    _thresh_block_delta_lb = new QLabel("Thresh Block Delta");
    _thresh_block_delta_sb = new QSpinBox;
    _thresh_block_delta_sb->setRange(0, 20);
    _thresh_block_delta_sb->setValue(0);
    _thresh_block_delta = 0;
    QHBoxLayout *threshd_hlayout = new QHBoxLayout;
    threshd_hlayout->addWidget(_thresh_block_delta_lb);
    threshd_hlayout->addWidget(_thresh_block_delta_sb);

    _run_btn = new QPushButton("Run");

    QVBoxLayout *main_layout = new QVBoxLayout;
    main_layout->addLayout(image_hlayout);
    main_layout->addLayout(blur_hlayout);
    main_layout->addLayout(threshb_hlayout);
    main_layout->addLayout(threshd_hlayout);
    main_layout->addWidget(_run_btn);

    connect(_image_file_btn, SIGNAL(clicked(bool)), this, SLOT(OnBrowseBtnClicked()));
    connect(_run_btn, SIGNAL(clicked(bool)), this, SLOT(OnRunBtnClicked()));
    connect(_blur_block_size_sb, SIGNAL(valueChanged(int)), this, SLOT(OnBlurSizeValChg(int)));
    connect(_thresh_block_size_sb, SIGNAL(valueChanged(int)), this, SLOT(OnThreshBlockValChg(int)));
    connect(_thresh_block_delta_sb, SIGNAL(valueChanged(int)), this, SLOT(onThreshDeltaValChg(int)));

    QWidget *widget = new QWidget;
    widget->setLayout(main_layout);
    setCentralWidget(widget);
}

CylinderQr::~CylinderQr()
{
    delete _qrm;
}

void CylinderQr::OnBrowseBtnClicked()
{
    _file_name = QFileDialog::getOpenFileName(this, "/home", "Images(*.png *.jpg)");
    _image_file_le->setText(_file_name);
}

void CylinderQr::OnRunBtnClicked()
{
    if (!QFile::exists(_file_name))
    {
        return;
    }
    _qrm->SetParas(_thresh_block_size, _thresh_block_delta, _block_size);
    _qrm->ProcessImg(_file_name.toStdString());
}

void CylinderQr::OnBlurSizeValChg(int val)
{
    _block_size = val;
}

void CylinderQr::OnThreshBlockValChg(int val)
{
    _thresh_block_size = val;
}

void CylinderQr::onThreshDeltaValChg(int val)
{
    _thresh_block_delta = val;
}

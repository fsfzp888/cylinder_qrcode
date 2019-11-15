#include <iostream>
#include <QApplication>
#include "cylinderqr.h"

int main(int argc, char **argv)
{
    QApplication app(argc, argv);
    CylinderQr *qr = new CylinderQr;
    qr->show();
    return app.exec();
}

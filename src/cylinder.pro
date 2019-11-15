TEMPLATE = app
QT       += core gui
CONFIG   += c++11
greaterThan(QT_MAJOR_VERSION, 4): QT += widgets

SOURCES += main.cpp \
    arc.cpp \
    finder_pattern.cpp \
    fp_manager.cpp \
    qr.cpp \
    qr_manager.cpp \
    cylinderqr.cpp

HEADERS += \
    arc.h \
    finder_pattern.h \
    fp_manager.h \
    qr.h \
    qr_manager.h \
    cylinderqr.h


INCLUDEPATH += /usr/local/include

#CONFIG += link_pkgconfig
#PKGCONFIG = opencv

LIBS += `pkg-config opencv --cflags --libs` -Wl,--rpath=/usr/local/lib
#LIBS += `pkg-config --libs opencv`


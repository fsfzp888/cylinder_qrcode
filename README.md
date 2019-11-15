# Cylinder Qr Code Retifier Demo Example

------

Gitee:
https://gitee.com/fsfzp888/cylinder_qrcode

This is a demo project using OPENCV to retify QR code on cylinder surface, C++ and android project code are provided. Although I don't think it's useful, it's still a good learning material for OPENCV.

This project try to retify the qr code on cylinder surface, so that normal scanner could recognize the distorted qr code. However, there are still many problems here. I may not fix them in the future.

System: Ubuntu 16.04

Building Step:

```bash
sudo apt install -y qt5-default qtcreator cmake cmake-gui
sudo apt install -y build-essential libgtk2.0-dev libavcodec-dev
sudo apt install -y libavformat-dev libjpeg.dev libtiff4.dev libswscale-dev libjasper-dev

cd cylinder_qrcode/opencv
tar -Jxvf opencv-3.4.1.tar.xz -C .
mkdir opencv_build
cd opencv_build
cmake -D CMAKE_BUILD_TYPE=Release -D CMAKE_INSTALL_PREFIX=/usr/local ../opencv-3.4.1

make -j4
sudo make install
```
Then the opencv3 and qt5 would be installed in your system successfully.
After installing the requirements, cd to the source directory to build the source code:
```bash
cd cylinder_qrcode/src
qmake
make
./cylinder
```

You would see the following simple qt GUI now:

![main](https://gitee.com/uploads/images/2018/0321/221444_59a3f48e_1256822.png "main.png")

Click Browse to select qr code picture in cylinder_qrcode/datas, and press run, then the result may be:

![res](https://gitee.com/uploads/images/2018/0321/221454_72fa7309_1256822.png "res.png")

The result picture may not seen to be great sometimes, so you may need to change the following parameters
to get better result:

> * Blur Block Size
> * Thresh Block Size
> * Thresh Block Delta

Detail documentation could be found in:
https://gitee.com/fsfzp888/cylinder_qrcode/tree/master/docs


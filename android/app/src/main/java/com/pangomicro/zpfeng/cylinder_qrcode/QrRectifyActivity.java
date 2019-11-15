package com.pangomicro.zpfeng.cylinder_qrcode;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.datamatrix.DataMatrixReader;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;

public class QrRectifyActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "OCVSample::Activity";
    private ImageView imageView;
    private Bitmap bmp;
    private Mat srcImg, grayImg, colorImg;
    private Button btnNext;
    private TextView textView;
    private static Qr qr = new Qr();
    private static int i;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_rectify);
        i = 0;
        imageView=(ImageView)findViewById(R.id.qrImgView);
//        Intent intent=getIntent();
//        if(intent!=null)
//        {
//            bmp=intent.getParcelableExtra("bitmap");
//            imageView.setImageBitmap(bmp);
//        }
        srcImg = IntentMat.intentMat.clone();
        grayImg = new Mat(srcImg.rows(), srcImg.cols(), CvType.CV_8UC1);
        Imgproc.cvtColor(srcImg, grayImg, Imgproc.COLOR_BGR2GRAY);
        if (qr.process(srcImg)) {
            qr.drawArcs(srcImg);
//            qr.drawFps(srcImg);
        }
        bmp = Bitmap.createBitmap(srcImg.width(), srcImg.height(),
                Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(srcImg, bmp);
        imageView.setImageBitmap(bmp);
        btnNext = (Button)findViewById(R.id.btnNext);
        btnNext.setOnClickListener(this);
//        textView = (TextView)findViewById(R.id.tvQrResult);
    }

    @Override
    public void onClick(View v) {
        switch (i) {
            case 0:
                if (isProcess)
                    return;
                isProcess = true;
                if (!qr.rectifyParabola(grayImg)) {
                    Log.e(TAG, "parabola Rectification Error");
                }
                srcImg = grayImg.clone();
                qr.drawArcs(srcImg);
                bmp = Bitmap.createBitmap(srcImg.width(), srcImg.height(),
                        Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(srcImg, bmp);
                imageView.setImageBitmap(bmp);
                btnNext.setText(R.string.perspectiveRectify);
                ++i;
                isProcess = false;
                break;
            case 1:
                if (isProcess)
                    return;
                isProcess = true;
                srcImg = qr.performPerspectiveTransform(grayImg);
                bmp = Bitmap.createBitmap(srcImg.width(), srcImg.height(),
                        Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(srcImg, bmp);
                imageView.setImageBitmap(bmp);
                btnNext.setText(R.string.multiPerspectiveRectify);
                ++i;
                isProcess = false;
                break;
            case 2:
                if (isProcess)
                    return;
                isProcess = true;
                srcImg = qr.breakUpAndRecombine(grayImg);
                bmp = Bitmap.createBitmap(srcImg.width(), srcImg.height(),
                        Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(srcImg, bmp);
                imageView.setImageBitmap(bmp);
//                btnNext.setText(R.string.TryQrDecode);
//                ++i;
                isProcess = false;
                break;
            case 3:
                if (isProcess)
                    return;
                isProcess = true;
//                bmp = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
//                Imgproc.cvtColor(srcImg, srcImg, Imgproc.COLOR_RGB2GRAY);
                if (srcImg == null) {
                    Log.e(TAG, "Error srcImg is null!!!");
                    return;
                }
                colorImg = new Mat(srcImg.rows(), srcImg.cols(), CvType.CV_8UC3);
                Imgproc.cvtColor(srcImg, colorImg, Imgproc.COLOR_GRAY2RGB);
                Utils.matToBitmap(colorImg, bmp);
                imageView.setImageBitmap(bmp);
//                btnNext.setText(R.string.TryQrDecode);
//                ++i;
                isProcess = false;
                break;
//                String res = QRCodeUtils.getStringFromQrCodeBitmap(imageView.getDrawable());
//                int[] intArray = new int[bmp.getWidth()*bmp.getHeight()];
//                bmp.getPixels(intArray, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
//
//                LuminanceSource source = new RGBLuminanceSource(bmp.getWidth(),
//                        bmp.getHeight(),intArray);
//                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
////                Hashtable<DecodeHintType, Object> hints = new Hashtable<>();
////                hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
////                hints.put(DecodeHintType.POSSIBLE_FORMATS, BarcodeFormat.QR_CODE);
////                reader.setHints(hints);
//                try {
//                    Result result = reader.decodeWithState(bitmap);
//                    String res = result.getText();
//                    textView.setText(res);
//                } catch (ReaderException e) {
//                    e.printStackTrace();
//                    Log.e(TAG, "Error decode!!!");
//                } finally {
//                    reader.reset();
//                }
//                isProcess = false;
//                break;
            default:
                if (isProcess)
                    return;
                isProcess = true;
                i = 0;
                btnNext.setText(R.string.parabolaRectify);
                isProcess = false;
                break;
        }
    }
    private MultiFormatReader reader = new MultiFormatReader();
    private boolean isProcess;
}

package com.example.ocr;

import android.graphics.Bitmap;

public class ImageUtils {

    private Bitmap bitmap;
    private static ImageUtils sInstance;

    public static ImageUtils getInstance(){
        if (sInstance == null){
            sInstance = new ImageUtils();
        }

        return sInstance;
    }


    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

}

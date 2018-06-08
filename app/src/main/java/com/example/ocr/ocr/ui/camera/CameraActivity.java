package com.example.ocr.ocr.ui.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextPaint;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ocr.ImageUtils;
import com.example.ocr.R;
import com.example.ocr.ocr.idcardquality.IDcardQualityProcess;
import com.example.ocr.ocr.ui.camera.view.CameraView;
import com.example.ocr.ocr.ui.camera.view.MaskView;
import com.example.ocr.ocr.ui.camera.view.OCRCameraLayout;
import com.example.ocr.ocr.ui.crop.CropView;
import com.example.ocr.ocr.ui.crop.FrameOverlayView;
import com.example.ocr.ocr.ui.util.SystemUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraActivity extends Activity {

    public static final String KEY_OUTPUT_FILE_PATH = "outputFilePath";
    public static final String KEY_CONTENT_TYPE = "contentType";
    public static final String KEY_NATIVE_TOKEN = "nativeToken";
    public static final String KEY_NATIVE_ENABLE = "nativeEnable";
    public static final String KEY_NATIVE_MANUAL = "nativeEnableManual";

    public static final String CONTENT_TYPE_GENERAL = "general";
    public static final String CONTENT_TYPE_ID_CARD_FRONT = "IDCardFront";
    public static final String CONTENT_TYPE_ID_CARD_BACK = "IDCardBack";
    public static final String CONTENT_TYPE_BANK_CARD = "bankCard";
    public static final String CONTENT_TYPE_PASSPORT = "passport";

    private static final int REQUEST_CODE_PICK_IMAGE = 100;
    private static final int PERMISSIONS_REQUEST_CAMERA = 800;
    private static final int PERMISSIONS_EXTERNAL_STORAGE = 801;

    private File outputFile;
    private String contentType;
    private Handler handler = new Handler();

    private boolean isNativeEnable;
    private boolean isNativeManual;

    private OCRCameraLayout cropContainer;
    //    private OCRCameraLayout takePictureContainer;
    private RelativeLayout takePictureContainer;
    private OCRCameraLayout confirmResultContainer;
    private ImageView lightButton; // 闪光灯
    private CameraView cameraView;
    private ImageView displayImageView;
    private CropView cropView;
    private FrameOverlayView overlayView;
    private MaskView cropMaskView;
    //    private ImageView takePhotoBtn;
    private TextView takePhotoBtn; // 拍照

    private HorizontalScrollView mHorizontalScrollView;
    private View mLayoutTitle; // 标题栏
    private View mLayoutLabel; // 副标题栏
    private ImageView mBackPressed; // 标题返回键

    public static final String TAG = "CameraActivity";

    private PermissionCallback permissionCallback = new PermissionCallback() {
        @Override
        public boolean onRequestPermission() {
            ActivityCompat.requestPermissions(CameraActivity.this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSIONS_REQUEST_CAMERA);
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //隐藏状态栏
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //显示状态栏
        SystemUtils.changeWindow(this, getResources().getColor(R.color.colorYellow));

        setContentView(R.layout.bd_ocr_activity_camera);

        initView();
        initEvent();
        mLayoutLabel = findViewById(R.id.layout_photo_shot);
        mLayoutTitle = findViewById(R.id.toolbar);
        mBackPressed = findViewById(R.id.iv_backPressed);
        mBackPressed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CameraActivity.this.finish();
            }
        });
//        takePictureContainer = (OCRCameraLayout) findViewById(R.id.take_picture_container);
        takePictureContainer = findViewById(R.id.take_picture_container);

        confirmResultContainer = (OCRCameraLayout) findViewById(R.id.confirm_result_container);
        cameraView = (CameraView) findViewById(R.id.camera_view);
        cameraView.getCameraControl().setPermissionCallback(permissionCallback);
        lightButton = (ImageView) findViewById(R.id.light_button);
        lightButton.setOnClickListener(lightButtonOnClickListener);
//        takePhotoBtn = (ImageView) findViewById(R.id.take_photo_button);

        mHorizontalScrollView = findViewById(R.id.horizontalScrollView);
        takePhotoBtn = (TextView) findViewById(R.id.take_photo_button);
        TextPaint paint = takePhotoBtn.getPaint();
        paint.setFlags(Paint.UNDERLINE_TEXT_FLAG); //下划线
        paint.setAntiAlias(true); //抗锯齿
        // 去除滑到尽头时的阴影
        mHorizontalScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        // 水平方向的水平滚动条是否显示
        mHorizontalScrollView.setHorizontalScrollBarEnabled(false);

        findViewById(R.id.album_button).setOnClickListener(albumButtonOnClickListener);
        takePhotoBtn.setOnClickListener(takeButtonOnClickListener);

        // confirm result;
        displayImageView = (ImageView) findViewById(R.id.display_image_view);
        confirmResultContainer.findViewById(R.id.confirm_button).setOnClickListener(confirmButtonOnClickListener);
        confirmResultContainer.findViewById(R.id.cancel_button).setOnClickListener(confirmCancelButtonOnClickListener);
        findViewById(R.id.rotate_button).setOnClickListener(rotateButtonOnClickListener);

        cropView = (CropView) findViewById(R.id.crop_view);
//        cropContainer = (OCRCameraLayout) findViewById(R.id.crop_container);
        cropContainer = findViewById(R.id.crop_container);
        overlayView = (FrameOverlayView) findViewById(R.id.overlay_view);
        cropContainer.findViewById(R.id.confirm_button).setOnClickListener(cropConfirmButtonListener);
        cropMaskView = (MaskView) cropContainer.findViewById(R.id.crop_mask_view);
        cropContainer.findViewById(R.id.cancel_button).setOnClickListener(cropCancelButtonListener);

        setOrientation(getResources().getConfiguration());
        initParams();

        cameraView.setAutoPictureCallback(autoTakePictureCallback);
    }

    private TextView mTitleOne, mTitleTwo, mTitleThree; // 副标题
    private LinearLayout mTableOne, mTableTwo, mTableThree, mTableFour, mTableFive;
    // 下面标签
    private TextView mTitleVin, mTitleLicense, mTitleSingle, mTitleKeys, mTitleCard;
    private ImageView mImgVin, mImgLicense, mImgSingle, mImgKeys, mImgCard;
    private TextView mHintText; // 提示文本信息
    private ImageView mHintImage; // 提示图片
    private boolean isLicenseSelect = true; // 是否是行驶证标签
    private boolean isCardSelect; // 是否是登记证标签

    private void initView() {
        mTitleOne = findViewById(R.id.tv_title_one);
        mTitleTwo = findViewById(R.id.tv_title_two);
        mTitleThree = findViewById(R.id.tv_title_three);

        TextPaint paint1 = mTitleOne.getPaint();
        paint1.setFlags(Paint.UNDERLINE_TEXT_FLAG); //下划线
        paint1.setAntiAlias(true); //抗锯齿

        TextPaint paint2 = mTitleTwo.getPaint();
        paint2.setFlags(Paint.UNDERLINE_TEXT_FLAG); //下划线
        paint2.setAntiAlias(true); //抗锯齿

        TextPaint paint3 = mTitleThree.getPaint();
        paint3.setFlags(Paint.UNDERLINE_TEXT_FLAG); //下划线
        paint3.setAntiAlias(true); //抗锯齿

        mTableOne = findViewById(R.id.ll_table_one);
        mTableTwo = findViewById(R.id.ll_table_two);
        mTableThree = findViewById(R.id.ll_table_three);
        mTableFour = findViewById(R.id.ll_table_four);
        mTableFive = findViewById(R.id.ll_table_five);

        mTitleVin = findViewById(R.id.tv_table_one);
        mTitleLicense = findViewById(R.id.tv_table_two);
        mTitleSingle = findViewById(R.id.tv_table_three);
        mTitleKeys = findViewById(R.id.tv_table_four);
        mTitleCard = findViewById(R.id.tv_table_five);

        mImgVin = findViewById(R.id.iv_icon_one);
        mImgLicense = findViewById(R.id.iv_icon_two);
        mImgSingle = findViewById(R.id.iv_icon_three);
        mImgKeys = findViewById(R.id.iv_icon_four);
        mImgCard = findViewById(R.id.iv_icon_five);

        mHintText = findViewById(R.id.hint_take_photo);
        mHintImage = findViewById(R.id.iv_shot_photo);
    }

    private void initEvent() {
        mTitleOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTitleTextColor(1);
                setTitleHintImage(1);
                setHintText(1);
            }
        });
        mTitleTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTitleTextColor(2);
                setTitleHintImage(2);
                setHintText(2);
            }
        });
        mTitleThree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTitleTextColor(3);
                setTitleHintImage(3);
                setHintText(3);
            }
        });

        mTableOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTableTextColor(1);
                setHintImage(0x1);
                setHintText(0x10);
            }
        });

        mTableTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTableTextColor(2);
                setHintImage(0x2);
                setHintText(0x20);
            }
        });

        mTableThree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTableTextColor(3);
                setHintImage(0x3);
                setHintText(0x30);
            }
        });

        mTableFour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTableTextColor(4);
                setHintImage(0x4);
                setHintText(0x40);
            }
        });

        mTableFive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTableTextColor(5);
                setHintImage(0x5);
                setHintText(0x50);
            }
        });
    }

    // 设置副标题字体颜色
    private void setTitleTextColor(int position) {
        if (position == 1) {
            mTitleOne.setTextColor(getResources().getColor(R.color.colorYellow));
            mTitleTwo.setTextColor(getResources().getColor(R.color.colorWhite));
            mTitleThree.setTextColor(getResources().getColor(R.color.colorWhite));
        } else if (position == 2) {
            mTitleOne.setTextColor(getResources().getColor(R.color.colorWhite));
            mTitleTwo.setTextColor(getResources().getColor(R.color.colorYellow));
            mTitleThree.setTextColor(getResources().getColor(R.color.colorWhite));
        } else if (position == 3) {
            mTitleOne.setTextColor(getResources().getColor(R.color.colorWhite));
            mTitleTwo.setTextColor(getResources().getColor(R.color.colorWhite));
            mTitleThree.setTextColor(getResources().getColor(R.color.colorYellow));
        }
    }

    // 设置标签文本更新
    private void setTableTextColor(int position) {
        setHintLayout(position);
        switch (position) {
            case 1:
                mTitleVin.setTextColor(getResources().getColor(R.color.colorYellow));
                mTitleLicense.setTextColor(getResources().getColor(R.color.colorWhite));
                mTitleSingle.setTextColor(getResources().getColor(R.color.colorWhite));
                mTitleKeys.setTextColor(getResources().getColor(R.color.colorWhite));
                mTitleCard.setTextColor(getResources().getColor(R.color.colorWhite));
                mImgVin.setImageResource(R.mipmap.title_vin_on);
                mImgLicense.setImageResource(R.mipmap.title_xingshizheng_off);
                mImgSingle.setImageResource(R.mipmap.title_baoxiandan_off);
                mImgKeys.setImageResource(R.mipmap.title_keys_off);
                mImgCard.setImageResource(R.mipmap.title_dengji_off);
                break;
            case 2:
                mTitleVin.setTextColor(getResources().getColor(R.color.colorWhite));
                mTitleLicense.setTextColor(getResources().getColor(R.color.colorYellow));
                mTitleSingle.setTextColor(getResources().getColor(R.color.colorWhite));
                mTitleKeys.setTextColor(getResources().getColor(R.color.colorWhite));
                mTitleCard.setTextColor(getResources().getColor(R.color.colorWhite));
                mImgVin.setImageResource(R.mipmap.title_vin_off);
                mImgLicense.setImageResource(R.mipmap.title_xingshizheng_on);
                mImgSingle.setImageResource(R.mipmap.title_baoxiandan_off);
                mImgKeys.setImageResource(R.mipmap.title_keys_off);
                mImgCard.setImageResource(R.mipmap.title_dengji_off);
                break;
            case 3:
                mTitleVin.setTextColor(getResources().getColor(R.color.colorWhite));
                mTitleLicense.setTextColor(getResources().getColor(R.color.colorWhite));
                mTitleSingle.setTextColor(getResources().getColor(R.color.colorYellow));
                mTitleKeys.setTextColor(getResources().getColor(R.color.colorWhite));
                mTitleCard.setTextColor(getResources().getColor(R.color.colorWhite));
                mImgVin.setImageResource(R.mipmap.title_vin_off);
                mImgLicense.setImageResource(R.mipmap.title_xingshizheng_off);
                mImgSingle.setImageResource(R.mipmap.title_baoxiandan_on);
                mImgKeys.setImageResource(R.mipmap.title_keys_off);
                mImgCard.setImageResource(R.mipmap.title_dengji_off);
                break;
            case 4:
                mTitleVin.setTextColor(getResources().getColor(R.color.colorWhite));
                mTitleLicense.setTextColor(getResources().getColor(R.color.colorWhite));
                mTitleSingle.setTextColor(getResources().getColor(R.color.colorWhite));
                mTitleKeys.setTextColor(getResources().getColor(R.color.colorYellow));
                mTitleCard.setTextColor(getResources().getColor(R.color.colorWhite));
                mImgVin.setImageResource(R.mipmap.title_vin_off);
                mImgLicense.setImageResource(R.mipmap.title_xingshizheng_off);
                mImgSingle.setImageResource(R.mipmap.title_baoxiandan_off);
                mImgKeys.setImageResource(R.mipmap.title_keys_on);
                mImgCard.setImageResource(R.mipmap.title_dengji_off);
                break;
            case 5:
                mTitleVin.setTextColor(getResources().getColor(R.color.colorWhite));
                mTitleLicense.setTextColor(getResources().getColor(R.color.colorWhite));
                mTitleSingle.setTextColor(getResources().getColor(R.color.colorWhite));
                mTitleKeys.setTextColor(getResources().getColor(R.color.colorWhite));
                mTitleCard.setTextColor(getResources().getColor(R.color.colorYellow));
                mImgVin.setImageResource(R.mipmap.title_vin_off);
                mImgLicense.setImageResource(R.mipmap.title_xingshizheng_off);
                mImgSingle.setImageResource(R.mipmap.title_baoxiandan_off);
                mImgKeys.setImageResource(R.mipmap.title_keys_off);
                mImgCard.setImageResource(R.mipmap.title_dengji_on);
                break;
            default:
                mTitleVin.setTextColor(getResources().getColor(R.color.colorWhite));
                mTitleLicense.setTextColor(getResources().getColor(R.color.colorYellow));
                mTitleSingle.setTextColor(getResources().getColor(R.color.colorWhite));
                mTitleKeys.setTextColor(getResources().getColor(R.color.colorWhite));
                mTitleCard.setTextColor(getResources().getColor(R.color.colorWhite));
                mImgVin.setImageResource(R.mipmap.title_vin_off);
                mImgLicense.setImageResource(R.mipmap.title_xingshizheng_on);
                mImgSingle.setImageResource(R.mipmap.title_baoxiandan_off);
                mImgKeys.setImageResource(R.mipmap.title_keys_off);
                mImgCard.setImageResource(R.mipmap.title_dengji_off);
                break;
        }
    }

    // 设置副标题布局显示
    private void setHintLayout(int position) {
        if (position == 2) {
            mLayoutLabel.setVisibility(View.VISIBLE);
            mTitleOne.setText("行驶证主页");
            mTitleTwo.setText("行驶证副页");
            mTitleThree.setText("行驶证主页背面");
        } else if (position == 5) {
            mLayoutLabel.setVisibility(View.VISIBLE);
            mTitleOne.setText("登记证第1页");
            mTitleTwo.setText("登记证第2页");
            mTitleThree.setText("登记证最后页");
        } else {
            mLayoutLabel.setVisibility(View.INVISIBLE);
        }
    }

    // 设置提示图片
    private void setHintImage(int position) {
        switch (position) {
            case 0x1:
                isLicenseSelect = false;
                isCardSelect = false;
                mHintImage.setVisibility(View.VISIBLE);
                mHintImage.setImageResource(R.mipmap.vin);
                break;
            case 0x2:
                isLicenseSelect = true;
                isCardSelect = false;
                mHintImage.setVisibility(View.VISIBLE);
                break;
            case 0x3:
                isLicenseSelect = false;
                isCardSelect = false;
                mHintImage.setVisibility(View.VISIBLE);
                mHintImage.setImageResource(R.mipmap.baoxiandan);
                break;
            case 0x4:
                isLicenseSelect = false;
                isCardSelect = false;
                mHintImage.setVisibility(View.VISIBLE);
                mHintImage.setImageResource(R.mipmap.car_keys);
                break;
            case 0x5:
                isLicenseSelect = false;
                isCardSelect = true;
                mHintImage.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void setTitleHintImage(int position) {
        if (isLicenseSelect && !isCardSelect) {
            if (position == 1) {
                mHintImage.setImageResource(R.mipmap.xingshizheng);
            } else if (position == 2) {
                mHintImage.setImageResource(R.mipmap.xingshizheng_fu);
            } else if (position == 3) {
                mHintImage.setImageResource(R.mipmap.xingshizheng_bei);
            }
        }

        if (!isLicenseSelect && isCardSelect) {
            if (position == 1) {
                mHintImage.setImageResource(R.mipmap.car_dengji_1);
            } else if (position == 2) {
                mHintImage.setImageResource(R.mipmap.car_dengji_2);
            } else if (position == 3) {
                mHintImage.setImageResource(R.mipmap.car_dengji_3);
            }
        }
    }

    // 设置提示文本
    private void setHintText(int position) {
        String hintMsg = null;
        switch (position) {
            case 0x10:
                hintMsg = "";
                break;
            case 1:
                hintMsg = "请对准行驶证主页进行拍照";
                break;
            case 2:
                hintMsg = "请对准行驶证副页进行拍照";
                break;
            case 3:
                hintMsg = "请对准行驶证主页背面进行拍照";
                break;
            case 0x20:
                hintMsg = "请对准行驶证主页进行拍照";
                break;
            case 0x30:
                hintMsg = "请对准保险单进行拍照";
                break;
            case 0x40:
                hintMsg = "请对准车钥匙进行拍照";
                break;
            case 0x50:
                hintMsg = "请对准车辆登记证进行拍照";
                break;
            default:
                hintMsg = "";
                break;
        }

        mHintText.setText(hintMsg);
    }


    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    private void initParams() {
        String outputPath = getIntent().getStringExtra(KEY_OUTPUT_FILE_PATH);
        final String token = getIntent().getStringExtra(KEY_NATIVE_TOKEN);
        isNativeEnable = getIntent().getBooleanExtra(KEY_NATIVE_ENABLE, true);
        isNativeManual = getIntent().getBooleanExtra(KEY_NATIVE_MANUAL, false);

        if (token == null && !isNativeManual) {
            isNativeEnable = false;
        }

        if (outputPath != null) {
            outputFile = new File(outputPath);
        }

        contentType = getIntent().getStringExtra(KEY_CONTENT_TYPE);
        if (contentType == null) {
            contentType = CONTENT_TYPE_GENERAL;
        }
        int maskType;
        switch (contentType) {
            case CONTENT_TYPE_ID_CARD_FRONT:
                maskType = MaskView.MASK_TYPE_ID_CARD_FRONT;
                overlayView.setVisibility(View.INVISIBLE);
                if (isNativeEnable) {
                    takePhotoBtn.setVisibility(View.INVISIBLE);
                }
                break;
            case CONTENT_TYPE_ID_CARD_BACK:
                maskType = MaskView.MASK_TYPE_ID_CARD_BACK;
                overlayView.setVisibility(View.INVISIBLE);
                if (isNativeEnable) {
                    takePhotoBtn.setVisibility(View.INVISIBLE);
                }
                break;
            case CONTENT_TYPE_BANK_CARD:
                maskType = MaskView.MASK_TYPE_BANK_CARD;
                overlayView.setVisibility(View.INVISIBLE);
                break;
            case CONTENT_TYPE_PASSPORT:
                maskType = MaskView.MASK_TYPE_PASSPORT;
                overlayView.setVisibility(View.INVISIBLE);
                break;
            case CONTENT_TYPE_GENERAL:
            default:
                maskType = MaskView.MASK_TYPE_NONE;
                cropMaskView.setVisibility(View.INVISIBLE);
                break;
        }

        // 身份证本地能力初始化
        if (maskType == MaskView.MASK_TYPE_ID_CARD_FRONT || maskType == MaskView.MASK_TYPE_ID_CARD_BACK) {
            if (isNativeEnable && !isNativeManual) {
                initNative(token);
            }
        }
        cameraView.setEnableScan(isNativeEnable);
        cameraView.setMaskType(maskType, this);
        cropMaskView.setMaskType(maskType);
    }

    private void initNative(final String token) {
        CameraNativeHelper.init(CameraActivity.this, token,
                new CameraNativeHelper.CameraNativeInitCallback() {
                    @Override
                    public void onError(int errorCode, Throwable e) {
                        cameraView.setInitNativeStatus(errorCode);
                    }
                });
    }

    private void showTakePicture() {
        cameraView.getCameraControl().resume();
        updateFlashMode();
        takePictureContainer.setVisibility(View.VISIBLE);
        confirmResultContainer.setVisibility(View.INVISIBLE);
        cropContainer.setVisibility(View.INVISIBLE);

        mLayoutTitle.setVisibility(View.VISIBLE);

        Log.e(TAG, "showTakePicture()");
    }

    private void showCrop() {
        cameraView.getCameraControl().pause();
        updateFlashMode();
        takePictureContainer.setVisibility(View.INVISIBLE);
        confirmResultContainer.setVisibility(View.INVISIBLE);
        cropContainer.setVisibility(View.VISIBLE);

        mLayoutTitle.setVisibility(View.INVISIBLE);

        Log.e(TAG, "showCrop()");
    }

    private void showResultConfirm() {
        cameraView.getCameraControl().pause();
        updateFlashMode();
        takePictureContainer.setVisibility(View.INVISIBLE);
        confirmResultContainer.setVisibility(View.VISIBLE);
        cropContainer.setVisibility(View.INVISIBLE);

        mLayoutTitle.setVisibility(View.INVISIBLE);
        Log.e(TAG, "showResultConfirm()");
    }

    // take photo;
    private void updateFlashMode() {
        int flashMode = cameraView.getCameraControl().getFlashMode();
        if (flashMode == ICameraControl.FLASH_MODE_TORCH) {
            lightButton.setImageResource(R.mipmap.bd_ocr_light_on);
        } else {
            lightButton.setImageResource(R.mipmap.bd_ocr_light_off);
        }
    }

    private View.OnClickListener albumButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ActivityCompat.requestPermissions(CameraActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            PERMISSIONS_EXTERNAL_STORAGE);
                    return;
                }
            }
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
        }
    };

    private View.OnClickListener lightButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (cameraView.getCameraControl().getFlashMode() == ICameraControl.FLASH_MODE_OFF) {
                cameraView.getCameraControl().setFlashMode(ICameraControl.FLASH_MODE_TORCH);
            } else {
                cameraView.getCameraControl().setFlashMode(ICameraControl.FLASH_MODE_OFF);
            }
            updateFlashMode();
        }
    };

    private View.OnClickListener takeButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            cameraView.takePicture(outputFile, takePictureCallback);
        }
    };

    private CameraView.OnTakePictureCallback autoTakePictureCallback = new CameraView.OnTakePictureCallback() {
        @Override
        public void onPictureTaken(final Bitmap bitmap) {
            CameraThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                        bitmap.recycle();
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Intent intent = new Intent();
                    intent.putExtra(CameraActivity.KEY_CONTENT_TYPE, contentType);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
            });
        }
    };

    private CameraView.OnTakePictureCallback takePictureCallback = new CameraView.OnTakePictureCallback() {
        @Override
        public void onPictureTaken(final Bitmap bitmap) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    takePictureContainer.setVisibility(View.INVISIBLE);
                    if (cropMaskView.getMaskType() == MaskView.MASK_TYPE_NONE) {
                        cropView.setFilePath(outputFile.getAbsolutePath());
                        showCrop();
                    } else if (cropMaskView.getMaskType() == MaskView.MASK_TYPE_BANK_CARD) {
                        cropView.setFilePath(outputFile.getAbsolutePath());
                        cropMaskView.setVisibility(View.INVISIBLE);
                        overlayView.setVisibility(View.VISIBLE);
                        overlayView.setTypeWide();
                        showCrop();
                    } else {
                        displayImageView.setImageBitmap(bitmap);
                        showResultConfirm();
                    }
                }
            });
        }
    };

    private View.OnClickListener cropCancelButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // 释放 cropView中的bitmap;
            cropView.setFilePath(null);
            showTakePicture();
        }
    };

    private View.OnClickListener cropConfirmButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int maskType = cropMaskView.getMaskType();
            Rect rect;
            switch (maskType) {
                case MaskView.MASK_TYPE_BANK_CARD:
                case MaskView.MASK_TYPE_ID_CARD_BACK:
                case MaskView.MASK_TYPE_ID_CARD_FRONT:
                    rect = cropMaskView.getFrameRect();
                    break;
                case MaskView.MASK_TYPE_NONE:
                default:
                    rect = overlayView.getFrameRect();
                    break;
            }
            Bitmap cropped = cropView.crop(rect);
            displayImageView.setImageBitmap(cropped);
            ImageUtils.getInstance().setBitmap(cropped); // 保存剪裁后的图片
            cropAndConfirm();
        }
    };

    private void cropAndConfirm() {
        cameraView.getCameraControl().pause();
        updateFlashMode();
        doConfirmResult();
    }

    private void doConfirmResult() {
        CameraThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                    Bitmap bitmap = ((BitmapDrawable) displayImageView.getDrawable()).getBitmap();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent();
                intent.putExtra(CameraActivity.KEY_CONTENT_TYPE, contentType);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
    }

    private View.OnClickListener confirmButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            doConfirmResult();
        }
    };

    private View.OnClickListener confirmCancelButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            displayImageView.setImageBitmap(null);
            showTakePicture();
        }
    };

    private View.OnClickListener rotateButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            cropView.rotate(90);
        }
    };

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(contentURI, null, null, null, null);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setOrientation(newConfig);
    }

    private void setOrientation(Configuration newConfig) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int orientation;
        int cameraViewOrientation = CameraView.ORIENTATION_PORTRAIT;
        switch (newConfig.orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                cameraViewOrientation = CameraView.ORIENTATION_PORTRAIT;
                orientation = OCRCameraLayout.ORIENTATION_PORTRAIT;
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                orientation = OCRCameraLayout.ORIENTATION_HORIZONTAL;
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
                    cameraViewOrientation = CameraView.ORIENTATION_HORIZONTAL;
                } else {
                    cameraViewOrientation = CameraView.ORIENTATION_INVERT;
                }
                break;
            default:
                orientation = OCRCameraLayout.ORIENTATION_PORTRAIT;
                cameraView.setOrientation(CameraView.ORIENTATION_PORTRAIT);
                break;
        }
//        takePictureContainer.setOrientation(orientation);
        cameraView.setOrientation(cameraViewOrientation);
        cropContainer.setOrientation(orientation);
        confirmResultContainer.setOrientation(orientation);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                cropView.setFilePath(getRealPathFromURI(uri));
                showCrop();
            } else {
                cameraView.getCameraControl().resume();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraView.getCameraControl().refreshPermission();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.camera_permission_required, Toast.LENGTH_LONG)
                            .show();
                }
                break;
            }
            case PERMISSIONS_EXTERNAL_STORAGE:
            default:
                break;
        }
    }

    /**
     * 做一些收尾工作
     */
    private void doClear() {
        CameraThreadPool.cancelAutoFocusTimer();
        if (isNativeEnable && !isNativeManual) {
            IDcardQualityProcess.getInstance().releaseModel();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.doClear();
    }

}

package com.example.ocr;

import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


public class MyCropView extends View {

    // Private Constants ///////////////////////////////////////////////////////
    private static final float BMP_LEFT = 0f;
    private static final float BMP_TOP = 20f;
    private static final float DEFAULT_BORDER_RECT_WIDTH = 200f;
    private static final float DEFAULT_BORDER_RECT_HEIGHT = 200f;
    private static final int POS_TOP_LEFT = 0;
    private static final int POS_TOP_RIGHT = 1;
    private static final int POS_BOTTOM_LEFT = 2;
    private static final int POS_BOTTOM_RIGHT = 3;
    private static final int POS_TOP = 4;
    private static final int POS_BOTTOM = 5;
    private static final int POS_LEFT = 6;
    private static final int POS_RIGHT = 7;
    private static final int POS_CENTER = 8;

    // this constant would be best to use event number
    private static final float BORDER_LINE_WIDTH = 6f;
    private static final float BORDER_CORNER_LENGTH = 30f;
    private static final float TOUCH_FIELD = 10f;

    // Member Variables ////////////////////////////////////////////////////////
    private String mBmpPath;

    private Bitmap mBmpToCrop;
    private RectF mBmpBound;
    private Paint mBmpPaint;
    private Paint mBorderPaint;// 裁剪区边框
    private Paint mGuidelinePaint;
    private Paint mBgPaint;
    private RectF mDefaultBorderBound;
    private RectF mBorderBound;
    private PointF mLastPoint = new PointF();
    private float mBorderWidth;
    private float mBorderHeight;
    private int touchPos;

    // Constructors ////////////////////////////////////////////////////////////
    public MyCropView(Context context) {
        super(context);
        init(context);
    }

    public MyCropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    // View Methods ////////////////////////////////////////////////////////////
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // TODO Auto-generated method stub
        // super.onSizeChanged(w, h, oldw, oldh);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        // super.onDraw(canvas);
        if (mBmpPath != null) {
            canvas.drawBitmap(mBmpToCrop, null, mBmpBound, mBmpPaint);
            canvas.drawRect(mBorderBound.left, mBorderBound.top, mBorderBound.right, mBorderBound.bottom, mBorderPaint);
            drawGuidlines(canvas);
            drawBackground(canvas);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        // super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setLastPosition(event);
                getParent().requestDisallowInterceptTouchEvent(true);
                // onActionDown(event.getX(), event.getY());
                touchPos = detectTouchPosition(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                onActionMove(event.getX(), event.getY());
                setLastPosition(event);
                break;
            case MotionEvent.ACTION_UP:
                break;
        }

        return true;
    }

    // Public Methods //////////////////////////////////////////////////////////
    public String getBmpPath() {
        return mBmpPath;
    }


    public void setBmpPath(String picPath) {
        this.mBmpPath = picPath;
        mBmpToCrop = BitmapFactory.decodeFile(mBmpPath);
        setBmp();
    }

    public void setBitmap(Bitmap bitmap){
        this.mBmpToCrop = bitmap;
        setBmp();
    }

    public Bitmap getCroppedImage() {
        // 先不考虑图片被压缩的情况 就当作现在的图片就是1：1的
        return Bitmap.createBitmap(mBmpToCrop, (int) mBorderBound.left, (int) mBorderBound.top, (int) mBorderWidth,
                (int) mBorderHeight);
    }

    // Private Methods /////////////////////////////////////////////////////////
    private void init(Context context) {

        mBmpPaint = new Paint();
        // 以下是抗锯齿
        mBmpPaint.setAntiAlias(true);// 防止边缘的锯齿
        mBmpPaint.setFilterBitmap(true);// 对位图进行滤波处理

        mBorderPaint = new Paint();
        mBorderPaint.setStyle(Style.STROKE);
        mBorderPaint.setColor(Color.parseColor("#AAFFFFFF"));
        mBorderPaint.setStrokeWidth(BORDER_LINE_WIDTH);

        mGuidelinePaint = new Paint();
        mGuidelinePaint.setColor(Color.parseColor("#AAFFFFFF"));
        mGuidelinePaint.setStrokeWidth(1f);

        mBgPaint = new Paint();
        mBgPaint.setColor(Color.parseColor("#B0000000"));
        mBgPaint.setAlpha(150);
    }

    private void setBmp() {
        mBmpBound = new RectF();
        mBmpBound.left = BMP_LEFT;
        mBmpBound.top = BMP_TOP;
        mBmpBound.right = mBmpBound.left + mBmpToCrop.getWidth();
        mBmpBound.bottom = mBmpBound.top + mBmpToCrop.getHeight();

        // 使裁剪框一开始出现在图片的中心位置
        mDefaultBorderBound = new RectF();

        mDefaultBorderBound.left = (mBmpBound.left + mBmpBound.right - DEFAULT_BORDER_RECT_WIDTH) / 2;
        mDefaultBorderBound.top = (mBmpBound.top + mBmpBound.bottom - DEFAULT_BORDER_RECT_HEIGHT) / 2;
        mDefaultBorderBound.right = mDefaultBorderBound.left + DEFAULT_BORDER_RECT_WIDTH;
        mDefaultBorderBound.bottom = mDefaultBorderBound.top + DEFAULT_BORDER_RECT_HEIGHT;

        mBorderBound = new RectF();
        mBorderBound.left = mDefaultBorderBound.left;
        mBorderBound.top = mDefaultBorderBound.top;
        mBorderBound.right = mDefaultBorderBound.right;
        mBorderBound.bottom = mDefaultBorderBound.bottom;

        getBorderEdgeLength();
        invalidate();
    }


    private void drawBackground(Canvas canvas) {

           /*-
           -------------------------------------
           |                top                |
           -------------------------------------
           |      |                    |       |<——————————mBmpBound
           |      |                    |       |
           | left |                    | right |
           |      |                    |       |
           |      |                  <─┼───────┼────mBorderBound
           -------------------------------------
           |              bottom               |
           -------------------------------------
          */

        // Draw "top", "bottom", "left", then "right" quadrants.
        // because the border line width is larger than 1f, in order to draw a complete border rect ,
        // i have to change zhe rect coordinate to draw
        float delta = BORDER_LINE_WIDTH / 2;
        float left = mBorderBound.left - delta;
        float top = mBorderBound.top - delta;
        float right = mBorderBound.right + delta;
        float bottom = mBorderBound.bottom + delta;

        // -------------------------------------------------------------------------------移动到上下两端会多出来阴影
        canvas.drawRect(mBmpBound.left, mBmpBound.top, mBmpBound.right, top, mBgPaint);
        canvas.drawRect(mBmpBound.left, bottom, mBmpBound.right, mBmpBound.bottom, mBgPaint);
        canvas.drawRect(mBmpBound.left, top, left, bottom, mBgPaint);
        canvas.drawRect(right, top, mBmpBound.right, bottom, mBgPaint);
    }

    // 画裁剪区域中间的参考线
    private void drawGuidlines(Canvas canvas) {
        // Draw vertical guidelines.
        final float oneThirdCropWidth = mBorderBound.width() / 3;

        final float x1 = mBorderBound.left + oneThirdCropWidth;
        canvas.drawLine(x1, mBorderBound.top, x1, mBorderBound.bottom, mGuidelinePaint);
        final float x2 = mBorderBound.right - oneThirdCropWidth;
        canvas.drawLine(x2, mBorderBound.top, x2, mBorderBound.bottom, mGuidelinePaint);

        // Draw horizontal guidelines.
        final float oneThirdCropHeight = mBorderBound.height() / 3;

        final float y1 = mBorderBound.top + oneThirdCropHeight;
        canvas.drawLine(mBorderBound.left, y1, mBorderBound.right, y1, mGuidelinePaint);
        final float y2 = mBorderBound.bottom - oneThirdCropHeight;
        canvas.drawLine(mBorderBound.left, y2, mBorderBound.right, y2, mGuidelinePaint);
    }


    private void onActionMove(float x, float y) {
        float deltaX = x - mLastPoint.x;
        float deltaY = y - mLastPoint.y;
        // 这里先不考虑裁剪框放最大的情况
        switch (touchPos) {
            case POS_CENTER:
                mBorderBound.left += deltaX;
                // fix border position
                if (mBorderBound.left < mBmpBound.left)
                    mBorderBound.left = mBmpBound.left;
                if (mBorderBound.left > mBmpBound.right - mBorderWidth)
                    mBorderBound.left = mBmpBound.right - mBorderWidth;

                mBorderBound.top += deltaY;
                if (mBorderBound.top < mBmpBound.top)
                    mBorderBound.top = mBmpBound.top;

                if (mBorderBound.top > mBmpBound.bottom - mBorderHeight)
                    mBorderBound.top = mBmpBound.bottom - mBorderHeight;

                mBorderBound.right = mBorderBound.left + mBorderWidth;
                mBorderBound.bottom = mBorderBound.top + mBorderHeight;
                break;

            case POS_TOP:
                resetTop(deltaY);
                break;
            case POS_BOTTOM:
                resetBottom(deltaY);
                break;
            case POS_LEFT:
                resetLeft(deltaX);
                break;
            case POS_RIGHT:
                resetRight(deltaX);
                break;
            case POS_TOP_LEFT:
                resetTop(deltaY);
                resetLeft(deltaX);
                break;
            case POS_TOP_RIGHT:
                resetTop(deltaY);
                resetRight(deltaX);
                break;
            case POS_BOTTOM_LEFT:
                resetBottom(deltaY);
                resetLeft(deltaX);
                break;
            case POS_BOTTOM_RIGHT:
                resetBottom(deltaY);
                resetRight(deltaX);
                break;
            default:

                break;
        }
        invalidate();
    }


    private int detectTouchPosition(float x, float y) {
        if (x > mBorderBound.left + TOUCH_FIELD && x < mBorderBound.right - TOUCH_FIELD
                && y > mBorderBound.top + TOUCH_FIELD && y < mBorderBound.bottom - TOUCH_FIELD)
            return POS_CENTER;

        if (x > mBorderBound.left + BORDER_CORNER_LENGTH && x < mBorderBound.right - BORDER_CORNER_LENGTH) {
            if (y > mBorderBound.top - TOUCH_FIELD && y < mBorderBound.top + TOUCH_FIELD)
                return POS_TOP;
            if (y > mBorderBound.bottom - TOUCH_FIELD && y < mBorderBound.bottom + TOUCH_FIELD)
                return POS_BOTTOM;

        }
        if (y > mBorderBound.top + BORDER_CORNER_LENGTH && y < mBorderBound.bottom - BORDER_CORNER_LENGTH) {
            if (x > mBorderBound.left - TOUCH_FIELD && x < mBorderBound.left + TOUCH_FIELD)
                return POS_LEFT;
            if (x > mBorderBound.right - TOUCH_FIELD && x < mBorderBound.right + TOUCH_FIELD)
                return POS_RIGHT;
        }

        // 前面的逻辑已经排除掉了几种情况 所以后面的 ┏ ┓ ┗ ┛ 边角就按照所占区域的方形来判断就可以了
        if (x > mBorderBound.left - TOUCH_FIELD && x < mBorderBound.left + BORDER_CORNER_LENGTH) {
            if (y > mBorderBound.top - TOUCH_FIELD && y < mBorderBound.top + BORDER_CORNER_LENGTH)
                return POS_TOP_LEFT;

            if (y > mBorderBound.bottom - BORDER_CORNER_LENGTH && y < mBorderBound.bottom + TOUCH_FIELD)
                return POS_BOTTOM_LEFT;
        }

        if (x > mBorderBound.right - BORDER_CORNER_LENGTH && x < mBorderBound.right + TOUCH_FIELD) {
            if (y > mBorderBound.top - TOUCH_FIELD && y < mBorderBound.top + BORDER_CORNER_LENGTH)
                return POS_TOP_RIGHT;

            if (y > mBorderBound.bottom - BORDER_CORNER_LENGTH && y < mBorderBound.bottom + TOUCH_FIELD)
                return POS_BOTTOM_RIGHT;
        }
        return -1;
    }


    private void setLastPosition(MotionEvent event) {
        mLastPoint.x = event.getX();
        mLastPoint.y = event.getY();
    }

    private void getBorderEdgeLength() {
        mBorderWidth = mBorderBound.width();
        mBorderHeight = mBorderBound.height();
    }


    private void getBorderEdgeWidth() {
        mBorderWidth = mBorderBound.width();
    }

    private void getBorderEdgeHeight() {
        mBorderHeight = mBorderBound.height();
    }

    private void resetLeft(float delta) {
        mBorderBound.left += delta;
        getBorderEdgeWidth();
        fixBorderLeft();
    }

    private void resetTop(float delta) {
        mBorderBound.top += delta;
        getBorderEdgeHeight();
        fixBorderTop();
    }

    private void resetRight(float delta) {
        mBorderBound.right += delta;
        getBorderEdgeWidth();
        fixBorderRight();
    }

    private void resetBottom(float delta) {
        mBorderBound.bottom += delta;
        getBorderEdgeHeight();
        fixBorderBottom();
    }

    private void fixBorderLeft() {
        if (mBorderBound.left < mBmpBound.left)
            mBorderBound.left = mBmpBound.left;
        if (mBorderWidth < 2 * BORDER_CORNER_LENGTH)
            mBorderBound.left = mBorderBound.right - 2 * BORDER_CORNER_LENGTH;
    }

    private void fixBorderTop() {
        // fix top
        if (mBorderBound.top < mBmpBound.top)
            mBorderBound.top = mBmpBound.top;
        if (mBorderHeight < 2 * BORDER_CORNER_LENGTH)
            mBorderBound.top = mBorderBound.bottom - 2 * BORDER_CORNER_LENGTH;
    }

    private void fixBorderRight() {
        // fix right
        if (mBorderBound.right > mBmpBound.right)
            mBorderBound.right = mBmpBound.right;
        if (mBorderWidth < 2 * BORDER_CORNER_LENGTH)
            mBorderBound.right = mBorderBound.left + 2 * BORDER_CORNER_LENGTH;
    }

    private void fixBorderBottom() {
        // fix bottom
        if (mBorderBound.bottom > mBmpBound.bottom)
            mBorderBound.bottom = mBmpBound.bottom;
        if (mBorderHeight < 2 * BORDER_CORNER_LENGTH)
            mBorderBound.bottom = mBorderBound.top + 2 * BORDER_CORNER_LENGTH;
    }
}

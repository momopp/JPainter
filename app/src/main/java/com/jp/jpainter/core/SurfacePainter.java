package com.jp.jpainter.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewConfiguration;

import com.jp.jpainter.R;
import com.jp.jpainter.utils.LogUtil;

/**
 *
 */
public class SurfacePainter extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    /**
     * 闲置状态。无触摸交互且无需更新视图。
     */
    private static final int STATUS_IDLE = 0;

    /**
     * 绘制状态。用户产生交互且正在绘制。
     */
    private static final int STATUS_PAINTING = 1;

    /**
     * 缩放状态。用户产生交互且正在缩放视图。
     */
    private static final int STATUS_SCALING = 3;

    /**
     * 移动状态。用户产生交互且正在移动视图。
     */
    private static final int STATUS_MOVING = 4;

    /**
     * 销毁状态。SurfaceView 已被销毁。
     */
    private static final int STATUS_DESTROYED = 5;

    /**
     * 每一帧的时间。定义为 60fps
     */
    private static final int mFrameTime = (int) (1000 / 60);

    private static final int DEFAULT_CORNER_RADIUS = 30;

    private SurfaceHolder mHolder;
    private Canvas mCanvas;
    private Paint mPaint;
    Path mPath;

    private int mHeight;
    private int mWidth;
    private int mCurrentStatus;

    private float mCurrentOffsetX = 0;
    private float mCurrentOffsetY = 0;
    private float mCurrentScale = 1.0f;

    public SurfacePainter(Context context) {
        this(context, null);
    }

    public SurfacePainter(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SurfacePainter(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        mHolder = getHolder();
        mHolder.addCallback(this);

        setFocusable(true);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(4.0f);

        CornerPathEffect effect = new CornerPathEffect(DEFAULT_CORNER_RADIUS);
        mPaint.setPathEffect(effect);

        mPath = new Path();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCanvas = mHolder.lockCanvas();
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mHolder.unlockCanvasAndPost(mCanvas);

        setCurrentStatus(STATUS_IDLE);
        new Thread(this).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mHeight = height;
        mWidth = width;

        mCanvas = mHolder.lockCanvas();
        drawCanvasBackground();
        mHolder.unlockCanvasAndPost(mCanvas);

        Log.i(this.getClass().getSimpleName(),
                "surfaceChanged: width = " + width + ", height = " + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        setCurrentStatus(STATUS_DESTROYED);
    }

    @Override
    public void run() {
        while (STATUS_DESTROYED != getCurrentStatus()) {
            long start = System.currentTimeMillis();
            drawContent();
            long end = System.currentTimeMillis();

            long time = end - start;
            if (time < mFrameTime) {
                try {
                    Thread.sleep(mFrameTime - time);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 绘制内容
     */
    private void drawContent() {
        if (STATUS_IDLE == getCurrentStatus() || STATUS_DESTROYED == getCurrentStatus()) {
            return;
        }

        try {
            mCanvas = mHolder.lockCanvas();

            LogUtil.d("Painter", "scale = " + mCurrentScale + ", offX = " + mCurrentOffsetX + ", offY = " + mCurrentOffsetY);
            mCanvas.translate(mCurrentOffsetX, mCurrentOffsetY);
            mCanvas.scale(mCurrentScale, mCurrentScale);

            drawCanvasBackground();
            mCanvas.drawPath(mPath, mPaint);

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (mCanvas != null) {
                mHolder.unlockCanvasAndPost(mCanvas);
            }
        }
    }

    /**
     * 绘制画布背景。
     */
    private void drawCanvasBackground() {
        Bitmap t = BitmapFactory.decodeResource(getResources(), R.drawable.canvas_background);
        BitmapShader bs = new BitmapShader(t, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Paint p = new Paint();
        p.setShader(bs);

        mCanvas.drawPaint(p);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(8, 0, 0, 0xFF666666);
        mCanvas.drawRect(0, 0, mWidth, mHeight, paint);
    }

    // 目前适配两个手指
    private int pointer1Index = -1;
    private int pointer2Index = -1;

    // 当前触摸点数量
    private int currentFingerCount;
    private boolean isFirstFingerTouching = false;

    private float downX;
    private float downY;

    private float moveStartX;
    private float moveStartY;

    private float scaleStartX1;
    private float scaleStartY1;
    private float scaleStartX2;
    private float scaleStartY2;


    private float canvasPivotX;
    private float canvasPivotY;

    private float scaleStartScale;

    private float scaleCurrentX1;
    private float scaleCurrentY1;
    private float scaleCurrentX2;
    private float scaleCurrentY2;
    private float scaleCurrentPivotX;
    private float scaleCurrentPivotY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                isFirstFingerTouching = true;
                pointer1Index = event.getActionIndex();
                currentFingerCount = event.getPointerCount();
                setCurrentStatus(STATUS_PAINTING);

                mPath.moveTo(coordinateScreen2Canvas(x, mCurrentOffsetX),
                        coordinateScreen2Canvas(y, mCurrentOffsetY));
                downX = x;
                downY = y;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                currentFingerCount = event.getPointerCount();

                if (2 == currentFingerCount) {
                    if (-1 == pointer2Index) {
                        pointer2Index = event.getActionIndex();

                    } else {
                        pointer1Index = event.getActionIndex();
                    }

                    if (shouldScaling(event)) {
                        setCurrentStatus(STATUS_SCALING);

                        scaleStartX1 = event.getX(pointer1Index);
                        scaleStartY1 = event.getY(pointer1Index);
                        scaleStartX2 = event.getX(pointer2Index);
                        scaleStartY2 = event.getY(pointer2Index);
                        float scaleStartPivotX = Math.min(scaleStartX1, scaleStartX2)
                                + (Math.abs(scaleStartX1 - scaleStartX2) / 2);
                        float scaleStartPivotY = Math.min(scaleStartY1, scaleStartY2)
                                + (Math.abs(scaleStartY1 - scaleStartY2) / 2);

                        canvasPivotX = coordinateScreen2Canvas(scaleStartPivotX, mCurrentOffsetX);
                        canvasPivotY = coordinateScreen2Canvas(scaleStartPivotY, mCurrentOffsetY);

                        scaleStartScale = mCurrentScale;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                switch (getCurrentStatus()) {
                    case STATUS_PAINTING:
                        if (isFirstFingerTouching) {
                            mPath.lineTo(coordinateScreen2Canvas(x, mCurrentOffsetX),
                                    coordinateScreen2Canvas(y, mCurrentOffsetY));
                        }
                        break;

                    case STATUS_SCALING:
                        scaleCurrentX1 = event.getX(pointer1Index);
                        scaleCurrentY1 = event.getY(pointer1Index);
                        scaleCurrentX2 = event.getX(pointer2Index);
                        scaleCurrentY2 = event.getY(pointer2Index);

                        double beforeSpan = Math.hypot(
                                scaleStartX1 - scaleStartX2, scaleStartY1 - scaleStartY2);
                        double afterSpan = Math.hypot(scaleCurrentX1 - scaleCurrentX2,
                                scaleCurrentY1 - scaleCurrentY2);

                        scaleCurrentPivotX = Math.min(scaleCurrentX1, scaleCurrentX2)
                                + (Math.abs(scaleCurrentX1 - scaleCurrentX2) / 2);
                        scaleCurrentPivotY = Math.min(scaleCurrentY1, scaleCurrentY2)
                                + (Math.abs(scaleCurrentY1 - scaleCurrentY2) / 2);

                        mCurrentScale = (float) (scaleStartScale * (afterSpan / beforeSpan));
                        mCurrentOffsetX = scaleCurrentPivotX - canvasPivotX * mCurrentScale;
                        mCurrentOffsetY = scaleCurrentPivotY - canvasPivotY * mCurrentScale;

                        break;

                    case STATUS_MOVING:
                        mCurrentOffsetX += x - moveStartX;
                        mCurrentOffsetY += y - moveStartY;

                        moveStartX = x;
                        moveStartY = y;
                        break;

                    case STATUS_IDLE:
                    default:
                        Log.w(this.getClass().getSimpleName(), "incorrect status");
                        break;
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                currentFingerCount = event.getPointerCount() - 1;
                int actionIndex = event.getActionIndex();

                if (actionIndex == pointer1Index) {
                    isFirstFingerTouching = false;
                }

                if ((STATUS_SCALING == getCurrentStatus())
                        && (actionIndex == pointer1Index || actionIndex == pointer2Index)) {
                    setCurrentStatus(STATUS_MOVING);
                    moveStartX = actionIndex == pointer1Index ? event.getX(pointer2Index)
                            : event.getX(pointer1Index);
                    moveStartY = actionIndex == pointer1Index ? event.getY(pointer2Index)
                            : event.getY(pointer1Index);

                    if (actionIndex == pointer1Index) {
                        pointer1Index = -1;
                    } else if (actionIndex == pointer2Index) {
                        pointer2Index = -1;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                currentFingerCount = 0;
                isFirstFingerTouching = false;
                pointer1Index = -1;
                pointer2Index = -1;
                setCurrentStatus(STATUS_IDLE);
                break;

            case MotionEvent.ACTION_CANCEL:
                LogUtil.d("ACTION_CANCEL ->", "actionIndex = " + event.getActionIndex());
                break;
        }

        return true;
    }

    /**
     * View 上的坐标转换为在 canvas 中的坐标
     *
     * @param coordinate View 上的坐标
     * @param offset     偏移量
     * @return 在 canvas 中的坐标
     */
    private float coordinateScreen2Canvas(float coordinate, float offset) {
        return (coordinate - offset) / mCurrentScale;
    }

    /**
     * 判断是否进行缩放。当第一个手指触摸时间小于 TapTimeout 并且移动的距离小于 TouchSlop 时，第二个手指按下
     * 即触发缩放。
     *
     * @param event 传入 MotionEvent
     * @return true  触发缩放
     * false 不触发缩放
     */
    private boolean shouldScaling(MotionEvent event) {
        long downTime = event.getDownTime();
        long eventTime = event.getEventTime();
        float firstFingerX = event.getX(pointer1Index);
        float firstFingerY = event.getY(pointer1Index);

        double firstFingerSpan = Math.hypot(firstFingerX - downX, firstFingerY - downY);
        long timeInterval = eventTime - downTime;
        ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        return STATUS_MOVING == getCurrentStatus()
                || (viewConfiguration.getScaledTouchSlop() > firstFingerSpan
                && timeInterval < ViewConfiguration.getTapTimeout());
    }

    /**
     * 设定当前状态。
     *
     * @param currentStatus 当前状态
     */
    private void setCurrentStatus(int currentStatus) {
        mCurrentStatus = currentStatus;
    }

    /**
     * 获取当前状态。
     *
     * @return 当前状态
     */
    public int getCurrentStatus() {
        return mCurrentStatus;
    }
}

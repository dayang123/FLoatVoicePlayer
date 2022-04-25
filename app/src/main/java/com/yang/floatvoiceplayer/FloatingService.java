package com.yang.floatvoiceplayer;

import android.animation.ValueAnimator;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.yang.floatvoiceplayer.utils.Utils;


public class FloatingService extends Service {

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams wmParams;
    private LayoutInflater inflater;
    private View mFloatingLayout;
    private int screenWidth, screenHeight;

    private TextView currentTime, totalTime, fileName, tvSpeed;
    private boolean play;
    private ImageView playButton;


    @Override
    public void onCreate() {
        super.onCreate();
        initWindow();//设置悬浮窗基本参数（位置、宽高等）
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    public class MyBinder extends Binder {
        public FloatingService getService() {
            return FloatingService.this;
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingLayout != null) {
            mWindowManager.removeViewImmediate(mFloatingLayout);
            mFloatingLayout = null;
        }
    }

    private void initWindow() {
        mWindowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(displayMetrics);

        Display display = mWindowManager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        screenWidth = point.x;
        screenHeight = point.y;
        Log.d("measureScreen", "width + height" + screenWidth + " " + screenHeight);

        wmParams = getWindowParams();
        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.gravity = Gravity.START | Gravity.TOP;
//        wmParams.x = screenWidth / 3 * 2;
//        wmParams.y = screenHeight / 6;
        inflater = LayoutInflater.from(getApplicationContext());

        mFloatingLayout = inflater.inflate(R.layout.view_floating, null);
        currentTime = mFloatingLayout.findViewById(R.id.tv_current_time);
        totalTime = mFloatingLayout.findViewById(R.id.tv_total_time);
        fileName = mFloatingLayout.findViewById(R.id.name);
        fileName.setOnClickListener(view -> action(PlayActivity.ACTION_INIT));

        mFloatingLayout.setOnTouchListener(onTouchListener);

        mFloatingLayout.findViewById(R.id.iv_back).setOnClickListener(view -> action(PlayActivity.ACTION_BACK));
        playButton = mFloatingLayout.findViewById(R.id.iv_play);
        playButton.setOnClickListener(view -> {
            play = !play;
            playButton.setSelected(play);
            action(play ? PlayActivity.ACTION_PLAY : PlayActivity.ACTION_PAUSE);
        });
        mFloatingLayout.findViewById(R.id.iv_forward).setOnClickListener(view -> action(PlayActivity.ACTION_FORWARD));
        tvSpeed = mFloatingLayout.findViewById(R.id.tv_speed);
        tvSpeed.setOnClickListener(view -> action(PlayActivity.ACTION_SPEED));
        mFloatingLayout.findViewById(R.id.iv_stop).setOnClickListener(view -> action(PlayActivity.ACTION_STOP));
        mWindowManager.addView(mFloatingLayout, wmParams);
    }

    private void action(String action) {
        Intent intent = new Intent(PlayActivity.BROADCAST_INTENT_FILTER);
        intent.putExtra(PlayActivity.ACTION, action);
        sendBroadcast(intent);
    }

    public void updateProgress(int currentTime) {
        this.currentTime.setText(Utils.formatVideoTime(currentTime));
    }

    public void initPlay() {
        currentTime.setText("00:00");
        play = false;
        playButton.setSelected(false);
    }

    public void initData(String name, int totalTime) {
        fileName.setText(name);
        this.totalTime.setText(Utils.formatVideoTime(totalTime));
    }

    public void updateSpeed(String speed) {
        tvSpeed.setText(speed);
    }

    private WindowManager.LayoutParams getWindowParams() {
        wmParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        return wmParams;
    }

    private float moveY, mTouchStartY, moveX, mTouchStartX;
    private boolean isScroll;

    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            moveY = event.getRawY();
            moveX = event.getRawX();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTouchStartY = event.getY();
                    mTouchStartX = event.getX();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if(isScroll) {
                        updateViewPosition();
                    } else {
                        if(Math.abs(mTouchStartY - event.getY()) > mFloatingLayout.getHeight() / 3 ||
                                Math.abs(mTouchStartX - event.getX()) > mFloatingLayout.getWidth() / 3)  {
                            updateViewPosition();
                        } else {
                            break;
                        }
                    }
                    isScroll = true;
                    break;
                case MotionEvent.ACTION_UP:
                    if(isScroll) {
                        isScroll = false;
                        //autoScrollToSide();
                        return  true;
                    }
                    break;
            }

            return false;
        }
    };

    private void autoScrollToSide() {
        int[] screenInfo = new int[2];
        mFloatingLayout.getLocationOnScreen(screenInfo);
        float startRawX =screenInfo[0];

        boolean isToLeft = startRawX + mFloatingLayout.getWidth() / 2 < screenWidth / 2;
        Log.d("CalculateSide", "up point: " + startRawX + "  viewWidth: " + mFloatingLayout.getWidth() + "    screenWidth:  " + screenWidth );

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, screenWidth/2).setDuration(500);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (isToLeft) {
                    wmParams.x = (int) (startRawX * (1 - animation.getAnimatedFraction()));
                } else {
                    wmParams.x = (int) (startRawX + (screenWidth - startRawX - mFloatingLayout.getWidth()) * (animation.getAnimatedFraction()));
                }
                mWindowManager.updateViewLayout(mFloatingLayout, wmParams);
            }
        });
        valueAnimator.start();
    }

    private void updateViewPosition() {
        wmParams.y = (int) (moveY - mTouchStartY - screenHeight / 25);
        wmParams.x = (int)(moveX - mTouchStartX);
        mWindowManager.updateViewLayout(mFloatingLayout, wmParams);
    }
}

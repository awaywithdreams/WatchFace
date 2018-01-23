package cis490c.niagara.edu.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;




public class WatchFaceService extends CanvasWatchFaceService {
    @Override
    public Engine onCreateEngine() {
        return super.onCreateEngine();
    }

    private class WatchFaceEngine extends Engine{
        private Typeface WATCH_TEXT_TYPEFACE =
                Typeface.create(Typeface.SANS_SERIF,Typeface.BOLD);

        private static final int MSG_UPDATE_TIME_ID =38;
        private long mUpdateRateMs= 1000;
        private Time mDisplayTime;


        private Paint mBackgroundColorPaint;
        private Paint mTextColorPaint;

        private boolean mHasTimeZoneReceiverBeenRegistered = false;
        private boolean mIsMuteMode;
        private boolean mIsLowBitAmbient;

        private float mXOffset;
        private float mYOffset;

        private int mBackgroundColor = Color.BLACK;
        private int mTextColor=Color.RED;

        final BroadcastReceiver mTimeZoneBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mDisplayTime.clear(intent.getStringExtra("time-zone"));
                mDisplayTime.setToNow();
            }
        };

        private final Handler mTimeHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what){
                    case MSG_UPDATE_TIME_ID:{
                        invalidate();
                        if (isVisible() && !isInAmbientMode()){
                            long currentTimeMillis = System.currentTimeMillis();
                            long delay = mUpdateRateMs - (currentTimeMillis % mUpdateRateMs);
                            mTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME_ID,delay);
                        }
                    }
                }
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceService.this)
                                .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                                .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                                .setShowSystemUiTime(false)
                                .build());
            mDisplayTime = new Time();

            initBackground();
            initDisplayText();

        }

        private void initBackground(){
            mBackgroundColorPaint = new Paint();
            mBackgroundColorPaint.setColor(mBackgroundColor);
        }
        private void initDisplayText(){
            mTextColorPaint = new Paint();
            mTextColorPaint.setColor(mTextColor);
            mTextColorPaint.setTypeface(WATCH_TEXT_TYPEFACE);
            mTextColorPaint.setAntiAlias(true);
            mTextColorPaint.setTextSize(32);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible){
                if (!mHasTimeZoneReceiverBeenRegistered){
                    IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_CHANGED);
                    WatchFaceService.this.registerReceiver(mTimeZoneBroadcastReceiver,filter);
                    mHasTimeZoneReceiverBeenRegistered = true;
                }
                mDisplayTime.clear(TimeZone.getDefault().getID());
                mDisplayTime.setToNow();

            } else {
                if (mHasTimeZoneReceiverBeenRegistered){
                    WatchFaceService.this.unregisterReceiver(mTimeZoneBroadcastReceiver);
                    mHasTimeZoneReceiverBeenRegistered = false;
                }
            }

            updateTimer();
        }

        private void updateTimer(){
            mTimeHandler.removeMessages(MSG_UPDATE_TIME_ID);
            if (isVisible() && !isInAmbientMode()){
                mTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME_ID);
            }
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            mYOffset = 150;
            mXOffset = 100;
        }


        @Override
        public void onPropertiesChanged(Bundle properties){
            super.onPropertiesChanged(properties);

            if (properties.getBoolean(PROPERTY_BURN_IN_PROTECTION,false)){
                mIsLowBitAmbient= properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT,false);
            }

        }

        @Override
        public void onAmbientModeChanged (boolean inAmbientMode){
            super.onAmbientModeChanged(inAmbientMode);

            if (inAmbientMode){
                mTextColorPaint.setColor(Color.parseColor("white"));
            } else {
                mTextColorPaint.setColor(Color.parseColor("red"));

            }

            if (mIsLowBitAmbient){
                mTextColorPaint.setAntiAlias(!inAmbientMode);
            }

            invalidate();
            updateTimer();
    }
        @Override
        public void onInterruptionFilterChanged (int interruptionFilter){
            super.onInterruptionFilterChanged(interruptionFilter);

        boolean isDeviceMuted = (interruptionFilter ==
            android.support.wearable.watchface.WatchFaceService.INTERRUPTION_FILTER_NONE);

         if (isDeviceMuted){
             mUpdateRateMs = TimeUnit.MINUTES.toMillis(1);
         } else {
             mUpdateRateMs = 1000;
         }
            if (mIsMuteMode != isDeviceMuted){
                mIsMuteMode = isDeviceMuted;
                int alpha = (isDeviceMuted)? 100:255;
                mTextColorPaint.setAlpha(alpha);
                invalidate();
                updateTimer();
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();

        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds){
            super.onDraw(canvas, bounds);

            mDisplayTime.setToNow();
            drawBackground(canvas, bounds);
            drawTimeText(canvas);
        }

        private void drawBackground(Canvas canvas, Rect bounds){
            canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundColorPaint);
        }

        private void drawTimeText(Canvas canvas){
            String timeText = getHoursString() + ":" + String.format("%02d", mDisplayTime.minute);
            if(isInAmbientMode() || mIsMuteMode){
                timeText += (mDisplayTime.hour <12)? "AM" : "PM";
            } else {
                timeText += String.format(":%02d", mDisplayTime.second);
            }
            int currentsecond = mDisplayTime.second;

            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String weekday = new SimpleDateFormat("EEE").format(new Date());

           canvas.drawText(timeText, currentsecond, mYOffset, mTextColorPaint);
            canvas.drawText(date, mXOffset, mYOffset-50, mTextColorPaint);
            canvas.drawText(weekday, mXOffset, mYOffset-100, mTextColorPaint);


        }

        private  String getHoursString(){
            if (mDisplayTime.hour % 12 ==0)
                return "12";
            else if (mDisplayTime.hour <12)
                return String.valueOf(mDisplayTime.hour);
            else
                return String.valueOf(mDisplayTime.hour-12);
        }





    }
}

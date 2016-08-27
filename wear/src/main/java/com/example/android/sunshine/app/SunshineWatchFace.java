/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);


    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    private class Engine extends CanvasWatchFaceService.Engine implements
            GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener{
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mTextPaint;
        boolean mAmbient;
        Calendar mCalendar;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        float digitalXOffset;
        float digitalYOffset;
        float dateXOffset;
        float dateYOffset;
        float startLineXOffset;
        float startLineYOffset;
        float lineLength;
        float imageXOffset;
        float imageYOffset;
        float maxXOffset;
        float maxYOffset;
        float minXOffset;
        float minYOffset;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;
        Resources resources;
        Paint datePaint;
        GoogleApiClient googleApiClient;
        private final String TAG = Engine.class.getSimpleName();
        String minTemp;
        String maxTemp;
        Bitmap weatherBitmap;
        Paint maxTempPaint;
        Paint minTempPaint;


        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            googleApiClient = createGoogleApiClient();
            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            resources =SunshineWatchFace.this.getResources();
            digitalYOffset = resources.getDimension(R.dimen.digital_y_offset);
            lineLength = resources.getDimension(R.dimen.line_length);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mTextPaint = createTextPaint(resources.getColor(R.color.digital_text));
            datePaint = createDatePaint(resources.getColor(R.color.off_white));
            maxTempPaint = createMaxTempPaint(resources.getColor(R.color.white));
            minTempPaint = createMinTempPaint(resources.getColor(R.color.off_white));

            mCalendar = Calendar.getInstance();
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG,"Broadcast received");
                    minTemp = intent.getStringExtra("MIN_TEMP");
                    Log.d(TAG,minTemp);
                    maxTemp = intent.getStringExtra("MAX_TEMP");
                    weatherBitmap = intent.getParcelableExtra("IMAGE");
                    weatherBitmap = Bitmap.createScaledBitmap(weatherBitmap,75,75,false);

                    invalidate();

                }
            };
            LocalBroadcastManager.getInstance(getApplicationContext())
                    .registerReceiver(receiver,new IntentFilter("dispatch"));
        }

        private GoogleApiClient createGoogleApiClient(){
            return new GoogleApiClient.Builder(getApplicationContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API).build();
        }
        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }
        private Paint createDatePaint(int textColor){
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            paint.setTextSize(22);
            return paint;
        }
        private Paint createMaxTempPaint(int textColor){
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setAntiAlias(true);
            paint.setTextSize(38);
            return paint;
        }
        private Paint createMinTempPaint(int textColor){
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(Typeface.MONOSPACE);
            paint.setAntiAlias(true);
            paint.setTextSize(38);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            digitalXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);

            dateXOffset = resources.getDimension(isRound
                ? R.dimen.date_x_offset_round:R.dimen.date_x_offset);

            dateYOffset = resources.getDimension(isRound
                ? R.dimen.date_y_offset_round:R.dimen.date_y_offset);

            startLineXOffset = resources.getDimension(isRound
                    ? R.dimen.start_line_x_offset_round:R.dimen.start_line_x_offset);

            startLineYOffset = resources.getDimension(isRound
                    ? R.dimen.start_line_y_offset_round:R.dimen.start_line_y_offset);

            imageXOffset = resources.getDimension(isRound
                ? R.dimen.image_x_offset_round:R.dimen.image_x_offset);

            imageYOffset = resources.getDimension(isRound
                    ? R.dimen.image_y_offset_round:R.dimen.image_y_offset);

            maxXOffset = resources.getDimension(isRound
                    ? R.dimen.max_x_offset_round:R.dimen.max_x_offset);

            maxYOffset = resources.getDimension(isRound
                    ? R.dimen.max_y_offset_round:R.dimen.max_y_offset);

            minXOffset = resources.getDimension(isRound
                    ? R.dimen.min_x_offset_round:R.dimen.min_x_offset);

            minYOffset = resources.getDimension(isRound
                    ? R.dimen.min_y_offset_round:R.dimen.min_y_offset);


            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mTextPaint.setTextSize(textSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                }
                if (mAmbient){
                    mBackgroundPaint.setColor(resources.getColor(R.color.black));
                }
                invalidate();
            }

        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
                    Toast.makeText(getApplicationContext(), R.string.message, Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.

            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            String monthText = mCalendar.getDisplayName(Calendar.MONTH,Calendar.SHORT, Locale.getDefault());
            String dayText = mCalendar.getDisplayName(Calendar.DAY_OF_WEEK,Calendar.SHORT, Locale.getDefault());
            String dayYearText = String.format("%02d %d",mCalendar.get(Calendar.DAY_OF_MONTH)
                    ,mCalendar.get(Calendar.YEAR));
            String formattedDate = String.format("%s, %s %s",dayText,monthText,dayYearText);
            String timeText =String.format("%d:%02d", mCalendar.get(Calendar.HOUR),
                    mCalendar.get(Calendar.MINUTE));
            canvas.drawText(timeText, digitalXOffset, digitalYOffset, mTextPaint);
            canvas.drawText(formattedDate, dateXOffset, dateYOffset , datePaint);
            canvas.drawLine(startLineXOffset, startLineYOffset, startLineXOffset + lineLength, startLineYOffset ,datePaint);

            if (weatherBitmap != null){
                canvas.drawBitmap(weatherBitmap, imageXOffset , imageYOffset ,null);
                canvas.drawText(maxTemp, maxXOffset, maxYOffset,maxTempPaint);
                canvas.drawText(minTemp, minXOffset, minYOffset,minTempPaint);
            }

        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.d(TAG,"Google api connected");
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG,"Google api suspended");

        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.d(TAG,"Google api connection failed");

        }
    }
}

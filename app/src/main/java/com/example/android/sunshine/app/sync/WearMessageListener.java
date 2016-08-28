package com.example.android.sunshine.app.sync;

import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by rashwan on 8/28/16.
 */

public class WearMessageListener extends WearableListenerService{
    private static final String TAG = WearMessageListener.class.getSimpleName();
    private static final String GET_WEATHER_MESSAGE_PATH = "/get_weather";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG,"message received");
        if (messageEvent.getPath().equals(GET_WEATHER_MESSAGE_PATH)){
            Log.d(TAG,"received message with path: " + messageEvent.getPath());
            SunshineSyncAdapter.syncImmediately(this);
        }
    }
}

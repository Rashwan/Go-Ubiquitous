package com.example.android.sunshine.app.sync;

import android.util.Log;

import com.example.android.sunshine.app.R;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by rashwan on 8/28/16.
 */

public class WearMessageListener extends WearableListenerService{
    private static final String TAG = WearMessageListener.class.getSimpleName();

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG,"message received");
        if (messageEvent.getPath().equals(getString(R.string.get_weather_path))){
            Log.d(TAG,"received message with path: " + messageEvent.getPath());
            SunshineSyncAdapter.syncImmediately(this);
        }
    }
}

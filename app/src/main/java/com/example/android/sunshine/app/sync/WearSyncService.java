package com.example.android.sunshine.app.sync;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

/**
 * Created by rashwan on 8/26/16.
 */

public class WearSyncService extends IntentService{
    public static final String ACTION_UPDATE_WATCHFACE = "com.example.android.sunshine.app.ACTION_UPDATE_WATCHFACE";
    private static final int GOOGLE_API_CLIENT_TIMEOUT_S = 10;
    private static final String TAG = WearSyncService.class.getSimpleName();
    private static final String GOOGLE_API_CLIENT_ERROR_MSG = "Failed to connect to GoogleApiClient (error code = %d)";


    public WearSyncService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG,"Update Wear watch face intent received");
        if (intent.getAction().equals(ACTION_UPDATE_WATCHFACE)){
            sendDataToWearable();
        }
    }

    private void sendDataToWearable() {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API).build();
        // It's OK to use blockingConnect() here as we are running in an
        // IntentService that executes work on a separate (background) thread.
        ConnectionResult connectionResult = googleApiClient.blockingConnect(
                GOOGLE_API_CLIENT_TIMEOUT_S, TimeUnit.SECONDS);
        if (connectionResult.isSuccess() && googleApiClient.isConnected()) {
            PutDataMapRequest dataMap = PutDataMapRequest.create("/weather");
            dataMap.getDataMap().putString("KEY", String.valueOf(Math.random()));
            PutDataRequest request = dataMap.setUrgent().asPutDataRequest();
            DataApi.DataItemResult result = Wearable.DataApi.putDataItem(googleApiClient, request)
                    .await();
            if (!result.getStatus().isSuccess()) {
                Log.e(TAG, String.format("Error sending data using DataApi (error code = %d)",
                        result.getStatus().getStatusCode()));
            }else if(result.getStatus().isSuccess()){
                Log.d(TAG,"Data sent to wearable");
            }

        } else {
            Log.e(TAG, String.format(GOOGLE_API_CLIENT_ERROR_MSG,
                    connectionResult.getErrorCode()));
        }
    }
}

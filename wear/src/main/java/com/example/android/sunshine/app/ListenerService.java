package com.example.android.sunshine.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by rashwan on 8/27/16.
 */

public class ListenerService extends WearableListenerService {
    private static final String TAG = ListenerService.class.getSimpleName();
    Bitmap bitmap;


    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.d(TAG,"onDataChanged");
        for (DataEvent event: dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem()!= null){
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                String minTemp = dataMapItem.getDataMap().getString("MIN_TEMP");
                String maxTemp = dataMapItem.getDataMap().getString("MAX_TEMP");
                Asset image = dataMapItem.getDataMap().getAsset("IMAGE");
                if (bitmap != null){
                    bitmap.recycle();
                }
                bitmap = loadBitmapFromAsset(image);
                Intent intent = new Intent("dispatch");
                intent.putExtra("MIN_TEMP",minTemp);
                intent.putExtra("MAX_TEMP",maxTemp);
                intent.putExtra("IMAGE",bitmap);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            }
        }

    }
    public Bitmap loadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        ConnectionResult connectionResult = googleApiClient.blockingConnect(
                10, TimeUnit.SECONDS);

        if (!connectionResult.isSuccess()) {
            Log.d(TAG,"Couldn't connect to play services from wearable");
            return null;
        }
        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                googleApiClient, asset).await().getInputStream();
        googleApiClient.disconnect();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }
}

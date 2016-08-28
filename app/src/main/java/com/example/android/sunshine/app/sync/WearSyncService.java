package com.example.android.sunshine.app.sync;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.example.android.sunshine.app.R;
import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by rashwan on 8/26/16.
 */

public class WearSyncService extends IntentService{
    public static final String ACTION_UPDATE_WATCHFACE = "com.example.android.sunshine.app.ACTION_UPDATE_WATCHFACE";
    private static final int GOOGLE_API_CLIENT_TIMEOUT_S = 10;
    private static final String TAG = WearSyncService.class.getSimpleName();
    private static final String GOOGLE_API_CLIENT_ERROR_MSG = "Failed to connect to GoogleApiClient (error code = %d)";
    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
    };
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final String KEY_MIN_TEMP = "MIN_TEMP";
    private static final String KEY_MAX_TEMP = "MAX_TEMP";
    private static final String KEY_WEATHER_IMAGE = "WEATHER_IMAGE";

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

            String location = Utility.getPreferredLocation(this);
            Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                    location, System.currentTimeMillis());
            Cursor data = getContentResolver().query(weatherForLocationUri, FORECAST_COLUMNS, null,
                    null, WeatherContract.WeatherEntry.COLUMN_DATE + " ASC");
            if (data == null) {
                return;
            }
            if (!data.moveToFirst()) {
                data.close();
                return;
            }
            // Extract the weather data from the Cursor
            int weatherId = data.getInt(INDEX_WEATHER_ID);
            int weatherArtResourceId = Utility.getArtResourceForWeatherCondition(weatherId);
            double maxTemp = data.getDouble(INDEX_MAX_TEMP);
            double minTemp = data.getDouble(INDEX_MIN_TEMP);
            String formattedMaxTemperature = Utility.formatTemperature(this, maxTemp);
            String formattedMinTemperature = Utility.formatTemperature(this, minTemp);
            data.close();

            Bitmap bitmap = BitmapFactory.decodeResource(getResources(),weatherArtResourceId);
            Asset asset = createAssetFromBitmap(bitmap);

            PutDataMapRequest dataMap = PutDataMapRequest.create(getString(R.string.put_weather_path));

            dataMap.getDataMap().putString(KEY_MIN_TEMP,formattedMinTemperature);
            dataMap.getDataMap().putString(KEY_MAX_TEMP,formattedMaxTemperature);
            dataMap.getDataMap().putAsset(KEY_WEATHER_IMAGE,asset);

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
    private Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }
}

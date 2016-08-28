package com.example.android.sunshine.app;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by rashwan on 8/28/16.
 */

public class MessagesService extends IntentService{
    private static final String TAG  = MessagesService.class.getSimpleName();
    public static final String SYNC_IMMEDIATELY_ACTION = "com.example.android.sunshine.app.SYNC_IMMEDIATELY_ACTION";
    private static final String GET_WEATHER_MESSAGE_PATH = "/get_weather";

    GoogleApiClient googleApiClient;


    public MessagesService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG,"Received Intent");
        if (intent.getAction().equals(SYNC_IMMEDIATELY_ACTION)){
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .build();

            ConnectionResult connectionResult = googleApiClient.blockingConnect(
                    10, TimeUnit.SECONDS);
            if (connectionResult.isSuccess() && googleApiClient.isConnected()){
                CapabilityApi.GetCapabilityResult result = Wearable.CapabilityApi.getCapability(
                        googleApiClient
                        ,getApplicationContext().getString(R.string.get_weather_capability_name)
                        ,CapabilityApi.FILTER_REACHABLE).await(10,TimeUnit.SECONDS);
                if (result.getStatus().isSuccess()){
                    CapabilityInfo capability = result.getCapability();
                    String nodeId = pickBestNode(capability.getNodes());
                    sendMessage(nodeId);
                }
            }
        }
    }
    private String pickBestNode(Set<Node> nodes){
        String bestNodeId = null;
        for (Node node: nodes) {
            if (node.isNearby()){
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }
    private void sendMessage(String nodeId){
        if (nodeId != null){
            Wearable.MessageApi.sendMessage(googleApiClient,nodeId,GET_WEATHER_MESSAGE_PATH
                ,"Start syncing".getBytes())
            .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                    if (!sendMessageResult.getStatus().isSuccess()){
                        Log.d(TAG,"Failed to send message");
                    }
                }
            });
        }else {
            Log.d(TAG,"Failed to get any nodes with the desired capability");
        }
    }
}

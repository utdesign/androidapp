package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseAnalytics;
import com.parse.ParseInstallation;
import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONObject;

/**
 * Created by quangta93 on 4/17/15.
 */
public class CustomParsePushBroadcastReceiver extends ParsePushBroadcastReceiver {
    private static final String TAG = CustomParsePushBroadcastReceiver.class.getSimpleName();
    public static final String ACTION_PARSE_RECEIVE = "ACTION_PARSE_RECEIVE";
    public static final String EXTRA_INSTRUCTION = "EXTRA_INSTRUCTION";
    private static final String EXTRA_ALERT = "alert";

    @Override
    protected void onPushReceive(Context context, Intent intent) {
        Log.d(TAG, "action = " + intent.getAction());

        JSONObject receivedJSON = null;
        try {
            Bundle receivedMessage = intent.getExtras();
            if (receivedMessage.containsKey(ParsePushBroadcastReceiver.KEY_PUSH_CHANNEL)) {
                Log.d(TAG, receivedMessage.getString(ParsePushBroadcastReceiver.KEY_PUSH_CHANNEL));
            }
            if (receivedMessage.containsKey(ParsePushBroadcastReceiver.KEY_PUSH_DATA)) {
                Log.d(TAG, receivedMessage.getString(ParsePushBroadcastReceiver.KEY_PUSH_DATA));
                receivedJSON = new JSONObject(receivedMessage.getString(ParsePushBroadcastReceiver.KEY_PUSH_DATA));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (receivedJSON != null) {
            Intent outgoingIntent = new Intent();
            outgoingIntent.setAction(ACTION_PARSE_RECEIVE);
            try {
                outgoingIntent.putExtra(EXTRA_INSTRUCTION, receivedJSON.getString(EXTRA_ALERT));
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            Log.d(TAG, "Push Receiver broadcasts action " + outgoingIntent.getAction());
            context.sendBroadcast(outgoingIntent);
        }
    }
}


package com.example.android.bluetoothlegatt;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONObject;

/**
 * Created by quangta93 on 4/17/15.
 */
public class CustomParsePushReceiver extends ParsePushBroadcastReceiver {
    private static final String TAG = CustomParsePushReceiver.class.getSimpleName();

    // Broadcast Intent attributes
    public static final String ACTION_PARSE_RECEIVE = "ACTION_PARSE_RECEIVE";
    public static final String EXTRA_INSTRUCTION = "EXTRA_INSTRUCTION";
    public static final String EXTRA_SESSION_ID = "EXTRA_SESSION";

    // Parse Notification attributes
    public static final String EXTRA_COMMAND = "command";
    public static final String EXTRA_SESSION = "session";

    @Override
    protected void onPushReceive(Context context, Intent intent) {
        Log.d(TAG, "action = " + intent.getAction());

        try {
            Bundle receivedMessage = intent.getExtras();
            JSONObject receivedJSON;
            Log.d(TAG, "KEY_PUSH_DATA = " + receivedMessage.getString(ParsePushBroadcastReceiver.KEY_PUSH_DATA));
            receivedJSON = new JSONObject(receivedMessage.getString(ParsePushBroadcastReceiver.KEY_PUSH_DATA));
            if (receivedJSON.length() > 0) {
                Intent broadcastIntent = new Intent().setAction(ACTION_PARSE_RECEIVE);
                broadcastIntent.putExtra(EXTRA_INSTRUCTION, receivedJSON.getString(EXTRA_COMMAND));
                broadcastIntent.putExtra(EXTRA_SESSION_ID, receivedJSON.getString(EXTRA_SESSION));
                context.sendBroadcast(broadcastIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}


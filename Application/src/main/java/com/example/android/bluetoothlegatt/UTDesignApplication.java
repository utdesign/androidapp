package com.example.android.bluetoothlegatt;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import com.parse.Parse;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.ParsePushBroadcastReceiver;
import com.parse.PushService;
import com.parse.SaveCallback;

/**
 * Created by quangta93 on 2/21/15.
 */
public class UTDesignApplication extends Application {
    private static final String TAG = UTDesignApplication.class.getSimpleName();    // Log tag
    private static UTDesignApplication singleton;

    public UTDesignApplication getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;

        // Initialize Parse
        Parse.initialize(this, Constant.PARSE_APPLICATION_ID, Constant.PARSE_CLIENT_KEY);
        Parse.setLogLevel(Parse.LOG_LEVEL_VERBOSE);
        // Subscribe to Parse broadcast messages.
        ParsePush.subscribeInBackground(Constant.DEFAULT_CHANNEL, new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d(TAG, "Successfully subscribed to the broadcast channel.");
                } else {
                    Log.e(TAG, "Failed to subscribe for push", e);
                }
            }
        });
        // Store current Parse configuration.
        ParseInstallation.getCurrentInstallation().saveInBackground();
    }
}

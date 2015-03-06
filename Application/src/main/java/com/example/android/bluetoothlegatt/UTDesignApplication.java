package com.example.android.bluetoothlegatt;

import android.app.Application;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParsePush;
import com.parse.SaveCallback;

/**
 * Created by quangta93 on 2/21/15.
 */
public class UTDesignApplication extends Application {

    private static final String TAG = UTDesignApplication.class.getSimpleName();    // Log tag
    private static UTDesignApplication singleton;

    public static final String PARSE_TAG = "com.parse.push";

    public UTDesignApplication getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;

        // Initialize Parse
        Parse.initialize(this, Utility.PARSE_APPLICATION_ID, Utility.PARSE_CLIENT_KEY);
        ParsePush.subscribeInBackground("", new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d(PARSE_TAG, "successfully subscribed to the broadcast channel.");
                } else {
                    Log.e(PARSE_TAG, "failed to subscribe for push", e);
                }
            }
        });

    }
}

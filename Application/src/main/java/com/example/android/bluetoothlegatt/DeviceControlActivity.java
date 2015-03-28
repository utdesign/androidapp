/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jpardogo.android.googleprogressbar.library.FoldingCirclesDrawable;

import roboguice.inject.InjectView;

/**
 * For a given BLE device, this Activity provides the user interface to send instructions and
 * receive responses supported by the device.  The Activity communicates with {@code BluetoothLeService},
 * which in turn interacts with the Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {

    private static final String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_CONNECTION_METHOD = "CONNECTION_METHOD";
    public static final String BLUETOOTH_METHOD = "BLUETOOTH";
    public static final String WIFI_METHOD = "WIFI";
    public static final int NUMBER_OF_REPEATED_WRITE = 20;

    // List of available instructions
    public static final String GET_INSTRUCTION = "get ";
    public static final String PUT_INSTRUCTION = "put ";
    public static final String SLEEP_INSTRUCTION = "sleep ";

    private String mDeviceName;
    private String mDeviceAddress;
    private String mConnectionMethod;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mGattCharacteristic;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private boolean mConnected = false;
    private boolean isGraphChecked = false;
    private int mPinNumber;
    private int mPinValue;
    private int mSleepDuration;

    @InjectView(R.id.tv_response)
    private TextView mResponse;
    @InjectView(R.id.tv_last_command)
    private TextView mLastInstruction;
    @InjectView(R.id.btn_read)
    private Button mReadButton;
    @InjectView(R.id.btn_get)
    private Button mGetButton;
    @InjectView(R.id.btn_put)
    private Button mPutButton;
    @InjectView(R.id.btn_sleep)
    private Button mSleepButton;
    @InjectView(R.id.checkbox_graph)
    private CheckBox mGraphCheckBox;
    @InjectView(R.id.et_get_pin)
    private EditText mGetPinEditText;
    @InjectView(R.id.et_put_pin)
    private EditText mPutPinEditText;
    @InjectView(R.id.et_put_value)
    private EditText mPutValEditText;
    @InjectView(R.id.et_sleep_ms)
    private EditText mSleepDurationEditText;

    ProgressBar mProgressBar;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                onBackPressed();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a result of read
    // or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                setControlView();
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                // If Bluetooth service is disconnected, leave for front page
                mConnected = false;
                Toast.makeText(DeviceControlActivity.this, "Device has been disconnected.", Toast.LENGTH_SHORT).show();
                finish();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Search all the supported services and characteristics on the user interface.
                setupBluetoothLeService();
            }
        }
    };

    private void setupBluetoothLeService() {
        if (mBluetoothLeService == null) return;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : mBluetoothLeService.getSupportedGattServices()) {
            final String serviceUuid = gattService.getUuid().toString();

            if (serviceUuid.equalsIgnoreCase(Constant.DEFAULT_MSP_SERVICE_UUID)) {
                // Found default GATT Service
                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                    String uuid = gattCharacteristic.getUuid().toString();
                    // loop up characteristic based on uuid
                    if (uuid.equalsIgnoreCase(Constant.DEFAULT_MSP_CHARACTERISTIC_UUID)) {
                        mGattCharacteristic = gattCharacteristic;
                    }
                    break;
                }
            }
        }
    }

    private void onCommanSet() {
        if (Util.isCharacteristicReadable(mGattCharacteristic)) {
            // If there is an active notification on a characteristic, clear
            // it first so it doesn't update the data field on the user interface.
            if (mNotifyCharacteristic != null) {
                mBluetoothLeService.setCharacteristicNotification(
                        mNotifyCharacteristic, false);
                mNotifyCharacteristic = null;
            }
            mBluetoothLeService.readCharacteristic(mGattCharacteristic);
        }
        if (Util.isCharacteristicWritable(mGattCharacteristic)) {
            String command = "put 14 0\n";
            mGattCharacteristic.setValue(command);
            mBluetoothLeService.writeCharacteristic(mGattCharacteristic);
        } else {
            Toast.makeText(getApplicationContext(), "Characteristic is not writable!", Toast.LENGTH_LONG).show();
        }
        if (Util.isCharacteristicNotifiable(mGattCharacteristic)) {
            mNotifyCharacteristic = mGattCharacteristic;
            mBluetoothLeService.setCharacteristicNotification(
                    mGattCharacteristic, true);
        }
    }

    /**
     * Inflates the Folding Circle {@link ProgressBar} while connecting to device.
     */
    private void setProgressBar() {
        setContentView(R.layout.google_progress_bar);
        mProgressBar = (ProgressBar) findViewById(R.id.google_progress);
        mProgressBar.setIndeterminateDrawable(new FoldingCirclesDrawable.Builder(this).
                colors(getProgressDrawableColors()).build());
    }

    /**
     * Returns an array of colors that is displaying in the Folding Circle {@link ProgressBar} .
     * @return Color array.
     */
    private int[] getProgressDrawableColors() {
        int[] colors = new int[4];
        colors[0] = getResources().getColor(R.color.red);
        colors[1] = getResources().getColor(R.color.blue);
        colors[2] = getResources().getColor(R.color.yellow);
        colors[3] = getResources().getColor(R.color.green);
        return colors;
    }

    /**
     * Inflates the activity's view after a device is connected.
     */
    private void setControlView() {
        // Disable progress bar to save resources
        mProgressBar = null;

        // Inflate control view
        setContentView(R.layout.activity_device_control);

        // Set listeners for buttons
        mReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetoothLeService == null) {
                    Log.w(TAG, "BluetoothLeService == null");
                    return;
                }
                mBluetoothLeService.readCharacteristic();
            }
        });
        mGetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPinNumber = Integer.parseInt(mGetPinEditText.getText().toString());
                String instruction = GET_INSTRUCTION + mPinNumber + "\n";
                if (isGraphChecked) {
                    // Request repeatedly and graph data
                    writeRepeat(instruction);
                } else {
                    // Request once
                    writeToDevice(instruction);
                }
            }
        });
        mPutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPinNumber = Integer.parseInt(mPutPinEditText.getText().toString());
                mPinValue = Integer.parseInt(mPutValEditText.getText().toString());
                writeToDevice(PUT_INSTRUCTION + mPinNumber + " " + mPinValue + "\n");
            }
        });
        mSleepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSleepDuration = Integer.parseInt(mSleepDurationEditText.getText().toString());
                writeToDevice(SLEEP_INSTRUCTION + mSleepDuration + "\n");
            }
        });
        mGraphCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isGraphChecked = isChecked;
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mConnectionMethod = intent.getStringExtra(EXTRAS_CONNECTION_METHOD);
        try {
            getActionBar().setTitle(mDeviceName);
            getActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException e) {
            Log.w(TAG, "NullPointerException when trying to getActionBar().");
        }

        if (mConnectionMethod.equalsIgnoreCase(BLUETOOTH_METHOD)) {
            // Bind BluetoothLeService to this activity
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

            setProgressBar();
        } else if (mConnectionMethod.equalsIgnoreCase(WIFI_METHOD)) {
            setControlView();
        }
    }

    /**
     * Call {@code BluetoothLeService} to write selected command to connected device.
     *
     * @param instruction Requested command.
     */
    private void writeToDevice(String instruction) {
        if (mBluetoothLeService == null) {
            Log.w(TAG, "BluetoothLeService == null");
            return;
        }
        mLastInstruction.setText(instruction.trim());
        mBluetoothLeService.writeCharacteristic(instruction);
    }

    /**
     * Call {@code BluetoothLeService} to write selected command repeatedly to connected device;
     *
     * @param instruction
     */
    private void writeRepeat(String instruction) {
        if (mBluetoothLeService == null) {
            Log.w(TAG, "BluetoothLeService == null");
            return;
        }
        mLastInstruction.setText(instruction.trim());
        for (int counter = 0; counter < NUMBER_OF_REPEATED_WRITE; counter++) {
            mBluetoothLeService.writeCharacteristic(instruction);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    private void displayData(String data) {
        if (data != null) {
            mResponse.setText(data);
        }
    }

    public static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE_READ);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE_WRITE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE_NOTIFY);
        return intentFilter;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }
}

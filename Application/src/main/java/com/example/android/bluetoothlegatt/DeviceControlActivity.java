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
import android.bluetooth.BluetoothGattDescriptor;
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
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.jpardogo.android.googleprogressbar.library.FoldingCirclesDrawable;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to send instructions and
 * receive responses supported by the device.  The Activity communicates with {@code BluetoothLeService},
 * which in turn interacts with the Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private static final String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String NO_DATA_PRESENT = "NO DATA PRESENT";
    // General Configuration for Page.
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_CONNECTION_METHOD = "CONNECTION_METHOD";
    public static final String BLUETOOTH_METHOD = "BLUETOOTH";
    public static final String WIFI_METHOD = "WIFI";

    // Default MSP430 Configuration
    public static final String DEFAULT_MSP430_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static final String DEFAULT_MSP430_READ_CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static final String DEFAULT_MSP430_WRITE_CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static final String DEFAULT_MSP430_NOTIFY_CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";

    // List of available instructions
    public static final String GET_INSTRUCTION = "get ";
    public static final String PUT_INSTRUCTION = "put ";
    public static final String SLEEP_INSTRUCTION = "sleep ";
    public static final String IF_INSTRUCTION = "if ";
    public static final String ENDIF_INSTRUCTION = "endif";

    private String mDeviceName;
    private String mDeviceAddress;
    private boolean isBluetoothConnection;

    // Bluetooth Gatt Elements
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mReadCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private boolean isReadRequested = false;
    private boolean isWriteRequested = false;

    // Wifi Elements
    private RequestQueue mRequestQueue;

    // Device Control Elements
    private boolean isBluetoothConnected = false;
    private int mPinNumber;
    private int mPinValue;
    private int mSleepDuration;

    // UI Elements
    private ProgressBar mProgressBar;

    private TextView mResponse;
    private TextView mLastInstruction;
    private Button mReadButton;
    private Button mGetButton;
    private Button mPutButton;
    private Button mSleepButton;
    private CheckBox mGraphCheckBox;
    private EditText mGetPinEditText;
    private EditText mPutPinEditText;
    private EditText mPutValEditText;
    private EditText mSleepDurationEditText;
    private Button mIfOperatorButton;
    private Button mIfButton;
    private Button mEndIfButton;
    private EditText mIfLhsEditText;
    private EditText mIfRhsEditText;

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
            Log.w(TAG, "service is disconnected.");
            mBluetoothLeService = null;
            finish();
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a result of read or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                isBluetoothConnected = true;
                setControlView();
                invalidateOptionsMenu();

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                // If Bluetooth service is disconnected, leave for front page
                isBluetoothConnected = false;
                Toast.makeText(DeviceControlActivity.this, "Gatt profile has been disconnected.", Toast.LENGTH_SHORT).show();
                finish();

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Search all the supported services and characteristics on the user interface.
                setupBluetoothLeService();

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE_WRITE.equals(action)) {
                if (isWriteRequested) {
                    // Display write response.
                    isWriteRequested = false;
                    byte[] responseData = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    if (responseData != null) {
                        String writeResponse = new String(responseData).trim();
                        if (!mLastInstruction.getText().toString().trim().equalsIgnoreCase(writeResponse)) {
                            Toast.makeText(DeviceControlActivity.this, "write request and response do not match.",
                                    Toast.LENGTH_LONG).show();
                            DeviceControlActivity.this.finish();
                        }
                    } else {
                        Log.w(TAG, "Response data is null.");
                    }
                }

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE_READ.equals(action)) {
                if (isReadRequested) {
                    isReadRequested = false;
                    byte[] responseData = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    if (responseData != null) {
                        String responseText = new String(responseData).trim();
                        if (responseText.length() == 0) {
                            responseText = NO_DATA_PRESENT;
                        }
                        mResponse.setText(responseText);
                    } else {
                        Log.w(TAG, "Response data is null.");
                    }
                }

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE_NOTIFY.equals(action)) {
                byte[] notifiedData = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                if (notifiedData != null) {
                    String notifiedText = new String(notifiedData).trim();
                    if (notifiedText.length() == 0) {
                        notifiedText = NO_DATA_PRESENT;
                    } else {
                        // Handle incoming data
                    }
                    mResponse.setText(notifiedText);
                } else {
                    Log.w(TAG, "Response data is null.");
                }
            }
        }
    };

    /**
     * Setup {@link com.example.android.bluetoothlegatt.BluetoothLeService} by specifying default
     * {@link android.bluetooth.BluetoothGattService} and read, write, notify {@link android.bluetooth.BluetoothGattCharacteristic}.
     * Called only if the device connects to launchpad via Bluetooth.
     */
    private void setupBluetoothLeService() {
        if (mBluetoothLeService == null) {
            Log.w(TAG, "no BLE service in background");
            finish();
        }
        List<BluetoothGattService> gattServices = mBluetoothLeService.getSupportedGattServices();
        if (gattServices == null) {
            Log.w(TAG, "no GATT service found.");
            finish();
        }

        // Look for default service
        for (BluetoothGattService gattService : gattServices) {
            clearCharacteristicSetup();

            // MSP430 launchpad
            if (gattService.getUuid().toString().equalsIgnoreCase(DEFAULT_MSP430_SERVICE_UUID)) {
                if (bluetoothLookup(gattService, DEFAULT_MSP430_SERVICE_UUID, DEFAULT_MSP430_READ_CHARACTERISTIC_UUID,
                        DEFAULT_MSP430_WRITE_CHARACTERISTIC_UUID, DEFAULT_MSP430_NOTIFY_CHARACTERISTIC_UUID)) {
                    Log.i(TAG, "Finished setting up for MSP430.");
                    return;
                } else {
                    clearCharacteristicSetup();
                }
            }
            // The other one
            if (gattService.getUuid().toString().equalsIgnoreCase(GraphActivity.DEFAULT_GRAPH_SERVICE_UUID)) {
                if (bluetoothLookup(gattService, GraphActivity.DEFAULT_GRAPH_SERVICE_UUID, GraphActivity.DEFAULT_GRAPH_READ_CHARACTERISTIC_UUID,
                        GraphActivity.DEFAULT_GRAPH_WRITE_CHARACTERISTIC_UUID, GraphActivity.DEFAULT_GRAPH_NOTIFY_CHARACTERISTIC_UUID)) {
                    Log.i(TAG, "Launchpad is setup for graph activity only.");
                    return;
                } else {
                    clearCharacteristicSetup();
                }
            }
        }
        Log.w(TAG, "Bluetooth configuration is not setup.");
        Toast.makeText(this, "Bluetooth configuration could not be setup.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void clearCharacteristicSetup() {
        mReadCharacteristic = null;
        mWriteCharacteristic = null;
        mNotifyCharacteristic = null;
    }

    private boolean bluetoothLookup(BluetoothGattService gattService, String serviceUuid, String readCharacteristicUuid,
                                    String writeCharacteristicUuid, String notifyCharacteristicUuid) {
        for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
            final String uuid = characteristic.getUuid().toString();
            List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
            for (BluetoothGattDescriptor descriptor : descriptors) {
                Log.d(TAG, "descriptor = " + descriptor.toString());
            }
            if (uuid.toString().equalsIgnoreCase(readCharacteristicUuid) && Util.isCharacteristicReadable(characteristic)) {
                Log.d(TAG, "Setup characteristic read " + uuid.substring(4, 8));
                mReadCharacteristic = characteristic;
                // If there is an active notification on a characteristic, clear
                // it first so it doesn't update the data field on the user interface.
                if (mNotifyCharacteristic != null) {
                    mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
                    mNotifyCharacteristic = null;
                }
            }
            if (uuid.toString().equalsIgnoreCase((writeCharacteristicUuid)) && Util.isCharacteristicWritable(characteristic)) {
                Log.d(TAG, "Setup characteristic write " + uuid.substring(4, 8));
                mWriteCharacteristic = characteristic;
            }
            if (uuid.toString().equalsIgnoreCase(notifyCharacteristicUuid) && Util.isCharacteristicNotifiable(characteristic)) {
                Log.d(TAG, "Setup characteristic notify " + uuid.substring(4, 8));
                mNotifyCharacteristic = characteristic;
                mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, true);
            }
        }
        return (mReadCharacteristic != null && mWriteCharacteristic != null);
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
     *
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

        // Inject views
        mResponse = (TextView) findViewById(R.id.tv_response);
        mLastInstruction = (TextView) findViewById(R.id.tv_last_command);
        mReadButton = (Button) findViewById(R.id.btn_read);
        mGetButton = (Button) findViewById(R.id.btn_get);
        mGetPinEditText = (EditText) findViewById(R.id.et_get_pin);
        mGraphCheckBox = (CheckBox) findViewById(R.id.checkbox_graph);
        mPutButton = (Button) findViewById(R.id.btn_put);
        mPutPinEditText = (EditText) findViewById(R.id.et_put_pin);
        mPutValEditText = (EditText) findViewById(R.id.et_put_value);
        mSleepButton = (Button) findViewById(R.id.btn_sleep);
        mSleepDurationEditText = (EditText) findViewById(R.id.et_sleep_ms);
        mIfOperatorButton = (Button) findViewById(R.id.btn_if_operator);
        mIfButton = (Button) findViewById(R.id.btn_if);
        mEndIfButton = (Button) findViewById(R.id.btn_endif);
        mIfLhsEditText = (EditText) findViewById(R.id.et_if_lhs);
        mIfRhsEditText = (EditText) findViewById(R.id.et_if_rhs);

        // Set listeners for buttons
        mReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetoothLeService == null) {
                    Log.w(TAG, "BluetoothLeService == null");
                    return;
                }
                if (mReadCharacteristic == null) {
                    Log.w(TAG, "Bluetooth configuration is not setup properly.");
                    return;
                }

                isReadRequested = true;
                mBluetoothLeService.readCharacteristic(mReadCharacteristic);
            }
        });
        mGetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mGraphCheckBox != null) {
                    // no try-catch needed since the input is restricted to numbers.
                    String num = mGetPinEditText.getText().toString().trim();
                    if (num.length() > 0) {
                        mPinNumber = Integer.parseInt(num);
                    } else {
                        Log.w(TAG, "Invalid pin number");
                        return;
                    }
                    String instruction = GET_INSTRUCTION + mPinNumber + "\n";
                    if (mGraphCheckBox.isChecked()) {
                        openGraphActivity(instruction);
                    } else {
                        // Request once
                        sendInstruction(instruction);
                    }
                }
            }
        });
        mPutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String num = mPutPinEditText.getText().toString().trim();
                if (num.length() > 0) {
                    mPinNumber = Integer.parseInt(num);
                } else {
                    Log.w(TAG, "Invalid pin number");
                    return;
                }
                String val = mPutValEditText.getText().toString().trim();
                if (val.length() > 0) {
                    mPinValue = Integer.parseInt(val);
                } else {
                    Log.w(TAG, "Invalid pin value");
                    return;
                }
                sendInstruction(PUT_INSTRUCTION + mPinNumber + " " + mPinValue + "\n");
            }
        });
        mSleepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // no try-catch needed since the input is restricted to numbers.
                String sleepDuration = mSleepDurationEditText.getText().toString().trim();
                if (sleepDuration.length() > 0) {
                    mSleepDuration = Integer.parseInt(sleepDuration);
                    sendInstruction(SLEEP_INSTRUCTION + mSleepDuration + "\n");
                } else {
                    Log.w(TAG, "Invalid sleep duration");
                }
            }
        });
        mIfOperatorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn = (Button) v;
                if (btn.getText().toString().equalsIgnoreCase("<")) {
                    btn.setText("=");
                    return;
                }
                if (btn.getText().toString().equalsIgnoreCase("=")) {
                    btn.setText(">");
                    return;
                }
                if (btn.getText().toString().equalsIgnoreCase(">")) {
                    btn.setText("<");
                    return;
                }
            }
        });
        mIfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String lhs = mIfLhsEditText.getText().toString().trim();
                if (lhs.length() > 0) {
                    mPinNumber = Integer.parseInt(lhs);
                } else {
                    Log.w(TAG, "Invalid pin number.");
                    return;
                }
                String rhs = mIfRhsEditText.getText().toString().trim();
                if (rhs.length() > 0) {
                    mPinValue = Integer.parseInt(rhs);
                } else {
                    Log.w(TAG, "Invalid pin number.");
                    return;
                }
                sendInstruction(IF_INSTRUCTION + mPinNumber + " " + mIfOperatorButton.getText().toString().trim() + " " + mPinValue + "\n");
            }
        });
        mEndIfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendInstruction(ENDIF_INSTRUCTION + "\n");
            }
        });
    }

    private void openGraphActivity(String instruction) {
        // Open graph page to display 4K data.
        Intent intent = new Intent(DeviceControlActivity.this, GraphActivity.class);
        intent.putExtra(EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(GraphActivity.EXTRAS_GET_INSTRUCTION, instruction);
        if (isBluetoothConnection) {
            mBluetoothLeService.disconnect();
            intent.putExtra(EXTRAS_CONNECTION_METHOD, BLUETOOTH_METHOD);
        } else {
            intent.putExtra(EXTRAS_CONNECTION_METHOD, WIFI_METHOD);
        }
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        if (intent.hasExtra(EXTRAS_DEVICE_NAME)) {
            mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        } else {
            Log.d(TAG, "No device name");
            finish();
        }
        if (intent.hasExtra(EXTRAS_DEVICE_ADDRESS)) {
            mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        } else {
            Log.d(TAG, "No device address");
            finish();
        }
        if (intent.hasExtra(EXTRAS_CONNECTION_METHOD)) {
            String connectionMethod = intent.getStringExtra(EXTRAS_CONNECTION_METHOD);
            isBluetoothConnection = connectionMethod.equalsIgnoreCase(BLUETOOTH_METHOD);
        } else {
            Log.d(TAG, "No connection method.");
            finish();
        }
        try {
            getActionBar().setTitle(mDeviceName);
            getActionBar().setHomeButtonEnabled(true);
            getActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException e) {
            Log.w(TAG, "NullPointerException when trying to getActionBar().");
        }

        if (isBluetoothConnection) {
            // Bind BluetoothLeService to this activity
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

            setProgressBar();
        } else {
            mRequestQueue = Volley.newRequestQueue(this);
            setControlView();
        }
    }

    /**
     * Send instruction via determined communication channel.
     *
     * @param instruction Instruction to send
     */
    private void sendInstruction(String instruction) {
        if (isBluetoothConnection) {
            if (mBluetoothLeService == null) {
                Log.w(TAG, "BluetoothLeService == null");
                return;
            }

            isWriteRequested = true;
            mLastInstruction.setText(instruction.trim());
            mWriteCharacteristic.setValue(instruction);
            mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);

        } else {
            mLastInstruction.setText(instruction.trim());
            isWriteRequested = true;
            invalidateOptionsMenu();

            // TODO: send wifi request
            // Volley request
            String url = null;
            HashMap<String, String> params = new HashMap<>();
            params.put("command", instruction);
            CustomRequest request = new CustomRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    isWriteRequested = false;
                }
            });
            mRequestQueue.add(request);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_control, menu);
        if (isReadRequested || isWriteRequested) {
            menu.findItem(R.id.action_requesting).setVisible(true);
            menu.findItem(R.id.action_requesting).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        } else {
            menu.findItem(R.id.action_requesting).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_disconnect) {
            mBluetoothLeService.disconnect();
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isBluetoothConnection) {
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
            if (mBluetoothLeService != null) {
                final boolean result = mBluetoothLeService.connect(mDeviceAddress);
                Log.d(TAG, "Connect request result=" + result);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (isBluetoothConnection) {
            unregisterReceiver(mGattUpdateReceiver);
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

        if (isBluetoothConnection) {
            unbindService(mServiceConnection);
            mBluetoothLeService = null;
        } else {
            mRequestQueue.cancelAll(TAG);
        }
    }
}

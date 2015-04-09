package com.example.android.bluetoothlegatt;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.jpardogo.android.googleprogressbar.library.FoldingCirclesDrawable;

import java.util.ArrayList;
import java.util.List;


public class GraphActivity extends Activity {
    private static final String TAG = GraphActivity.class.getSimpleName();

    private static final boolean NO_DATA = true;
    public static final int WRITE_VALUE_FFT = 0x5E;
    public static final int WRITE_VALUE_SAMPLE = 0x5D;
    public static final int MESSAGE_PACKAGE_SIZE = 20;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_GRAPH_INSTRUCTION = "GET_INSTRUCTION";

    public static final String DEFAULT_GRAPH_SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    public static final String DEFAULT_GRAPH_READ_CHARACTERISTIC_UUID = "0000fff5-0000-1000-8000-00805f9b34fb";
    public static final String DEFAULT_GRAPH_WRITE_CHARACTERISTIC_UUID = "0000fff1-0000-1000-8000-00805f9b34fb";
    public static final String DEFAULT_GRAPH_NOTIFY_CHARACTERISTIC_UUID = "0000fff5-0000-1000-8000-00805f9b34fb";

    public static final String MODE_FFT = "FFT";
    public static final String MODE_SAMPLE = "SAMPLE";
    public static final String SAMPLE_DESCRIPTION = "Sample Number (X 2.4 = Hertz)";
    public static final String FFT_DESCRIPTION = "FFT Number (X x.x = Hertz)";
    public static final String MSP_DESCRIPTION = "PIN VALUES";

    private String mDeviceAddress;
    private String mDeviceName;
    private byte[] mGraphInstruction;
    private boolean isBluetoothConnection;
    private boolean isMsp430 = true;
    private String mode = MODE_FFT;

    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mReadCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private RequestQueue mRequestQueue;
    private boolean isSetGraphView = false;
    private int mExpectedPackageNumber = 0;
    private LineChart mChart;
    private ArrayList<String> xVals;
    private ArrayList<Entry> mEntries;
    private ArrayList<Entry> mEntryBuffer;

    private ProgressBar mProgressBar;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");

                // Back to the Scan page.
                startActivity(new Intent(GraphActivity.this, DeviceScanActivity.class));
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            // Unbind service
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Toast.makeText(GraphActivity.this, "Device connected!", Toast.LENGTH_SHORT).show();
                return;

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(GraphActivity.this, "Device disconnected!", Toast.LENGTH_SHORT).show();
                return;

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Setup the default characteristics to read, write, and notify
                // ALso, send a command to start receiving serial data
                setupBluetooth();
                return;
            }
            if (isMsp430) {
                // In case of MSP 430, data comes from notification characteristic.
                // TODO: send once, get many? need agreement with hardware developers.
                if (BluetoothLeService.ACTION_DATA_AVAILABLE_READ.equals(action)) {
                    byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    Log.i(TAG, "read data = " + new String(data));

                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE_WRITE.equals(action)) {
                    byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    Log.i(TAG, "written data = " + new String(data));

                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE_NOTIFY.equals(action)) {
                    // Once the instruction is written to MSP430, we only get graph data via notification characteristic.
                    byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    Log.i(TAG, "notified data = " + new String(data));
                    mExpectedPackageNumber++;

                    // Append new data set to the entry buffer.
                    int size = mEntryBuffer.size();
                    for (int i = 0; i < data.length; i++) {
                        // This will iterate through 20 bytes data.
                        int val = 0;
                        if (NO_DATA) {
                            val = ((int) (Math.random() * 200));
                        } else {
                            val = data[i];
                        }
                        mEntryBuffer.add(new Entry(val, size++));
                    }

                    if (mExpectedPackageNumber == MESSAGE_PACKAGE_SIZE) {
                        mExpectedPackageNumber = 0;
                        mEntries = new ArrayList<>(mEntryBuffer);
                        mEntryBuffer.clear();
                        if (!isSetGraphView) {
                            setGraphView();
                        }
                        draw();
                    }
                }
            } else {
                // SimpleBlePeripheral: data comes from reading characteristic repeatedly.
                // TODO: SimpleBlePeripheral read or notify?
                if (BluetoothLeService.ACTION_DATA_AVAILABLE_READ.equals(action)) {
                    // Start populating data
                    byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    Log.i(TAG, "data[" + data[0] + "] = " + Util.bytesToHexString(data));
                    Log.i(TAG, "expected package = " + mExpectedPackageNumber);

                    if (data[0] == mExpectedPackageNumber) {
                        mExpectedPackageNumber++;

                        // Add new data to the data set
                        int size = mEntries.size();
                        for (int i = 0; i < data.length - 1; i++) {
                            // Unlike MSP 430 case, this loop will iterate through 19 bytes data
                            // because the first byte is the package number.
                            int val = 0;
                            if (NO_DATA) {
                                val = ((int) (Math.random() * 200));
                            } else {
                                val = data[i + 1];
                            }
                            mEntries.add(new Entry(val, size++));
                        }
                    }

                    if (mExpectedPackageNumber < MESSAGE_PACKAGE_SIZE) {
                        mBluetoothLeService.readCharacteristic(mReadCharacteristic);
                    } else {
                        // Draw graph after read all data
                        if (!isSetGraphView) {
                            setGraphView();
                        }
                        draw();
                    }

                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE_WRITE.equals(action)) {
                    byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    Log.i(TAG, "write data = " + Util.bytesToHexString(data));

                    if (data[0] != WRITE_VALUE_FFT) {
                        Log.i(TAG, "write data not the same as sent.");
                        return;
                    }
                    // Start reading data
                    mBluetoothLeService.readCharacteristic(mReadCharacteristic);

                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE_NOTIFY.equals(action)) {
                    byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    Log.i(TAG, "Not sure about this. Notified data = " + Util.bytesToHexString(data));
                }
            }
        }
    };

    // Displays a Google Circle-Folding Progress Bar
    private void setProgressBar() {
        setContentView(R.layout.google_progress_bar);
        if (mProgressBar == null) {
            mProgressBar = (ProgressBar) findViewById(R.id.google_progress);
        }
        mProgressBar.setIndeterminateDrawable(new FoldingCirclesDrawable.Builder(this).
                colors(getProgressDrawableColors()).build());
        isSetGraphView = false;
    }

    private void setGraphView() {
        isSetGraphView = true;
        setContentView(R.layout.activity_graph);

        if (isBluetoothConnection) {
            if (isMsp430) {
                mChart.setDescription(MSP_DESCRIPTION);
            } else {
                // SimpleBlePeripheral
                if (mode.equalsIgnoreCase(MODE_FFT)) {
                    mChart.setDescription(FFT_DESCRIPTION);
                } else if (mode.equalsIgnoreCase(MODE_SAMPLE)) {
                    mChart.setDescription(SAMPLE_DESCRIPTION);
                }
            }
        }
        if (mChart == null) {
            mChart = (LineChart) findViewById(R.id.linechart);
            mChart.setDescriptionTextSize(14);
            mChart.setHighlightEnabled(false);
            mChart.setDrawGridBackground(true);
            mChart.setTouchEnabled(true);
            mChart.setDragEnabled(true);
            mChart.setScaleEnabled(true);
            mChart.setPinchZoom(false);

            XAxis x = mChart.getXAxis();
            YAxis y = mChart.getAxisLeft();
            y.setLabelCount(5);
            mChart.getAxisRight().setEnabled(false);
        }
    }

    private int[] getProgressDrawableColors() {
        int[] colors = new int[4];
        colors[0] = getResources().getColor(R.color.red);
        colors[1] = getResources().getColor(R.color.blue);
        colors[2] = getResources().getColor(R.color.yellow);
        colors[3] = getResources().getColor(R.color.green);
        return colors;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setProgressBar();

        final Intent intent = getIntent();
        // Get device name
        if (intent.hasExtra(EXTRAS_DEVICE_NAME)) {
            mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        } else {
            Log.d(TAG, "No device name");
            GraphActivity.this.finish();
        }
        // Get device address
        if (intent.hasExtra(EXTRAS_DEVICE_ADDRESS)) {
            mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        } else {
            Log.d(TAG, "No device address");
            GraphActivity.this.finish();
        }
        // Get device connection method: Bluetooth or Wifi
        if (intent.hasExtra(DeviceControlActivity.EXTRAS_CONNECTION_METHOD)) {
            String connectionMethod = intent.getStringExtra(DeviceControlActivity.EXTRAS_CONNECTION_METHOD);
            isBluetoothConnection = connectionMethod.equalsIgnoreCase(DeviceControlActivity.BLUETOOTH_METHOD);
        } else {
            Log.d(TAG, "No connection method");
            GraphActivity.this.finish();
        }
        if (isBluetoothConnection) {
            if (intent.hasExtra(DeviceControlActivity.EXTRAS_BLUETOOTH_DEVICE_MSP)) {
                // Get Bluetooth device type: MSP 430 or SimpleBlePeripheral
                isMsp430 = intent.getBooleanExtra(DeviceControlActivity.EXTRAS_BLUETOOTH_DEVICE_MSP, true);
            }

            if (isMsp430) {
                getGraphInstruction(intent);
            } else {
                // Set default graph instruction for SimpleBlePeripheral
                mGraphInstruction = new byte[]{WRITE_VALUE_FFT};
            }
        } else {
            // Set graph instruction for devices with Wifi connection.
            getGraphInstruction(intent);
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        try {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(mDeviceName);
        } catch (NullPointerException e) {
            Log.w(TAG, "NullPointerException when trying to action bar.");
        }

        if (isBluetoothConnection) {
            // Bind BluetoothLeService to this activity
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        } else {
            // Send get graph request to server.
        }
    }

    private void getGraphInstruction(Intent intent) {
        if (intent.hasExtra(EXTRAS_GRAPH_INSTRUCTION)) {
            mGraphInstruction = intent.getStringExtra(EXTRAS_GRAPH_INSTRUCTION).getBytes();
        } else {
            Log.d(TAG, "No get graph instruction");
            GraphActivity.this.finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu. This adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_graph, menu);

        // This item is for Bluetooth with the SimpleBlePeripheral device only.
        MenuItem toggle = menu.findItem(R.id.action_toggle);
        toggle.setVisible(isBluetoothConnection && !isMsp430);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_quit) {
            mBluetoothLeService.disconnect();
            finish();
            return true;

        } else if (id == R.id.action_toggle) {
            if (mode.equalsIgnoreCase(MODE_SAMPLE)) {
                mode = MODE_FFT;
                mGraphInstruction = new byte[]{WRITE_VALUE_FFT};
            } else if (mode.equalsIgnoreCase(MODE_FFT)) {
                mode = MODE_SAMPLE;
                mGraphInstruction = new byte[]{WRITE_VALUE_SAMPLE};
            }
            item.setTitle(mode);
            startCollectingData();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isBluetoothConnection) {
            registerReceiver(mGattUpdateReceiver, DeviceControlActivity.makeGattUpdateIntentFilter());
            if (mBluetoothLeService != null) {
                final boolean result = mBluetoothLeService.connect(mDeviceAddress);
                Log.d(TAG, "Connect request result = " + result);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isBluetoothConnection) {
            mBluetoothLeService.disconnect();
            unbindService(mServiceConnection);
            mBluetoothLeService = null;
        } else {
            mRequestQueue.cancelAll(TAG);
        }
    }

    private void startCollectingData() {
        setProgressBar();

        if (isBluetoothConnection) {
            // Prepare data array
            mEntries = new ArrayList<>();
            mEntryBuffer = new ArrayList<>();
            mExpectedPackageNumber = 0;

            // Request data
            mWriteCharacteristic.setValue(mGraphInstruction);
            mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
        } else {
            // Send Volley request
        }
    }

    /**
     * Setup {@link com.example.android.bluetoothlegatt.BluetoothLeService} by specifying default
     * {@link android.bluetooth.BluetoothGattService} and read, write, notify {@link android.bluetooth.BluetoothGattCharacteristic}.
     * Called only if the device connects to launchpad via Bluetooth.
     */
    private void setupBluetooth() {
        if (mBluetoothLeService == null) {
            Log.w(TAG, "no BLE service in background");
            GraphActivity.this.finish();
        }
        List<BluetoothGattService> gattServices = mBluetoothLeService.getSupportedGattServices();
        if (gattServices == null) {
            Log.w(TAG, "no GATT service found.");
            GraphActivity.this.finish();
        }

        // Look for default service
        for (BluetoothGattService gattService : gattServices) {
            clearCharacteristicSetup();

            if (isMsp430) {
                // MSP430
                if (gattService.getUuid().toString().equalsIgnoreCase(DeviceControlActivity.DEFAULT_MSP430_SERVICE_UUID)) {
                    if (bluetoothLookup(gattService, DeviceControlActivity.DEFAULT_MSP430_READ_CHARACTERISTIC_UUID,
                            DeviceControlActivity.DEFAULT_MSP430_WRITE_CHARACTERISTIC_UUID, DeviceControlActivity.DEFAULT_MSP430_NOTIFY_CHARACTERISTIC_UUID)) {
                        return;
                    } else {
                        clearCharacteristicSetup();
                    }
                }
            } else {
                // SimpleBlePeripheral
                if (gattService.getUuid().toString().equalsIgnoreCase(DEFAULT_GRAPH_SERVICE_UUID)) {
                    if (bluetoothLookup(gattService, DEFAULT_GRAPH_READ_CHARACTERISTIC_UUID,
                            DEFAULT_GRAPH_WRITE_CHARACTERISTIC_UUID, DEFAULT_GRAPH_NOTIFY_CHARACTERISTIC_UUID)) {
                        return;
                    } else {
                        clearCharacteristicSetup();
                    }
                }
            }
        }
        Toast.makeText(this, "Bluetooth configuration could not be setup.", Toast.LENGTH_LONG).show();
        GraphActivity.this.finish();
    }

    private void clearCharacteristicSetup() {
        mReadCharacteristic = null;
        mWriteCharacteristic = null;
        mNotifyCharacteristic = null;
    }


    /**
     * Try to match default characteristics to characteristics from a particular Gatt service.
     *
     * @return <b>true</b> if default characteristics are set up correctly.
     */
    private boolean bluetoothLookup(BluetoothGattService gattService, String readCharacteristicUuid,
                                    String writeCharacteristicUuid, String notifyCharacteristicUuid) {
        for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
            final String uuid = characteristic.getUuid().toString();

            Log.d(TAG, "Characteristic " + uuid.toString().substring(4, 8) + ": " + Util.isCharacteristicReadable(characteristic)
                    + " " + Util.isCharacteristicWritable(characteristic) + " " + Util.isCharacteristicNotifiable(characteristic));

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

    private void draw() {
        // Label X-axis
        if (xVals == null) {
            // TODO: this should be changed if size of mEntries is not consistent.
            xVals = new ArrayList<>();
            for (int i = 0; i < mEntries.size(); i++) {
                xVals.add(String.valueOf(i));
            }
        }

        // create a line data set and give it a type
        LineDataSet set1 = new LineDataSet(mEntries, "DataSet");
        set1.setDrawCubic(false);
        set1.setDrawCircles(false);
        set1.setLineWidth(2f);
        set1.setCircleSize(5f);
        set1.setHighLightColor(Color.rgb(244, 117, 117));
        set1.setColor(Color.rgb(104, 241, 175));
        set1.setFillColor(ColorTemplate.getHoloBlue());

        LineData data = new LineData(xVals, set1);
        data.setValueTextSize(9f);
        data.setDrawValues(false);

        // set data
        mChart.setData(data);
        // Start drawing the graph
        mChart.getLegend().setEnabled(false);
        mChart.animateXY(2000, 2000);
        mChart.invalidate();
    }

}

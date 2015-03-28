package com.example.android.bluetoothlegatt;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

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

    private static final boolean NO_DATA = false;
    public static final int WRITE_VALUE = 0x5E;
    public static final int NUMBER_OF_WRITE_OPERATIONS = 20;
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String DEFAULT_SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    public static final String DEFAULT_READ_CHARACTERISTIC_UUID = "0000fff5-0000-1000-8000-00805f9b34fb";
    public static final String DEFAULT_WRITE_CHARACTERISTIC_UUID = "0000fff1-0000-1000-8000-00805f9b34fb";
    public static final String DEFAULT_NOTIFY_CHARACTERISTIC_UUID = "0000fff5-0000-1000-8000-00805f9b34fb";

    private String mDeviceAddress;
    private String mDeviceName;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mReadCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private int mExpectedPackageNumber = 0;
    private LineChart mChart;
    private ArrayList<String> xVals;
    private ArrayList<Entry> mEntries;

    private ProgressBar mProgressBar;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");

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
                Log.i(TAG, "device connected");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(GraphActivity.this, "Device disconnected!", Toast.LENGTH_SHORT).show();
//                ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
//                toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
//                finish();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(TAG, "services discovered");
                // Setup the default characteristics to read, write, and notify
                // ALso, send a command to start receiving serial data
                setupBluetooth();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE_READ.equals(action)) {
                // Start populating data
                byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                Log.i(TAG, "read data[" + data[0] + "] = " + Util.bytesToHexString(data));
                Log.i(TAG, "expected package = " + mExpectedPackageNumber);

                if (data[0] == mExpectedPackageNumber) {
                    mExpectedPackageNumber++;

                    // Add new data to the data set
                    int size = mEntries.size();
                    for (int i = 0; i < data.length - 1; i++) {
                        int val = 0;
                        if (NO_DATA) {
                            val = ((int) (Math.random() * 200));
                        } else {
                            val = data[i + 1];
                        }
                        mEntries.add(new Entry(val, size++));
                    }
                }

                if (mBluetoothLeService == null || mReadCharacteristic == null) {
                    return;
                }
                if (mExpectedPackageNumber < NUMBER_OF_WRITE_OPERATIONS) {
                    mBluetoothLeService.readCharacteristic(mReadCharacteristic);
                } else {
                    // Draw graph after read all data
                    setGraphView();
                    draw();
                }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE_WRITE.equals(action)) {
                byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                Log.i(TAG, "write data = " + Util.bytesToHexString(data));

                if (data[0] != WRITE_VALUE) {
                    Log.i(TAG, "write data not the same as sent.");
                    return;
                }
                if (mBluetoothLeService == null || mReadCharacteristic == null) {
                    return;
                }

                // Start reading data
                mBluetoothLeService.readCharacteristic(mReadCharacteristic);
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE_NOTIFY.equals(action)) {
                byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                Log.i(TAG, "notified data = " + Util.bytesToHexString(data));
            }
        }
    };

    private void setProgressBar() {
        setContentView(R.layout.google_progress_bar);
        mProgressBar = (ProgressBar) findViewById(R.id.google_progress);
        mProgressBar.setIndeterminateDrawable(new FoldingCirclesDrawable.Builder(this).
                colors(getProgressDrawableColors()).build());
    }

    private void setGraphView() {
        setContentView(R.layout.activity_graph);

        mChart = (LineChart) findViewById(R.id.linechart);
        mChart.setDescription("Sample Number (X 2.4 = Hertz)");
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

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        try {
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(mDeviceName);
        } catch (NullPointerException e) {
            Log.w(TAG, "NullPointerException when trying to getActionBar().");
        }

        // Bind BluetoothLeService to this activity
        Log.d(TAG, "binding");
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        setProgressBar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_graph, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_quit) {
            finish();
            return true;
        }
        if (id == R.id.action_refresh) {
            startCollectingData();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, DeviceControlActivity.makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result = " + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private void startCollectingData() {
        if (mWriteCharacteristic == null || mReadCharacteristic == null) {
            Log.w(TAG, "Bluetooth Service is not fully setup.");
            finish();
        }

        setProgressBar();

        // Prepare data array
        if (mEntries == null) {
            mEntries = new ArrayList<>();
        }
        mExpectedPackageNumber = 0;
        mEntries.clear();

        // Request data
        mWriteCharacteristic.setValue(new byte[]{WRITE_VALUE});
        mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
    }

    private void setupBluetooth() {
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

            if (gattService.getUuid().toString().equalsIgnoreCase(DEFAULT_SERVICE_UUID)) {
                Log.i(TAG, "GATT Service found");
                // Look for characteristics to read and write
                for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
                    final String uuid = characteristic.getUuid().toString();
                    if (uuid.equalsIgnoreCase(DEFAULT_READ_CHARACTERISTIC_UUID) && Util.isCharacteristicReadable(characteristic)) {
                        Log.d(TAG, "Setup characteristic read " + uuid.substring(4, 8));
                        mReadCharacteristic = characteristic;
                        // If there is an active notification on a characteristic, clear
                        // it first so it doesn't update the data field on the user interface.
                        if (mNotifyCharacteristic != null) {
                            mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
                            mNotifyCharacteristic = null;
                        }
                    }
                    if (uuid.equalsIgnoreCase((DEFAULT_WRITE_CHARACTERISTIC_UUID)) && Util.isCharacteristicWritable(characteristic)) {
                        Log.d(TAG, "Setup characteristic write" + uuid.substring(4, 8));
                        mWriteCharacteristic = characteristic;
                    }
                    if (uuid.equalsIgnoreCase(DEFAULT_NOTIFY_CHARACTERISTIC_UUID) && Util.isCharacteristicNotifiable(characteristic)) {
                        Log.d(TAG, "Setup characteristic notify" + uuid.substring(4, 8));
                        mNotifyCharacteristic = characteristic;
                        mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, true);
                    }
                }

                startCollectingData();
                break;
            }
        }
    }

    private void draw() {
        // Label X-axis
        xVals = new ArrayList<String>();
        for (int i = 0; i < mEntries.size(); i++) {
            xVals.add(i + "");
        }

        // create a dataset and give it a type
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


    // FFT: 5E
    // Sample: 5D
}

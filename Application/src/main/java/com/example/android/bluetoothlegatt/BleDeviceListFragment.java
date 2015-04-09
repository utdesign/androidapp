package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by quangta93 on 3/26/15.
 */
public class BleDeviceListFragment extends ListFragment {
    private static final String TAG = BleDeviceListFragment.class.getSimpleName();

    private static final long SCAN_PERIOD = 5000;
    private DeviceScanActivity mActivity;
    private BleDeviceListAdapter mListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler = new Handler();

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                /**
                 * @scanRecord the content of advertisement record offered by the remote device
                 */
                public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "LeScanCallback: new device = " + device.getName());
                            mListAdapter.addDevice(device);
                            mListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    public BleDeviceListFragment() {
        mListAdapter = new BleDeviceListAdapter();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (DeviceScanActivity) getActivity();
        mBluetoothAdapter = mActivity.getBluetoothAdapter();

        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "Button pressed for " + TAG);
        switch (item.getItemId()) {
            case R.id.menu_scan: {
                bleScan();
                return true;
            }
            case R.id.menu_stop: {
                stopScan();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Scan for Ble devices.
     */
    public void bleScan() {
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = mActivity.getBluetoothAdapter();
        }
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "Cannot get bluetooth adapter.");
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, DeviceScanActivity.REQUEST_ENABLE_BT);
        }

        Log.d(TAG, "Start LE Scan");
        mActivity.updateOptionsMenu(true);
        if (mListAdapter == null) {
            mListAdapter = new BleDeviceListAdapter();
        }
        mListAdapter.clear();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Stops scanning after a pre-defined scan period.
                mActivity.updateOptionsMenu(false);
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }, SCAN_PERIOD);
        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    public void stopScan() {
        Log.d(TAG, "Stop LE Scan");
        mActivity.updateOptionsMenu(false);
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chooses not to enable Bluetooth.
        if (requestCode == DeviceScanActivity.REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            mListAdapter.clear();
            setListAdapter(mListAdapter);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        BluetoothDevice device = mListAdapter.getDevice(position);
        if (device == null) return;

        final Intent intent = new Intent(mActivity, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        intent.putExtra(DeviceControlActivity.EXTRAS_CONNECTION_METHOD, DeviceControlActivity.BLUETOOTH_METHOD);
        stopScan();
        startActivity(intent);
    }

    /**
     * Simple {@link android.widget.BaseAdapter} to support displaying names and addresses of scanned
     * BLE devices.
     */
    private class BleDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflater;

        public BleDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            if (getActivity() != null) {
                mInflater = mActivity.getLayoutInflater();
            }
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
                notifyDataSetChanged();
                Log.d(TAG, "new item added. notify data set.");
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflater.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}

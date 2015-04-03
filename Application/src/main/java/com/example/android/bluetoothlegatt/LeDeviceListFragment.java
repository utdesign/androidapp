package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
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
public class LeDeviceListFragment extends ListFragment implements DeviceScanActivity.BleDeviceListFragmentCallback {
    private static final String TAG = LeDeviceListFragment.class.getSimpleName();

    private DeviceScanActivity mActivity;
    private LeDeviceListAdapter mListAdapter;
    private BluetoothAdapter mBluetoothAdapter;

    public LeDeviceListFragment() {
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        BluetoothDevice device = mListAdapter.getDevice(position);
        if (device == null) return;

        final Intent intent = new Intent(getActivity(), DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        intent.putExtra(DeviceControlActivity.EXTRAS_CONNECTION_METHOD, DeviceControlActivity.BLUETOOTH_METHOD);

        mActivity.scanLeDevice(false);
        startActivity(intent);
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
        if (mActivity.getConnectionMethod().equalsIgnoreCase(DeviceControlActivity.BLUETOOTH_METHOD)) {
            switch (item.getItemId()) {
                case R.id.menu_scan:
                case R.id.menu_refresh: {
                    scan();
                    break;
                }
                case R.id.menu_stop: {
                    mActivity.scanLeDevice(false);
                    mListAdapter.clear();
                    break;
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void scan() {
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = mActivity.getBluetoothAdapter();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, DeviceScanActivity.REQUEST_ENABLE_BT);
        }
        mListAdapter = new LeDeviceListAdapter();
        setListAdapter(mListAdapter);
        mActivity.scanLeDevice(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        scan();
    }

    @Override
    public void onPause() {
        super.onPause();

        mActivity.scanLeDevice(false);
        mListAdapter.clear();
    }

    @Override
    public void onBleScanResult(BluetoothDevice device) {
        mListAdapter.addDevice(device);
        mListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == DeviceScanActivity.REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            mListAdapter.clear();
            setListAdapter(mListAdapter);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Simple {@link android.widget.BaseAdapter} to support displaying names and addresses of scanned
     * BLE devices.
     */
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflater;

        public LeDeviceListAdapter() {
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

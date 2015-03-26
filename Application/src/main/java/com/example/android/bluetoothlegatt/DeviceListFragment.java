package com.example.android.bluetoothlegatt;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * A simple {@link android.app.Fragment} contains list of scanned devices.
 * <p/>
 * Created by quangta93 on 3/24/15.
 */
public class DeviceListFragment extends Fragment
        implements DeviceScanActivity.BleDeviceListFragmentCallback, AdapterView.OnItemClickListener {
    private static final String TAG = DeviceListFragment.class.getSimpleName();

    private String mConnectionMethod;
    private ListView mListView;
    private DeviceListAdapter mListAdapter;
    private BluetoothAdapter mBluetoothAdapter;

    public DeviceListFragment() {
    }

    /**
     * Static method as a standard way to create and insert additional information to {@link android.support.v4.app.Fragment}.
     *
     * @param connectionMethod connection method of this current tab.
     * @return An instance of {@link com.example.android.bluetoothlegatt.DeviceListFragment}
     */
    public static DeviceListFragment newInstance(String connectionMethod) {
        DeviceListFragment fragment = new DeviceListFragment();
        Bundle args = new Bundle();
        args.putString(DeviceControlActivity.EXTRAS_CONNECTION_METHOD, connectionMethod);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.i(TAG, "item clicked: " + position);
        String deviceName = null;
        String deviceAddress = null;
        if (mConnectionMethod.equalsIgnoreCase(DeviceControlActivity.BLUETOOTH_METHOD)) {
            BluetoothDevice device = mListAdapter.getBleDevice(position);
            if (device == null) return;
            Log.i(TAG, "device name = " + device.getName() + "; address = " + device.getAddress());
        } else if (mConnectionMethod.equalsIgnoreCase(DeviceControlActivity.WIFI_METHOD)) {
            WifiDevice device = mListAdapter.getWifiDevice(position);
            if (device == null) return;
            Log.i(TAG, "device name = " + device.getName() + "; address = " + device.getAddress());
        }

        final Intent intent = new Intent(getActivity(), DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, deviceName);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, deviceAddress);
        intent.putExtra(DeviceControlActivity.EXTRAS_CONNECTION_METHOD, mConnectionMethod);


        // Stop scanning
        if (((DeviceScanActivity) getActivity()).isScanning()) {
            mBluetoothAdapter.stopLeScan(((DeviceScanActivity) getActivity()).getLeScanCallback());
            ((DeviceScanActivity) getActivity()).stopLeScan();
        }
        startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments().containsKey(DeviceControlActivity.EXTRAS_CONNECTION_METHOD)) {
            mConnectionMethod = getArguments().getString(DeviceControlActivity.EXTRAS_CONNECTION_METHOD);
        }
        if (mConnectionMethod == null) {
            Toast.makeText(getActivity(), "Cannot identify tab.", Toast.LENGTH_LONG).show();
            getActivity().finish();
        } else {
            if (mConnectionMethod.equalsIgnoreCase(DeviceControlActivity.BLUETOOTH_METHOD)) {
                mBluetoothAdapter = ((DeviceScanActivity) getActivity()).getBluetoothAdapter();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_list, container, false);
        mListView = (ListView) view.findViewById(R.id.listview_devices);
        mListView.setOnItemClickListener(this);
        mListAdapter = new DeviceListAdapter();
        mListView.setAdapter(mListAdapter);

        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
            case R.id.menu_refresh: {
                mListAdapter.clear();

                // Start scanning
                if (mConnectionMethod.equalsIgnoreCase(DeviceControlActivity.BLUETOOTH_METHOD)) {
                    ((DeviceScanActivity) getActivity()).scanLeDevice(true);
                } else if (mConnectionMethod.equalsIgnoreCase(DeviceControlActivity.WIFI_METHOD)) {
                    // scanWifiDevice(true);
                }
                break;
            }
            case R.id.menu_stop: {
                // Stop scanning
                ((DeviceScanActivity) getActivity()).scanLeDevice(false);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (mConnectionMethod.equalsIgnoreCase(DeviceControlActivity.BLUETOOTH_METHOD)) {
            if (mBluetoothAdapter == null) {
                mBluetoothAdapter = ((DeviceScanActivity) getActivity()).getBluetoothAdapter();
            }
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, DeviceScanActivity.REQUEST_ENABLE_BT);
            }

            // Initializes list view adapter.
            ((DeviceScanActivity) getActivity()).scanLeDevice(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mConnectionMethod.equalsIgnoreCase(DeviceControlActivity.BLUETOOTH_METHOD)) {
            ((DeviceScanActivity) getActivity()).scanLeDevice(false);
        }
        mListAdapter.clear();
    }

    @Override
    public void onBleScanResult(BluetoothDevice device) {
        mListAdapter.addDevice(device);
        mListAdapter.notifyDataSetChanged();
    }

    public void onWifiScanResult(WifiDevice device) {
        mListAdapter.addDevice((device));
        mListAdapter.notifyDataSetChanged();
    }

    /**
     * Simple {@link android.widget.BaseAdapter} to support displaying names and addresses of scanned
     * devices, including both {@link android.bluetooth.BluetoothDevice} and {@code WifiDevice}
     * to {@link android.widget.ListView}.
     */
    private class DeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private ArrayList<WifiDevice> mWifiDevices;
        private LayoutInflater mInflater;

        public DeviceListAdapter() {
            super();
            if (mConnectionMethod.equalsIgnoreCase(DeviceControlActivity.BLUETOOTH_METHOD)) {
                mLeDevices = new ArrayList<BluetoothDevice>();
            } else if (mConnectionMethod.equalsIgnoreCase(DeviceControlActivity.WIFI_METHOD)) {
                mWifiDevices = new ArrayList<WifiDevice>();
            }
            if (getActivity() != null) {
                mInflater = getActivity().getLayoutInflater();
            }
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                Log.i(TAG, "add Ble device: " + device.getName());
                mLeDevices.add(device);
            }
        }

        public void addDevice(WifiDevice device) {
            if (!mWifiDevices.contains(device)) {
                Log.i(TAG, "add Wifi device: " + device.getName());
                mWifiDevices.add(device);
            }
        }

        public BluetoothDevice getBleDevice(int position) {
            return mLeDevices.get(position);
        }

        public WifiDevice getWifiDevice(int position) {
            return mWifiDevices.get(position);
        }

        public void clear() {
            if (mLeDevices != null) {
                mLeDevices.clear();
            }
            if (mWifiDevices != null) {
                mWifiDevices.clear();
            }
        }

        @Override
        public int getCount() {
            if (mLeDevices != null) {
                return mLeDevices.size();
            }
            if (mWifiDevices != null) {
                return mWifiDevices.size();
            }
            return -1;
        }

        @Override
        public Object getItem(int i) {
            if (mLeDevices != null) {
                return mLeDevices.get(i);
            }
            if (mWifiDevices != null) {
                return mWifiDevices.get(i);
            }
            return null;
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

            String deviceName = null;
            String deviceAddress = null;
            if (mConnectionMethod.equalsIgnoreCase(DeviceControlActivity.BLUETOOTH_METHOD)) {
                BluetoothDevice device = mLeDevices.get(i);
                deviceName = device.getName();
                deviceAddress = device.getAddress();
            } else if (mConnectionMethod.equalsIgnoreCase(DeviceControlActivity.WIFI_METHOD)) {
                WifiDevice device = mWifiDevices.get(i);
                deviceName = device.getName();
                deviceAddress = device.getAddress();
            }
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(deviceAddress);
            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}

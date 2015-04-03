package com.example.android.bluetoothlegatt;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
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
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by quangta93 on 3/26/15.
 */
public class WifiDeviceListFragment extends ListFragment implements DeviceScanActivity.WifiScanCallback {
    private static final String TAG = WifiDeviceListFragment.class.getSimpleName();
    private static final String JSON_DEVICE_LIST = "devices";
    private static final String JSON_DEVICE_NAME = "name";
    private static final String JSON_DEVICE_ADDRESS = "address";

    private DeviceScanActivity mActivity;
    private WifiDeviceListAdapter mListAdapter;
    private RequestQueue mRequestQueue;

    public WifiDeviceListFragment() {
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        WifiDevice device = mListAdapter.getDevice(position);
        if (device == null) return;

        final Intent intent = new Intent(mActivity, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        intent.putExtra(DeviceControlActivity.EXTRAS_CONNECTION_METHOD, DeviceControlActivity.WIFI_METHOD);
        mActivity.scanLeDevice(false);
        startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (DeviceScanActivity) getActivity();
        mRequestQueue = Volley.newRequestQueue(mActivity);

        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "Button pressed for " + TAG);
        if (mActivity.getConnectionMethod().equalsIgnoreCase(DeviceControlActivity.WIFI_METHOD)) {
            switch (item.getItemId()) {
                case R.id.menu_scan:
                case R.id.menu_refresh: {
                    onScan();
                    break;
                }
                case R.id.menu_stop: {
                    stopScan();
                    break;
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        onScan();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopScan();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopScan();
    }

    private void stopScan() {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(TAG);
        }
        mActivity.scanWifiDevice(false);
        if (mListAdapter != null) mListAdapter.clear();
    }

    private void sendScanRequest() {
        Log.d(TAG, "Start Wifi Scan");
        // Instantiate the RequestQueue.
        String url = (Constant.SERVER_URL.length() > 0) ? Constant.SERVER_URL : Constant.SERVER_IP_ADDRESS;

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (mActivity == null || response == null || response.length() == 0) return;
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            // Parsing the response.
                            mListAdapter = new WifiDeviceListAdapter();
                            WifiDeviceListFragment.this.setListAdapter(mListAdapter);
                            JSONArray deviceList = jsonObject.getJSONArray(JSON_DEVICE_LIST);
                            if (deviceList == null) {
                                Toast.makeText(mActivity, "Error: No device list in response.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            for (int i = 0; i < deviceList.length(); i++) {
                                JSONObject device = (JSONObject) deviceList.get(i);
                                String deviceName = device.getString(JSON_DEVICE_NAME);
                                String deviceAddress = device.getString(JSON_DEVICE_ADDRESS);
                                if ((deviceName != null && device.length() > 0)
                                        && (deviceAddress != null && deviceAddress.length() > 0)) {
                                    mListAdapter.addDevice(new WifiDevice(deviceName, deviceAddress));
                                    mListAdapter.notifyDataSetChanged();
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Problem(s) parsing the response.");
                        }
                        mActivity.scanWifiDevice(false);
                        Log.d(TAG, "Stop Wifi Scan");
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.w(TAG, error.getMessage());
                mActivity.scanWifiDevice(false);
            }
        });
        // Add the request to the RequestQueue.
        stringRequest.setTag(TAG);
        mRequestQueue.add(stringRequest);
    }

    @Override
    public void onScan() {
        if (!Util.isNetworkConnected(mActivity)) {
            new MaterialDialog.Builder(mActivity)
                    .title(R.string.dialog_title)
                    .content(R.string.dialog_content)
                    .positiveText(R.string.enable)
                    .negativeText(R.string.cancel)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            // Enabling Wifi
                            WifiManager wifiManager = (WifiManager) mActivity.getSystemService(Context.WIFI_SERVICE);
                            wifiManager.setWifiEnabled(true);

                            // Initializes list view adapter.
                            mListAdapter = new WifiDeviceListAdapter();
                            setListAdapter(mListAdapter);
                            mActivity.scanWifiDevice(true);
                            Log.d(TAG, "sending request");
                            // sendScanRequest();
                            dialog.dismiss();
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            dialog.dismiss();
                        }
                    }).autoDismiss(false).show();
        } else {
            // Initializes list view adapter.
            Log.d(TAG, "sending request");
            mListAdapter = new WifiDeviceListAdapter();
            setListAdapter(mListAdapter);
            mActivity.scanWifiDevice(true);
            // sendScanRequest();
        }
    }

    /**
     * Simple {@link android.widget.BaseAdapter} to support displaying names and addresses of scanned
     * Wifi devices.
     */
    private class WifiDeviceListAdapter extends BaseAdapter {
        private ArrayList<WifiDevice> mLeDevices;
        private LayoutInflater mInflater;

        public WifiDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<WifiDevice>();
            if (getActivity() != null) {
                mInflater = mActivity.getLayoutInflater();
            }
        }

        public void addDevice(WifiDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
                notifyDataSetChanged();
            }
        }

        public WifiDevice getDevice(int position) {
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

            WifiDevice device = mLeDevices.get(i);
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

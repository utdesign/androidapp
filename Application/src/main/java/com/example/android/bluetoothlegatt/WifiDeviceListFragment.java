package com.example.android.bluetoothlegatt;

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
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;

/**
 * Created by quangta93 on 3/26/15.
 */
public class WifiDeviceListFragment extends ListFragment {
    private static final String TAG = WifiDeviceListFragment.class.getSimpleName();
    private static final String JSON_DEVICE_LIST = "devices";
    private static final String JSON_DEVICE_NAME = "name";
    private static final String JSON_DEVICE_ADDRESS = "address";

    // Retrofit API
    private static final String RETROFIT_API_ENDPOINT = "http://169.54.208.180:3000/utdesign";

    private DeviceScanActivity mActivity;
    private WifiDeviceListAdapter mListAdapter;
    private RestAdapter mRestAdapter = new RestAdapter.Builder()
            .setEndpoint(RETROFIT_API_ENDPOINT)
            .setLogLevel(RestAdapter.LogLevel.FULL)
            .build();
    private RetrofitScanApi mClient = mRestAdapter.create(RetrofitScanApi.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "on create");
        mActivity = (DeviceScanActivity) getActivity();
        mListAdapter = new WifiDeviceListAdapter();
        setListAdapter(mListAdapter);

        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan: {
                wifiScan();
                return true;
            }
            case R.id.menu_stop: {
                stopScan();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, " on resume");
        if (mActivity.getCurrentTab() == DeviceScanPagerAdapter.WIFI_TAB) {
            Log.d(TAG, "wifiScan");
            // wifiScan();
        }
    }

    /**
     * Start Wifi Scan by verifying Internet connection is enabled.
     */
    public void wifiScan() {
        // Initializes list view adapter.
        Log.d(TAG, "sending request");
        if (mListAdapter == null) {
            mListAdapter = new WifiDeviceListAdapter();
        }
        mListAdapter.clear();
        setListAdapter(mListAdapter);
        mActivity.updateOptionsMenu(true);
        // sendScanRequest();
    }

    /**
     * Send scan request to web server using Volley library.
     * Define the response handler.
     */

    private void sendScanRequest() {
        Log.d(TAG, "Start Wifi Scan");

        // Instantiate the RequestQueue.
        String url = (Constant.SERVER_URL.length() > 0) ? Constant.SERVER_URL : Constant.SERVER_IP_ADDRESS;

        // Request a string response from the provided URL.
        mClient.scanForDevices(new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                if (mActivity == null) {
                    return;
                }
                try {
                    JSONObject jsonResponse = getJsonResponse(response);
                    if (jsonResponse == null || jsonResponse.length() == 0) {
                        Toast.makeText(mActivity, "No Scan Response.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    // Parsing the response.
                    if (mListAdapter == null) {
                        mListAdapter = new WifiDeviceListAdapter();
                    }
                    mListAdapter.clear();
                    WifiDeviceListFragment.this.setListAdapter(mListAdapter);
                    JSONArray deviceList = jsonResponse.getJSONArray(JSON_DEVICE_LIST);
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
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
                mActivity.updateOptionsMenu(false);
                Log.d(TAG, "Stop Wifi Scan");
            }

            @Override
            public void failure(RetrofitError error) {
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_LONG).show();
                mActivity.updateOptionsMenu(false);
            }
        });
    }

    /**
     * Stop all scan requests.
     */
    public void stopScan() {
        mActivity.updateOptionsMenu(false);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        WifiDevice device = mListAdapter.getDevice(position);
        if (device == null) return;

        final Intent intent = new Intent(mActivity, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        intent.putExtra(DeviceControlActivity.EXTRAS_CONNECTION_METHOD, DeviceControlActivity.WIFI_METHOD);
        stopScan();
        startActivity(intent);
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
                Log.d(TAG, "new item added. notify data set.");
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

    private JSONObject getJsonResponse(Response response) {
        try {
            String strResponse = getStringResponse(response);
            if (strResponse == null) return null;
            return new JSONObject(strResponse);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            return null;
        }
    }

    private String getStringResponse(Response response) {
        try {
            byte[] buff = new byte[2048];
            response.getBody().in().read(buff);
            return new String(buff).trim();
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            return null;
        }
    }

    public interface RetrofitScanApi {

        @GET("/")
        public void scanForDevices(Callback<Object> callback);
    }

}

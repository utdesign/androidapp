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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends FragmentActivity implements ViewPager.OnPageChangeListener{
    // Log tag
    private static final String TAG = DeviceScanActivity.class.getSimpleName();
    private static final int INDICATOR_COLOR = R.color.orange;
    public static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;

    private boolean mScanning;
    private Handler mHandler;
    private String mConnectionMethod;
    private BluetoothAdapter mBluetoothAdapter;
    private DeviceScanPagerAdapter mPagerAdapter;
    private BleDeviceListFragmentCallback mBleCallback;

    private ViewPager mViewPager;
    private PagerSlidingTabStrip mTabs;

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mConnectionMethod = DeviceScanPagerAdapter.TITLES[position];
    }

    @Override
    public void onPageSelected(int position) {
        mConnectionMethod = DeviceScanPagerAdapter.TITLES[position];
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    /**
     * A callback interface that all fragments containing this fragment must implement.
     * This mechanism allows fragments to be notified of {@link android.bluetooth.BluetoothAdapter}
     * scan results.
     */
    public interface BleDeviceListFragmentCallback {

        public void onBleScanResult(BluetoothDevice device);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);
        setTitle(R.string.title_devices);
        setTheme(R.style.CardView_Light);

        mTabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        mTabs.setIndicatorColorResource(INDICATOR_COLOR);
        mTabs.setTextColor(getResources().getColor(R.color.black));
        mTabs.setShouldExpand(true);
        mTabs.setOnPageChangeListener(this);

        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mPagerAdapter = new DeviceScanPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources()
                .getDisplayMetrics());
        mViewPager.setPageMargin(pageMargin);
        mTabs.setViewPager(mViewPager);

        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!BluetoothLeUtility.hasBleSupport(this)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            return;
        } else {
            Log.i(TAG, "BLE supported.");
        }

        mBluetoothAdapter = BluetoothLeUtility.getBluetoothAdapter(this);
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            return;
        } else {
            Log.i(TAG, "Bluetooth supported");
        }
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public boolean isScanning() {
        return mScanning;
    }

    public void stopLeScan() {
        mScanning = false;
    }

    public void scanLeDevice (boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    public BluetoothAdapter.LeScanCallback getLeScanCallback() {
        return mLeScanCallback;
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                /**
                 * @scanRecord the content of advertisement record offered by the remote device
                 */
                public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mBleCallback = (BleDeviceListFragmentCallback) mPagerAdapter.getCurrentFragment(0);
                            if (mBleCallback != null) mBleCallback.onBleScanResult(device);
                        }
                    });
                }
            };
}
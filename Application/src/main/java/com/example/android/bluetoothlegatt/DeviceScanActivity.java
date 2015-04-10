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

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
public class DeviceScanActivity extends FragmentActivity implements ViewPager.OnPageChangeListener {
    private static final String TAG = DeviceScanActivity.class.getSimpleName();
    private static final int INDICATOR_COLOR = R.color.orange;
    public static final int REQUEST_ENABLE_BT = 1;

    private boolean mScanning;
    private BluetoothAdapter mBluetoothAdapter;

    private DeviceScanPagerAdapter mPagerAdapter;
    private ViewPager mViewPager;
    private PagerSlidingTabStrip mTabs;
    private int mCurrentTab;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);
        setTitle(R.string.title_devices);

        mTabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        mTabs.setIndicatorColorResource(INDICATOR_COLOR);
        mTabs.setTextColor(getResources().getColor(R.color.black));
        mTabs.setShouldExpand(true);
        mTabs.setOnPageChangeListener(this);

        // initialize view pager and its adapter.
        Log.d(TAG, "on create");
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mPagerAdapter = new DeviceScanPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        mViewPager.setPageMargin(pageMargin);
        mTabs.setViewPager(mViewPager);
        mCurrentTab = DeviceScanPagerAdapter.BLUETOOTH_TAB;
        mViewPager.setCurrentItem(mCurrentTab);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!Util.hasBleSupport(this)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        mBluetoothAdapter = Util.getBluetoothAdapter(this);
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scan, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    public void updateOptionsMenu(boolean enable) {
        mScanning = enable;
        invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCurrentTab == 0) {
            ((BleDeviceListFragment) mPagerAdapter.getCurrentItem(mCurrentTab)).stopScan();
        } else if (mCurrentTab == 1) {
            ((WifiDeviceListFragment) mPagerAdapter.getCurrentItem(mCurrentTab)).stopScan();
        }
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (state == ViewPager.SCROLL_STATE_IDLE) {
            if (mCurrentTab != mViewPager.getCurrentItem()) {
                // update current tab number
                mCurrentTab = mViewPager.getCurrentItem();
                onTabChanged();
            }
        }
    }

    private void onTabChanged() {
        Log.d(TAG, "tab changed.");
        if (mCurrentTab == 0) {
            // Stop Wifi scan
            ((WifiDeviceListFragment) mPagerAdapter.getCurrentItem(DeviceScanPagerAdapter.WIFI_TAB)).stopScan();
            // Start Ble scan
            ((BleDeviceListFragment) mPagerAdapter.getCurrentItem(DeviceScanPagerAdapter.BLUETOOTH_TAB)).bleScan();
        } else if (mCurrentTab == 1) {
            // Stop Ble scan
            ((BleDeviceListFragment) mPagerAdapter.getCurrentItem(DeviceScanPagerAdapter.BLUETOOTH_TAB)).stopScan();
            // Start Wifi scan
            ((WifiDeviceListFragment) mPagerAdapter.getCurrentItem(DeviceScanPagerAdapter.WIFI_TAB)).wifiScan();
        }
    }

    public int getCurrentTab() {
        return mCurrentTab;
    }
}
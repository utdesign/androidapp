package com.example.android.bluetoothlegatt;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

import java.util.ArrayList;

/**
 * A {@link android.support.v4.view.PagerAdapter} that serves {@code Fragment}
 * Created by quangta93 on 3/23/15.
 */
public class DeviceScanPagerAdapter extends FragmentPagerAdapter {
    public static final String TAG = DeviceScanPagerAdapter.class.getSimpleName();
    public static final String[] TITLES = new String[]{DeviceControlActivity.BLUETOOTH_METHOD, DeviceControlActivity.WIFI_METHOD};
    private DeviceListFragment[] mFragments;

    public DeviceScanPagerAdapter(FragmentManager fm) {
        super(fm);
        mFragments = new DeviceListFragment[TITLES.length];
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return TITLES[position];
    }

    @Override
    public Fragment getItem(int position) {
        mFragments[position] = DeviceListFragment.newInstance(TITLES[position]);
        return mFragments[position];
    }

    public Fragment getCurrentFragment(int position) {
        return mFragments[position];
    }

    @Override
    public int getCount() {
        return TITLES.length;
    }
}

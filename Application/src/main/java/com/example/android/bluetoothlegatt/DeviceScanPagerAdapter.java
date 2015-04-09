package com.example.android.bluetoothlegatt;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

/**
 * A {@link android.support.v4.view.PagerAdapter} that serves {@code Fragment}
 * Created by quangta93 on 3/23/15.
 */
public class DeviceScanPagerAdapter extends FragmentPagerAdapter {
    public static final String TAG = DeviceScanPagerAdapter.class.getSimpleName();
    public static final String[] TITLES = new String[]{DeviceControlActivity.BLUETOOTH_METHOD, DeviceControlActivity.WIFI_METHOD};
    public static final int BLUETOOTH_TAB = 0;
    public static final int WIFI_TAB = 1;
    private Fragment[] mFragments;

    public DeviceScanPagerAdapter(FragmentManager fm) {
        super(fm);
        mFragments = new Fragment[TITLES.length];
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return TITLES[position];
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            mFragments[0] = new BleDeviceListFragment();
        } else if (position == 1) {
            mFragments[1] = new WifiDeviceListFragment();
        }
        return mFragments[position];
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        mFragments[position] = fragment;
        return fragment;
    }

    public Fragment getCurrentItem(int position) {
        return mFragments[position];
    }

    @Override
    public int getCount() {
        return TITLES.length;
    }
}

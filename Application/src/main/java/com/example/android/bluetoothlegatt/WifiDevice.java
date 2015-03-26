package com.example.android.bluetoothlegatt;

/**
 * Created by quangta93 on 3/20/15.
 */
public class WifiDevice {

    private String mDeviceName;
    private String mDeviceMacAddress;

    public WifiDevice() {}

    public WifiDevice(String name, String macAddress) {
        mDeviceName = name;
        mDeviceMacAddress = macAddress;
    }

    public String getName() {
        return mDeviceName;
    }

    public String getAddress() {
        return mDeviceMacAddress;
    }
}

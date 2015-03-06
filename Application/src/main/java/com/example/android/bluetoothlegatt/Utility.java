package com.example.android.bluetoothlegatt;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by quangta93 on 2/21/15.
 */
public class Utility {

    /* Parse API */
    public static final String PARSE_APPLICATION_ID = "D11CTyj7ZpZpZmtTs04GXIyNb9a8IPhvJRI9Z1rW";
    public static final String PARSE_CLIENT_KEY = "GaT917Zh7uGdaLjilazCgnxAwLdkxV4zcDACADIN";


    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    // Convert from byte array to hex string
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}
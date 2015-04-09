package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.ProgressBar;

import com.jpardogo.android.googleprogressbar.library.FoldingCirclesDrawable;

/**
 * Created by quang.tran on 3/11/2015.
 */
public class Util {

    /**
     * @return Returns <b>true</b> if property is writable
     */
    public static boolean isCharacteristicWritable(BluetoothGattCharacteristic characteristic) {
        return ((characteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0);
    }

    /**
     * @return Returns <b>true</b> if property is Readable
     */
    public static boolean isCharacteristicReadable(BluetoothGattCharacteristic characteristic) {
        return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
    }

    /**
     * @return Returns <b>true</b> if property is supports notification
     */
    public static boolean isCharacteristicNotifiable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    // Convert from byte array to hex string
    public static String bytesToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static boolean hasBleSupport(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    // Checks if Bluetooth is supported on the device.
    public static BluetoothAdapter getBluetoothAdapter(Context context) {
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        return bluetoothManager.getAdapter();
    }

    /**
     * Returns <p>true</p> if the device is connected to the Internet.
     * @param context
     * @return
     */
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connectivityManager.getActiveNetworkInfo();
        return (activeInfo != null && activeInfo.isConnected());
    }


    /**
     * Inflates the Folding Circle {@link android.widget.ProgressBar} while connecting to device.
     */
    public static ProgressBar setProgressBar(Activity activity) {
        ProgressBar mProgressBar;
        activity.setContentView(R.layout.google_progress_bar);
        mProgressBar = (ProgressBar) activity.findViewById(R.id.google_progress);
        mProgressBar.setIndeterminateDrawable(new FoldingCirclesDrawable.Builder(activity).
                colors(Util.getProgressDrawableColors(activity)).build());
        return mProgressBar;
    }

    /**
     * Returns an array of colors that is displaying in the Folding Circle {@link android.widget.ProgressBar} .
     *
     * @return Color array.
     */
    private static int[] getProgressDrawableColors(Context context) {
        int[] colors = new int[4];
        colors[0] = context.getResources().getColor(R.color.red);
        colors[1] = context.getResources().getColor(R.color.blue);
        colors[2] = context.getResources().getColor(R.color.yellow);
        colors[3] = context.getResources().getColor(R.color.green);
        return colors;
    }
}

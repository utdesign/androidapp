package com.example.android.bluetoothlegatt;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Fragment class to handle user input instructions
 * @author quangta93
 */
public class DeviceControlFragment extends Fragment {

    private static final String TAG = DeviceControlFragment.class.getSimpleName();
    private BluetoothGattCharacteristic characteristic;

    public DeviceControlFragment() {
        // Required empty public constructor
    }

    @SuppressLint("ValidFragment")
    public DeviceControlFragment(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_control, container, false);
    }

}

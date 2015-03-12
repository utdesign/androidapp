package com.example.android.bluetoothlegatt;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import roboguice.inject.InjectView;


/**
 * A {@code Fragment} subclass to monitor a connected device.
 */
public class ControlFragment extends Fragment {

    private static final String TAG = ControlFragment.class.getSimpleName();

    public static final int NUMBER_OF_REPEATED_WRITE = 20;

    // List of available instructions
    public static final String GET_INSTRUCTION = "get ";
    public static final String PUT_INSTRUCTION = "put ";
    public static final String SLEEP_INSTRUCTION = "sleep ";

    private BluetoothLeService mBluetoothLeService;

    @InjectView(R.id.tv_response)       private TextView mResponse;
    @InjectView(R.id.tv_last_command)   private TextView mLastInstruction;
    @InjectView(R.id.btn_read)          private Button mReadButton;
    @InjectView(R.id.btn_get)           private Button mGetButton;
    @InjectView(R.id.btn_put)           private Button mPutButton;
    @InjectView(R.id.btn_sleep)         private Button mSleepButton;
    @InjectView(R.id.checkbox_graph)    private CheckBox mGraphCheckBox;
    @InjectView(R.id.et_get_pin)        private EditText mGetPinEditText;
    @InjectView(R.id.et_put_pin)        private EditText mPutPinEditText;
    @InjectView(R.id.et_put_value)      private EditText mPutValEditText;
    @InjectView(R.id.et_sleep_ms)       private EditText mSleepDurationEditText;

    private boolean isGraphChecked = false;
    private int mPinNumber;
    private int mPinValue;
    private int mSleepDuration;

    public ControlFragment() {
    }

    @SuppressLint("ValidFragment")
    public ControlFragment(BluetoothLeService service) {
        this.mBluetoothLeService = service;
    }

    /**
     * Inflate <b>Layout</b> for {@code ControlFragment} and set <b>Listener</b> for actions.
     * @param inflater              layout inflater
     * @param container             Place to inject fragment's layout
     * @param savedInstanceState    store bundle
     * @return                      inflated view for fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_control, container, false);

        mReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetoothLeService == null) {
                    Log.w(TAG, "BluetoothLeService == null");
                    return;
                }
                mBluetoothLeService.readCharacteristic();
            }
        });
        mGetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPinNumber = Integer.parseInt(mGetPinEditText.getText().toString());
                String instruction = GET_INSTRUCTION + mPinNumber + "\n";
                if (isGraphChecked) {
                    // Request repeatedly and graph data
                    writeRepeat(instruction);
                } else {
                    // Request once
                    writeToDevice(instruction);
                }
            }
        });
        mPutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPinNumber = Integer.parseInt(mPutPinEditText.getText().toString());
                mPinValue = Integer.parseInt(mPutValEditText.getText().toString());
                writeToDevice(PUT_INSTRUCTION + mPinNumber + " " + mPinValue + "\n");
            }
        });
        mSleepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSleepDuration = Integer.parseInt(mSleepDurationEditText.getText().toString());
                writeToDevice(SLEEP_INSTRUCTION + mSleepDuration + "\n");
            }
        });
        mGraphCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isGraphChecked = isChecked;
            }
        });
        return rootView;
    }

    /**
     * Call {@code BluetoothLeService} to write selected command to connected device.
     *
     * @param instruction Requested command.
     */
    private void writeToDevice(String instruction) {
        if (mBluetoothLeService == null) {
            Log.w(TAG, "BluetoothLeService == null");
            return;
        }
        mLastInstruction.setText(instruction.trim());
        mBluetoothLeService.writeCharacteristic(instruction);
    }

    /**
     * Call {@code BluetoothLeService} to write selected command repeatedly to connected device;
     *
     * @param instruction
     */
    private void writeRepeat(String instruction) {
        if (mBluetoothLeService == null) {
            Log.w(TAG, "BluetoothLeService == null");
            return;
        }
        mLastInstruction.setText(instruction.trim());
        for (int counter = 0; counter < NUMBER_OF_REPEATED_WRITE; counter ++) {
            mBluetoothLeService.writeCharacteristic(instruction);
        }
    }
}

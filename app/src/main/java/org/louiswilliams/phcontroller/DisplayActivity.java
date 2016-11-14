package org.louiswilliams.phcontroller;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import org.louiswilliams.phcontroller.databinding.ActivityDisplayBinding;

import java.util.Queue;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;

public class DisplayActivity extends AppCompatActivity {

    private static final String TAG = DisplayActivity.class.getSimpleName();
    private ProgressDialog mProgressDialog;
    private BluetoothGatt mBluetoothGatt;
    private CarData carData = new CarData();
    private BluetoothGattService carService;
    private AsyncTask<Void, Void, Boolean> dataCollectionTask;
    private AsyncTask<Void, Void, Void> logTask;
    private Queue<BluetoothGattDescriptor> descriptorQueue;
    private boolean logging;

    public static final String PH517_UUID = "B34A1000-2303-47C5-83D5-868362DEEBA6";
    public static final String CCCD = "00002902-0000-1000-8000-00805F9B34FB";

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mProgressDialog.dismiss();
                Log.i(TAG, "Connected to GATT server");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                    if (mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)) {
                        Log.i(TAG, "Using high connection priority");
                    }
                }
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                Log.i(TAG, "Connecting to GATT server");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (mProgressDialog != null)
                    mProgressDialog.dismiss();
                Log.i(TAG, "Disconnected from GATT server");
                finish();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Discovered services: ");

                for (BluetoothGattService service : mBluetoothGatt.getServices()) {
                    Log.i(TAG, "  " + service.getUuid());
                    if (service.getUuid().toString().equalsIgnoreCase(PH517_UUID)) {
                        carService = service;
                    }
                }
                if (carService != null) {
                    Log.i(TAG, "Found Car Service");
                    if (dataCollectionTask != null) {
                        dataCollectionTask.cancel(true);
                    }
                    for (BluetoothGattCharacteristic characteristic : carService.getCharacteristics()) {
                        Log.i(TAG, "Characteristic: " + characteristic.getUuid());
                    }

                    /* Subscribe to notifications */
                    for (String name : carData.getUuids().keySet()) {
                        String uuid = carData.getUuids().get(name);
                        BluetoothGattCharacteristic characteristic = carService.getCharacteristic(UUID.fromString(uuid));
                        if (characteristic != null) {
                            mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CCCD));
                            if (descriptor == null) {
                                Log.w(TAG, "Client characteristic configuration descriptor for " + uuid + "is null");
                            } else {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                descriptorQueue.add(descriptor);
                            }
                        } else {
                            Log.w(TAG, "Characteristic " + uuid + " not found");
                        }
                    }

                    // Write the first descriptor
                    if (!mBluetoothGatt.writeDescriptor(descriptorQueue.remove())) {
                        Log.i(TAG, "Failed to initiate writing notification descriptor");
                    }

//                    dataCollectionTask = new AsyncTask<Void, Void, Boolean>() {
//                        @Override
//                        protected Boolean doInBackground(Void... params) {
//                            boolean success = true;
//                            while (success) {
//                                success =  collectData();
//                            }
//                            return success;
//                        }
//                    };
//                    dataCollectionTask.execute();
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(DisplayActivity.this, "Car service not found on device", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Log.i(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Integer value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                if (value != null) {
                    carData.setByUUID(characteristic.getUuid().toString(), value);
                }
//                Log.i(TAG, "Characteristic " + characteristic.getUuid() + ": " + value);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Integer value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
            if (value != null) {
                carData.setByUUID(characteristic.getUuid().toString(), value);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Failed to write to descriptor " +descriptor.getUuid().toString() + " of " + descriptor.getCharacteristic().getUuid().toString() + ": " + status);
            }

            /* Dequeue and write one */
            if (!descriptorQueue.isEmpty()) {
                if (!mBluetoothGatt.writeDescriptor(descriptorQueue.remove())) {
                    Log.i(TAG, "Failed to initiate writing notification descriptor");
                }
            }
        }


    };

    boolean collectData() {
        boolean success = true;
        if (carService != null ) {
            /* Collect all the characteristic data */
            for (String name : carData.getUuids().keySet()) {
                String uuid = carData.getUuids().get(name);
                BluetoothGattCharacteristic characteristic = carService.getCharacteristic(UUID.fromString(uuid));
                if (characteristic != null) {
                    mBluetoothGatt.readCharacteristic(characteristic);
                } else {
//                    Log.w(TAG, "Characteristic " + uuid + " not found");
                }
            }
        } else {
            Log.w(TAG, "Sanity check failed. Car service not found yet?");
            success =  false;
        }
        return success;
    }

    void logCarData() {


        while (logging) {

        }
        if (logging) {

        } else {
            // Close file
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityDisplayBinding binding = DataBindingUtil.setContentView(DisplayActivity.this, R.layout.activity_display);

        binding.setCarData(carData);

        Intent intent = getIntent();
        BluetoothDevice device = intent.getParcelableExtra("BTLE_DEVICE");
        descriptorQueue = new ConcurrentLinkedQueue<>();
        mBluetoothGatt = device.connectGatt(this, true, mGattCallback);

        mProgressDialog = ProgressDialog.show(DisplayActivity.this, "", "Connecting...", true, true, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mBluetoothGatt.disconnect();
                finish();
            }
        });

        final Button logButton = (Button) findViewById(R.id.log_button);
        logButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (logTask != null){
                    logTask.cancel(false);
                }
                if (logging) {
                    logging = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            logButton.setEnabled(true);
                        }
                    });
                } else {
                    logging = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            logButton.setEnabled(false);
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
        if (dataCollectionTask != null && !dataCollectionTask.isCancelled()) {
            dataCollectionTask.cancel(true);
        }
        super.onDestroy();
    }
}

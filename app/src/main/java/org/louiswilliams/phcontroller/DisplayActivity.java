package org.louiswilliams.phcontroller;

import android.Manifest;
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
import android.content.pm.PackageManager;
import android.databinding.BindingAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DisplayActivity extends FragmentActivity {

    private static final String TAG = DisplayActivity.class.getSimpleName();
    private ProgressDialog mProgressDialog;
    private BluetoothGatt mBluetoothGatt;
    private CarData carData;
    private BluetoothGattService carService;
    private ScheduledExecutorService logService;
    private Queue<BluetoothGattDescriptor> descriptorQueue;
    private boolean logging;

    private static final int PERMISSION_REQUEST_WRITE = 456;

    public static final String PH517_UUID = "B34A1000-2303-47C5-83D5-868362DEEBA6";
    public static final String CCCD = "00002902-0000-1000-8000-00805F9B34FB";

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mProgressDialog.dismiss();
                Log.i(TAG, "Connected to GATT server");
                setBtConnectedIndicator(true);
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
                setBtConnectedIndicator(false);
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
                Log.w(TAG, "Failed to write to descriptor " + descriptor.getUuid().toString() + " of " + descriptor.getCharacteristic().getUuid().toString() + ": " + status);
            }

            /* Dequeue and write one */
            if (!descriptorQueue.isEmpty()) {
                if (!mBluetoothGatt.writeDescriptor(descriptorQueue.remove())) {
                    Log.i(TAG, "Failed to initiate writing notification descriptor");
                }
            }
        }


    };

    private final View.OnClickListener onLogButtonListener =  new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (ContextCompat.checkSelfPermission(DisplayActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(DisplayActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Don't show rationale
                } else {
                    ActivityCompat.requestPermissions(DisplayActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSION_REQUEST_WRITE);
                }

            } else if (logging) {
                logStop();
            } else {
                logStart();
            }
        }
    };

    private  final View.OnClickListener onAuxButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AuxFragment auxFragment = new AuxFragment();

            getSupportFragmentManager().beginTransaction().replace(R.id.display_frame, auxFragment)
                    .addToBackStack(null).commit();
        }
    };

    public View.OnClickListener getOnLogButtonListener() {
        return onLogButtonListener;
    }

    public View.OnClickListener getOnAuxButtonListener() {
        return onAuxButtonListener  ;
    }

    void logCarData() {
        logService = Executors.newSingleThreadScheduledExecutor();
        final String logFileName = String.format("data-%d.csv", new Date().getTime());
        File dataDir = new File(Environment.getExternalStorageDirectory(), "ph571");
        dataDir.mkdir();
        final File logFile = new File(dataDir, logFileName);
        final FileOutputStream outputStream;

        try {
            outputStream = new FileOutputStream(logFile);
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Could not log file: " + e.getMessage());
            return;
        }

        // Write header columns
        String headers = carData.getColumns();
        try {
            outputStream.write(headers.getBytes());
            outputStream.write('\n');
        } catch (IOException e) {
            Log.w(TAG, "Error logging data: " + e.getMessage());
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DisplayActivity.this, "Logging to: " + logFileName, Toast.LENGTH_SHORT).show();
            }
        });
        // Log every 0.5 second
        logService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (logging) {
                    try {
                        outputStream.write(carData.getRow().getBytes());
                        outputStream.write('\n');
                    } catch (IOException e) {
                        Log.w(TAG, "Error logging data: " + e.getMessage());
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(DisplayActivity.this, "File saved as: " + logFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        }
                    });
                    try {
                        outputStream.close();
                        logService.shutdown();
                    } catch (IOException e) {
                        Log.w(TAG, "Error closing file: " + e.getMessage());
                    }
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    private void logStart() {
        final Button logButton = (Button) findViewById(R.id.log_button);
        logging = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logButton.setText(R.string.stop_logging);
            }
        });
        logCarData();
    }

    private void logStop() {
        final Button logButton = (Button) findViewById(R.id.log_button);
        logging = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logButton.setText(R.string.start_logging);
            }
        });
        try {
            logService.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.w(TAG, "Could not terminate logging service");
            logService.shutdownNow();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        descriptorQueue = new ConcurrentLinkedQueue<>();


        Intent intent = getIntent();
        BluetoothDevice device = intent.getParcelableExtra("BTLE_DEVICE");
        if (device != null) {
            carData = new CarData();
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);

            mProgressDialog = ProgressDialog.show(DisplayActivity.this, "", "Connecting...", true, true, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    mBluetoothGatt.disconnect();
                }
            });

        } else {
            // If the device is null, then simulate the car data
            carData = new CarDataSim();
            ((CarDataSim) carData).run(50);
            Toast.makeText(this, "Simulating data...", Toast.LENGTH_SHORT).show();

            setBtConnectedIndicator(true);
        }

        // Set fragment to console fragment
        if (findViewById(R.id.display_frame) != null) {
            if (savedInstanceState != null) {
                return;
            }

            ConsoleFragment consoleFragment = new ConsoleFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.display_frame, consoleFragment).commit();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_WRITE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final Button scanButton = (Button) findViewById(R.id.scan_button);
                            assert scanButton != null;
                            scanButton.setEnabled(true);
                        }
                    });
                } else {

                    logStart();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
        }
    }

    private void setBtConnectedIndicator(final boolean connected) {
        final ImageView indicator = (ImageView) findViewById(R.id.bt_status);
        if (indicator != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (connected) {
                        indicator.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.bt_status_connected, null));
                    } else {
                        indicator.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.bt_status_disconnected, null));
                    }
                }
            });
        }
    }

    public CarData getCarData() {
        return carData;
    }

    @Override
    protected void onDestroy() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            setBtConnectedIndicator(false);
        }
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        if (carData instanceof CarDataSim) {
            ((CarDataSim)carData).stop();
        }
        super.onDestroy();
    }
}

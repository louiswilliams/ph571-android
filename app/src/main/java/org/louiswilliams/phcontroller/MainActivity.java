package org.louiswilliams.phcontroller;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public  static final String ADAFRUIT_ADDR = "C2:89:50:70:61:48";

    public static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int SCAN_PERIOD = 5000;
    private static final int PERMISSION_REQUEST_LOCATION = 123;

    private DeviceListAdapter mDeviceAdapter;
    private Set<BluetoothDevice> mDevices = new HashSet<>();

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mScanHandler;
    private boolean mScanning;


    // Add devices as they come in
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDevices.add(device);
                    if (device.getAddress().equalsIgnoreCase(ADAFRUIT_ADDR)) {
                        scanLeDevices(false);
                        connectToDevice(device);
                    }
                }
            });
        }
    };

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the device list
        ListView mDeviceList = (ListView) findViewById(R.id.le_device_list);
        mDeviceAdapter = new DeviceListAdapter(this, R.layout.device_item);
        if (mDeviceList != null) {
            mDeviceList.setAdapter(mDeviceAdapter);
        }

        // Connect to the Bluetooth device when the item is selected.
        mDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get device and start activity to connect and display GATT data
                BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);
                connectToDevice(device);
            }
        });

        // Scan Button handler
        mScanHandler = new Handler();
        final Button scanButton = (Button) findViewById(R.id.scan_button);
        assert scanButton != null;
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevices(!mScanning);
            }
        });

        // Request Blueooth permissions if not already granted
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_LOCATION);

                // PERMISSION_REQUEST_LOCATION is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        // Request to enable Bluetooth if not enabled
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            scanLeDevices(true);
        }

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_LOCATION: {
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

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
        }
    }


    private void scanLeDevices(final boolean enable) {
        final Button scanButton = (Button) findViewById(R.id.scan_button);
        if (enable) {
            assert scanButton != null;
            // Disable scan button and set text
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    scanButton.setText("Stop Scanning");
                }
            });

            // Stops scanning after a pre-defined scan period.
            // Adds the devices from the set to to the list adapter
            // Updates the text of the scan button
            mScanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDeviceAdapter.addAll(mDevices);
                            mDeviceAdapter.notifyDataSetChanged();
                            scanButton.setText(R.string.scan_button);
                        }
                    });

                }
            }, SCAN_PERIOD);

            mScanning = true;
            // Reset device set and listadapter
            mDevices.clear();
            mDeviceAdapter.clear();
            mDeviceAdapter.notifyDataSetChanged();
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        Intent i = new Intent(MainActivity.this, DisplayActivity.class);
        i.putExtra("BTLE_DEVICE", device);
        startActivity(i);
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}

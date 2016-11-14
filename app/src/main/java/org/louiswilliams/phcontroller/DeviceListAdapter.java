package org.louiswilliams.phcontroller;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {

    private int resource;

    public DeviceListAdapter(Context context, int resource) {
        super(context, resource);
        this.resource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BluetoothDevice device = getItem(position);
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(resource, parent, false);
            TextView name = (TextView) view.findViewById(R.id.device_item_name);
            TextView addr = (TextView) view.findViewById(R.id.device_item_addr);
            name.setText(device.getName());
            addr.setText(device.getAddress());
        }
        return view;
    }

}

package com.hadroncfy.aircraft;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SearchDeviceActivity extends AppCompatActivity implements DialogInterface.OnClickListener,AdapterView.OnItemClickListener {

    private static final int REQUEST_ENABLE_BT = 1;
    private ListView discoveryedDeviceList;
    private DeviceAdapter discoveringAdapter;

    private ListView pairedDevices;
    private DeviceAdapter pairedDeviceAdapter;

    private BluetoothAdapter bAdapter;
    private Resources res;
    private BroadcastReceiver blueReceiver;

    private AlertDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searchdevice);

        bAdapter = BluetoothAdapter.getDefaultAdapter();

        res = getResources();

        discoveryedDeviceList = (ListView) findViewById(R.id.devices);
        discoveringAdapter = new DeviceAdapter(this);
        discoveryedDeviceList.setAdapter(discoveringAdapter);
        discoveryedDeviceList.setOnItemClickListener(this);

        pairedDevices = (ListView) findViewById(R.id.paired_devices);
        pairedDeviceAdapter = new DeviceAdapter(this);
        pairedDevices.setAdapter(pairedDeviceAdapter);
        pairedDevices.setOnItemClickListener(this);

        dialog = new AlertDialog.Builder(this)
                .setTitle(res.getString(R.string.bluetooth))
                .setMessage(res.getString(R.string.bluetooth_hint))
                .setPositiveButton(res.getString(R.string.oui),this)
                .setNegativeButton(res.getString(R.string.non),this)
                .create();

        blueReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                //device discoveryed
                if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                    // obenir devoir depuis l'intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    discoveringAdapter.addDevice(device);
                }
                else if(action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)){

                }
                else if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){

                }
            }
        };

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(blueReceiver, filter);

        checkBluetooth();
    }

    private void checkBluetooth(){
        if(dialog.isShowing()){
            dialog.hide();
        }
        if(!bAdapter.isEnabled()){
            dialog.show();
        }
        else {
            updatePairedDevices();
            startSearch();
        }
    }

    private void updatePairedDevices(){
        if(bAdapter.isEnabled()){
            for(BluetoothDevice device : bAdapter.getBondedDevices()){
                pairedDeviceAdapter.addDevice(device);
            }
        }
    }

    private void startSearch(){
        bAdapter.startDiscovery();
    }

    private void stopSearch(){
        bAdapter.cancelDiscovery();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSearch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkBluetooth();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(blueReceiver);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        Toast.makeText(this,Integer.toString(i),Toast.LENGTH_SHORT).show();
        switch(i){
            case -1:
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

                break;
            case -2:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ENABLE_BT){
            if(resultCode == -1){
                updatePairedDevices();
                startSearch();
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        DeviceAdapter adapter = (DeviceAdapter) adapterView.getAdapter();
        BluetoothDevice device = (BluetoothDevice) adapter.getItem(i);
        Intent intent = new Intent(this,ControlActivity.class);
        intent.putExtra("device",device);
        startActivity(intent);
    }

    private class DeviceAdapter extends BaseAdapter {
        private class ViewHolder{
            TextView name;
            TextView mac;
        }

        private Context ctx;
        private LayoutInflater inflater;
        private List<BluetoothDevice> devices = new ArrayList<>();

        public DeviceAdapter(Context ctx){
            this.ctx = ctx;
            inflater = LayoutInflater.from(ctx);
        }
        public void addDevice(BluetoothDevice device){
            if(!devices.contains(device))
                devices.add(device);
            notifyDataSetChanged();
        }
        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int i) {
            return devices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder holder;
            if(view == null){
                view = inflater.inflate(R.layout.adapter_bluetooth_device,null);

                holder = new ViewHolder();
                holder.name = (TextView) view.findViewById(R.id.device_name);
                holder.mac = (TextView) view.findViewById(R.id.device_mac);

                view.setTag(holder);
            }
            else {
                holder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = devices.get(i);
            String name = device.getName();
            if(name == null){
                holder.name.setText(res.getString(R.string.untitled));
            }
            else {
                holder.name.setText(device.getName());
            }
            holder.mac.setText(device.getAddress());
            return view;
        }
    }
}

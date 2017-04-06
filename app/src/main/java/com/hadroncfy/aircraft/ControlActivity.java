package com.hadroncfy.aircraft;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hadroncfy.aircraft.device.AirCraftDevice;

import java.io.IOException;


/**
 * Created by cfy on 17-3-1.
 *
 */

public class ControlActivity extends AppCompatActivity implements View.OnClickListener,Handler.Callback {
    private Button btnDevelop;
    static AirCraftDevice device = new AirCraftDevice();
    private BluetoothDevice bDevice;
    private Resources res;
    private TextView connectDeviceText;

    private AlertDialog connectingHint;

    private static final int MESSAGE_CONNECTED = 1;
    private static final int MESSAGE_CONNECT_FAILED = 2;

    private Handler handler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        bDevice = getIntent().getParcelableExtra("device");

        res = getResources();

        handler = new Handler(this);

        btnDevelop = (Button) findViewById(R.id.btn_develop);
        btnDevelop.setOnClickListener(this);
        connectDeviceText = (TextView) findViewById(R.id.connected_device);

        connectingHint = new AlertDialog.Builder(this)
                .setMessage(res.getString(R.string.connecting))
                .setCancelable(false)
                .create();

        connectDevice();
    }

    private void connectDevice(){
        connectingHint.show();
        new ConnectThread().start();
    }

    private String convertDeviceName(String name){
        if(name.equals("")){
            return res.getString(R.string.untitled);
        }
        else {
            return name;
        }
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(this,DevelopActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            device.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        switch(message.what){
            case MESSAGE_CONNECTED:
                connectDeviceText.setText(res.getString(R.string.connected_to) + " " + convertDeviceName(bDevice.getName()));
                connectingHint.hide();
                break;
            case MESSAGE_CONNECT_FAILED:
                connectingHint.hide();
                Toast.makeText(ControlActivity.this,res.getString(R.string.failed_to_connect),Toast.LENGTH_SHORT).show();
                finish();
                break;
        }
        return false;
    }

    private class ConnectThread extends Thread{
        public void run(){
            try {
                device.connect(bDevice);
                handler.sendEmptyMessage(MESSAGE_CONNECTED);
            } catch (IOException e) {
                e.printStackTrace();
                handler.sendEmptyMessage(MESSAGE_CONNECT_FAILED);
            }
        }
    }
}

package com.hadroncfy.aircraft.device;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.hadroncfy.aircraft.BuildConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by cfy on 17-3-1.
 *
 */

public class AirCraftDevice {
    public static final int MODE_AUTO = 1;
    public static final int MODE_MANUAL = 2;


    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int DELAY = 1;
    private BluetoothSocket socket;
    private ConnectionThread thread;

    InputStream is;
    OutputStream os;

    private boolean connected = false;


    public AirCraftDevice(){
    }

    public synchronized void connect(BluetoothDevice device) throws IOException {
        socket = device.createRfcommSocketToServiceRecord(MY_UUID);
        socket.connect();
        is = socket.getInputStream();
        os = socket.getOutputStream();
        thread = new ConnectionThread();
        thread.start();
        connected = true;
    }

    public synchronized void disconnect() throws IOException {
        socket.close();
    }

    public synchronized void sendFloatParam(int addr,float val) throws IOException {
        int raw = Float.floatToIntBits(val);
        byte[] data = new byte[]{'^','p', (byte) addr, (byte) (raw >> 24), (byte) (raw >> 16), (byte) (raw >> 8), (byte) raw,'$'};

        os.write(data);
        os.flush();
        delay();
    }

    public synchronized void setMotorEnabled(boolean enabled) throws IOException {
        byte[] data = new byte[]{'^','m', (byte) (enabled ? 1 : 0),'$'};
        os.write(data);
        os.flush();
        delay();
    }

    public synchronized void setThrottle(int val) throws IOException {
        if(BuildConfig.DEBUG && !(val >= 0 && val <= 10000)) throw new IllegalArgumentException("throttle value" + val  + " is out of range");
        byte[] data = new byte[]{'^','t', (byte) (val >> 24), (byte) (val >> 16), (byte) (val >> 8), (byte) val,'$'};
        os.write(data);
        os.flush();
        delay();
    }

    public synchronized void setGyro(int which,float val) throws IOException {
        if(BuildConfig.DEBUG && !(which >= 0 && which <= 2)) throw new IllegalArgumentException("'which' cannot be " + which);
        int raw = Float.floatToIntBits(val);
        byte[] data = new byte[]{'^','g', (byte) which, (byte) (raw >> 24), (byte) (raw >> 16), (byte) (raw >> 8), (byte) raw,'$'};

        os.write(data);
        os.flush();
        delay();
    }

    public synchronized void setMode(int mode) throws IOException {
        if(mode != MODE_AUTO && mode != MODE_MANUAL){
            throw new IllegalArgumentException("mode " + mode + " is not supported");
        }
        byte[] data = new byte[]{'^','o', (byte) mode,'$'};
        os.write(data);
        os.flush();
        delay();
    }

    public synchronized boolean checkStatus() throws IOException {
        byte[] data = new byte[]{'^','s','$'};
        os.write(data);
        os.flush();
        delay();

        return is.read() == 0;
    }

    private void delay(){
        try {
            Thread.sleep(DELAY);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            super.run();

        }
    }

}

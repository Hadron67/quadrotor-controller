package com.hadroncfy.aircraft;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hadroncfy.aircraft.device.AirCraftDevice;
import com.hadroncfy.aircraft.device.DeviceException;
import com.hadroncfy.aircraft.device.PIDParametres;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.PI;
import static java.lang.Math.abs;

/**
 * Created by cfy on 17-3-1.
 */

public class DevelopActivity extends AppCompatActivity implements
        View.OnClickListener,
        AdapterView.OnItemSelectedListener,
        Handler.Callback, SeekBar.OnSeekBarChangeListener

{

    private static final int MSG_SEND_PID_FINISHED = 1;
    private static final int MSG_SEND_PID_FAILED = 2;
    private static final int MSG_SET_MODE_FINISHED = 3;
    private static final int MSG_SET_MODE_FAILED = 4;

    private Handler handler;

    private AppCompatSpinner spinner,spinnerMode;
    private ArrayAdapter adapterSpinner,spinnerModeAdapter;
    private EditText pidP,pidI,pidD;
    private TextView pLabel,iLabel,dLabel;
    private Button btnSend;

    private Button btnOn,btnOff,btnReset;
    private SeekBar seekBarThrottle,seekBarGx,seekBarGy,seekBarGz;

    private Resources res;

    private PIDParametres parametres = new PIDParametres();

    private int pidSet = 0;
    private int mode = AirCraftDevice.MODE_AUTO,lastMode = mode;

    private Timer sendThrottleTimer = new Timer();
    private TimerTask sendThrottleTask;
    private int currentThrottle = 0,lastThrottle = 0;
    private int currentGx = 0,lastGx = 0;
    private int currentGy = 0,lastGy = 0;
    private int currentGz = 0,lastGz = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_devlop);
        res = getResources();

        adapterSpinner = ArrayAdapter.createFromResource(this,R.array.pid_selector,android.R.layout.simple_spinner_item);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner = (AppCompatSpinner) findViewById(R.id.pid_selector);
        spinner.setAdapter(adapterSpinner);
        spinner.setOnItemSelectedListener(this);

        spinnerModeAdapter = ArrayAdapter.createFromResource(this,R.array.modes,android.R.layout.simple_spinner_item);
        spinnerModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMode = (AppCompatSpinner) findViewById(R.id.mode_selector);
        spinnerMode.setAdapter(spinnerModeAdapter);
        spinnerMode.setOnItemSelectedListener(this);

        pidP = (EditText) findViewById(R.id.pid_p);
        pidI = (EditText) findViewById(R.id.pid_i);
        pidD = (EditText) findViewById(R.id.pid_d);
        pLabel = (TextView) findViewById(R.id.p_label);
        iLabel = (TextView) findViewById(R.id.i_label);
        dLabel = (TextView) findViewById(R.id.d_label);

        btnOn = (Button) findViewById(R.id.on);
        btnOff = (Button) findViewById(R.id.off);
        btnReset = (Button) findViewById(R.id.btn_reset);
        seekBarThrottle = (SeekBar) findViewById(R.id.throttle);
        seekBarGx = (SeekBar) findViewById(R.id.sb_gx);
        seekBarGy = (SeekBar) findViewById(R.id.sb_gy);
        seekBarGz = (SeekBar) findViewById(R.id.sb_gz);
        seekBarThrottle.setMax(10000);
        seekBarGx.setMax(10000);
        seekBarGy.setMax(10000);
        seekBarGz.setMax(10000);
        seekBarGx.setProgress(5000);
        seekBarGy.setProgress(5000);
        seekBarGz.setProgress(5000);

        pidP.addTextChangedListener(new PIDWatcher(0));
        pidI.addTextChangedListener(new PIDWatcher(1));
        pidD.addTextChangedListener(new PIDWatcher(2));

        btnSend = (Button) findViewById(R.id.pid_send);

        btnSend.setOnClickListener(this);
        btnOn.setOnClickListener(this);
        btnOff.setOnClickListener(this);
        btnReset.setOnClickListener(this);

        seekBarThrottle.setOnSeekBarChangeListener(this);
        seekBarGx.setOnSeekBarChangeListener(this);
        seekBarGy.setOnSeekBarChangeListener(this);
        seekBarGz.setOnSeekBarChangeListener(this);

        loadParameters();
        presentPIDs();
        presentStatus();

        handler = new Handler(this);

    }

    private void loadParameters(){
        try {
            FileInputStream fi = openFileInput("pid.dat");
            parametres.readFrom(fi);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveParameters() throws IOException {
        FileOutputStream fo = openFileOutput("pid.dat",MODE_PRIVATE);
        parametres.writeTo(fo);
    }


    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.pid_send:
                block();
                btnSend.setText(R.string.sending);
                new SendPIDThread().start();
                break;
            case R.id.on:
                try {
                    ControlActivity.device.setMotorEnabled(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.off:
                try {
                    ControlActivity.device.setMotorEnabled(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_reset:
                seekBarThrottle.setProgress(0);
                seekBarGx.setProgress(5000);
                seekBarGy.setProgress(5000);
                seekBarGz.setProgress(5000);
                break;
        }
    }

    private void block(){
        btnSend.setEnabled(false);
        spinnerMode.setEnabled(false);
        seekBarThrottle.setEnabled(false);
        seekBarGx.setEnabled(false);
        seekBarGy.setEnabled(false);
        seekBarGz.setEnabled(false);
        btnOn.setEnabled(false);
        btnOff.setEnabled(false);
    }

    private void unblock(){
        btnSend.setEnabled(true);
        spinnerMode.setEnabled(true);
        seekBarThrottle.setEnabled(true);
        seekBarGx.setEnabled(true);
        seekBarGy.setEnabled(true);
        seekBarGz.setEnabled(true);
        btnOn.setEnabled(true);
        btnOff.setEnabled(true);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        switch(adapterView.getId()){
            case R.id.pid_selector:
                pidSet = i;
                presentPIDs();
                presentStatus();
                break;
            case R.id.mode_selector:
                if(mode != i + 1){
                    mode = i + 1;
                    block();
                    new SetModeThread().start();
                }
                break;
        }
    }

    private void presentStatus(){
        int i = pidSet;
        pLabel.setText(parametres.hasChanged(i,0) ? res.getString(R.string.pid_p_changed) : res.getString(R.string.pid_p));
        iLabel.setText(parametres.hasChanged(i,1) ? res.getString(R.string.pid_i_changed) : res.getString(R.string.pid_i));
        if(i <= 2) {
            dLabel.setText(parametres.hasChanged(i, 2) ? res.getString(R.string.pid_d_changed) : res.getString(R.string.pid_d));
        }
        else {
            dLabel.setText(res.getString(R.string.pid_d));
        }
    }
    private void presentPIDs(){
        int i = pidSet;
        pidP.setText(Float.toString(parametres.getParameter(i,0)));
        pidI.setText(Float.toString(parametres.getParameter(i,1)));
        if(i >= 3){
            pidD.setText("");
            pidD.setEnabled(false);
        }
        else {
            pidD.setEnabled(true);
            pidD.setText(Float.toString(parametres.getParameter(i,2)));
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public boolean handleMessage(Message message) {
        switch(message.what){
            case MSG_SEND_PID_FINISHED:
                presentStatus();
                presentPIDs();
                unblock();
                btnSend.setText(R.string.send);
                break;
            case MSG_SEND_PID_FAILED:
                Toast.makeText(this,res.getString(R.string.send_failed),Toast.LENGTH_SHORT).show();
                btnSend.setText(R.string.send);
                unblock();
                break;
            case MSG_SET_MODE_FINISHED:
                lastMode = mode;
                unblock();
                break;
            case MSG_SET_MODE_FAILED:
                Toast.makeText(this,res.getString(R.string.failed_to_set_mode),Toast.LENGTH_SHORT).show();
                spinnerMode.setOnItemSelectedListener(null);
                spinnerMode.setSelection(lastMode - 1);
                spinnerMode.setOnItemSelectedListener(this);
                unblock();
                mode = lastMode;
                break;
            default:throw new AssertionError("unknown message code " + message.what);
        }
        return false;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        switch(seekBar.getId()){
            case R.id.throttle:
                currentThrottle = i;
                break;
            case R.id.sb_gx:
                currentGx = i;
                break;
            case R.id.sb_gy:
                currentGy = i;
                break;
            case R.id.sb_gz:
                currentGz = i;
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        sendThrottleTask = new SendThrottleTask();
        sendThrottleTimer.scheduleAtFixedRate(sendThrottleTask,0,50);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        try {
            switch (seekBar.getId()) {
                case R.id.sb_gx:
                    seekBarGx.setProgress(5000);
                    ControlActivity.device.setGyro(0, 0);
                    break;
                case R.id.sb_gy:
                    seekBarGy.setProgress(5000);
                    ControlActivity.device.setGyro(1, 0);
                    break;
                case R.id.sb_gz:
                    seekBarGz.setProgress(5000);
                    ControlActivity.device.setGyro(2, 0);
                    break;
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
        sendThrottleTask.cancel();
    }

    private class PIDWatcher implements TextWatcher{
        int which;
        PIDWatcher(int which){
            this.which = which;
        }
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            try {
                float val = Float.parseFloat(editable.toString());
                parametres.setParameter(val,pidSet,which);
                saveParameters();
                presentStatus();
            }
            catch (NumberFormatException e){

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private class SendPIDThread extends Thread{
        @Override
        public void run() {
            super.run();
            try {
                parametres.sendPIDSync(ControlActivity.device,false);
            } catch (IOException | DeviceException e) {
                e.printStackTrace();
                handler.sendEmptyMessage(MSG_SEND_PID_FAILED);
            }
            handler.sendEmptyMessage(MSG_SEND_PID_FINISHED);

        }
    }
    private class SetModeThread extends Thread{
        @Override
        public void run() {
            super.run();
            AirCraftDevice device = ControlActivity.device;
            try {
                device.setMode(mode);
                if(!device.checkStatus()){
                    handler.sendEmptyMessage(MSG_SET_MODE_FAILED);
                }
                else {
                    handler.sendEmptyMessage(MSG_SET_MODE_FINISHED);
                }
            } catch (IOException e) {
                e.printStackTrace();
                handler.sendEmptyMessage(MSG_SET_MODE_FAILED);
            }
        }
    }
    private class SendThrottleTask extends TimerTask{

        @Override
        public void run() {
            try {
                if(abs(currentThrottle - lastThrottle) >= 100){
                    ControlActivity.device.setThrottle(lastThrottle = currentThrottle);
                }
                if(abs(currentGx - lastGx) >= 10){
                    float val = (float) ((currentGx - 5000) / 10000f * PI);
                    ControlActivity.device.setGyro(0,val);
                    lastGx = currentGx;
                }
                if(abs(currentGy - lastGy) >= 10){
                    float val = (float) ((currentGy - 5000) / 10000f * PI);
                    ControlActivity.device.setGyro(1,val);
                    lastGy = currentGy;
                }
                if(abs(currentGz - lastGz) >= 10){
                    float val = (float) ((currentGz - 5000) / 5000f * PI);
                    ControlActivity.device.setGyro(2,val);
                    lastGz = currentGz;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

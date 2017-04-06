package com.hadroncfy.aircraft.device;

import com.hadroncfy.aircraft.BuildConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Created by cfy on 17-3-1.
 *
 */

public class PIDParametres implements Serializable {

    private float[] pid = new float[15];
    private float[] last = new float[15];

    public PIDParametres(){
    }

    private static byte[] floatArray2byte(float[] val){
        byte[] ret = new byte[val.length * 4];
        for(int i = 0;i < val.length;i++){
            int raw = Float.floatToIntBits(val[i]);
            ret[4 * i + 0] = (byte) (raw >> 24);
            ret[4 * i + 1] = (byte) (raw >> 16);
            ret[4 * i + 2] = (byte) (raw >> 8);
            ret[4 * i + 3] = (byte) raw;
        }
        return ret;
    }

    private static float[] readFloat(InputStream is,int len) throws IOException {
        float[] ret = new float[len];
        for(int i = 0;i < len;i++){
            int raw = 0;
            raw |= (long)is.read();
            raw <<= 8;
            raw |= (long)is.read();
            raw <<= 8;
            raw |= (long)is.read();
            raw <<= 8;
            raw |= (long)is.read();

            ret[i] = Float.intBitsToFloat(raw);
        }
        return ret;
    }

    public void writeTo(OutputStream os) throws IOException {
        byte[] pidByte = floatArray2byte(pid);
        os.write(pidByte);
    }

    public void readFrom(InputStream is) throws IOException {
        pid = readFloat(is,pid.length);
        for(int i = 0;i < last.length;i++){
            last[i] = 0;
        }
    }

    private static int getIndex(int group,int which){
        if(BuildConfig.DEBUG && !(which >= 0 && which <= 2)) throw new IllegalArgumentException("'which' cannot be " + which);
        if(BuildConfig.DEBUG && !(group >= 0 && group <= 5)) throw new IllegalArgumentException("'group' cannot be " + group);
        if(group < 3){
            return group * 3 + which;
        }
        else {
            if(BuildConfig.DEBUG && which > 1) throw new IllegalArgumentException("'which' cannot be 2 when group > 2");
            group -= 3;
            return group * 2 + which + 9;
        }
    }

    public void setParameter(float val,int group,int which){
        int i = getIndex(group, which);
        pid[i] = val;
    }
    public float getParameter(int group,int which){
        return pid[getIndex(group,which)];
    }
    public boolean hasChanged(int group,int which){
        int i = getIndex(group, which);
        return last[i] != pid[i];
    }
    public void sendPIDSync(AirCraftDevice device, boolean all) throws IOException, DeviceException {
        for(int i = 0;i < pid.length;i++){
            if(last[i] != pid[i] || all){
                device.sendFloatParam(i,pid[i]);
                if(!device.checkStatus()){
                    throw new DeviceException();
                }
                last[i] = pid[i];
            }
        }
    }

}

package com.ulangch.hcontroller.model;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;

/**
 * Created by xyzc on 18-3-16.
 */

public class HBluetoothDevice {

    public static final String MAC_ADDRESS_ANY = "00:00:00:00:00:00";

    private int state;
    private String address;
    private BluetoothDevice device;

    public HBluetoothDevice () {
        state = BluetoothProfile.STATE_DISCONNECTED;
        address = MAC_ADDRESS_ANY;
    }

    public HBluetoothDevice(BluetoothDevice d) {
        state = BluetoothProfile.STATE_DISCONNECTED;
        if (d != null) {
            address = d.getAddress();
            device = d;
        }
    }

    public HBluetoothDevice(int s, BluetoothDevice d) {
        state = s;
        if (d != null) {
            address = d.getAddress();
            device = d;
        }
    }

    public HBluetoothDevice(int s, String addr, BluetoothDevice d) {
        state = s;
        address = addr;
        device = d;
    }

    public void setState(int s) {
        state = s;
    }

    public int getState() {
        return state;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public boolean isAvailable() {
        return state == BluetoothProfile.STATE_DISCONNECTED;
    }

    public boolean isConnected() {
        return state == BluetoothProfile.STATE_CONNECTED;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HBluetoothDevice[address=");
        sb.append(address);
        sb.append(", state=");
        sb.append(String.valueOf(state));
        sb.append("]");
        return sb.toString();
    }
}

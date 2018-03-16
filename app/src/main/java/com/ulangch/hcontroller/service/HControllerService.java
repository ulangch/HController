package com.ulangch.hcontroller.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.ulangch.hcontroller.utils.HUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xyzc on 18-3-15.
 */

public class HControllerService extends Service{
    private static final String TAG = "HControllerService";

    private BluetoothManager mBtManager;
    private BluetoothAdapter mBtAdapter;
    private Map<String, BluetoothGatt> mConnectedGatts;
    private List<ServiceCallback> mCallbacks;

    private IBinder mBinder;

    @Override
    public void onCreate() {
        super.onCreate();
        HUtils.logD(TAG, "onCreate");
        mBinder = new LocalBinder();
        mBtManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = mBtManager.getAdapter();
        mConnectedGatts = new HashMap<>();
        mCallbacks = new ArrayList<>();
    }

    @Override
    public IBinder onBind(Intent intent) {
        HUtils.logD(TAG, "onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        HUtils.logD(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        HUtils.logD(TAG, "onDestroy");
        super.onDestroy();
        for (BluetoothGatt btGatt : mConnectedGatts.values()) {
            if (btGatt != null) {
                btGatt.close();
            }
        }
    }

    public class LocalBinder extends Binder {
        public HControllerService getService() {
            return HControllerService.this;
        }
    }

    public boolean connect(String addr) {
        if (mBtAdapter == null || !HUtils.isAddress(addr)) {
            HUtils.logE(TAG, "bt adapter is null or addr is invalid");
            return false;
        }
        BluetoothGatt btGatt = mConnectedGatts.get(addr);
        if (btGatt != null) {
            HUtils.logD(TAG, "trying to use an existing bt gatt for connection");
            if (btGatt.connect()) {
                return true;
            }
            return false;
        }
        BluetoothDevice device = mBtAdapter.getRemoteDevice(addr);
        if (device == null) {
            HUtils.logE(TAG, "device not found, unable to connect");
            return false;
        }
        btGatt = device.connectGatt(this, false, mGattCallback);
        // mConnectedGatts.put(addr, btGatt);
        HUtils.logD(TAG, "trying to create a new connection");
        return true;
    }

    public boolean disconnect(String addr) {
        if (mBtAdapter == null || !HUtils.isAddress(addr)) {
            HUtils.logE(TAG, "bt adapter is null or addr is invalid");
            return false;
        }
        BluetoothGatt btGatt = mConnectedGatts.get(addr);
        if (btGatt == null) {
            HUtils.logE(TAG, "device not found, unable to disconnect");
            return false;
        }
        btGatt.disconnect();
        return true;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        HUtils.logD(TAG, "get connected devices, mConnectedGatts: " + mConnectedGatts);
        List<BluetoothDevice> devices = new ArrayList<>();
        for (BluetoothGatt btGatt : mConnectedGatts.values()) {
            devices.add(btGatt.getDevice());
        }
        return devices;
    }

    public void registerCallback(ServiceCallback c) {
        synchronized (mCallbacks) {
            if (mCallbacks.contains(c)) {
                throw new IllegalArgumentException("can not register same callback");
            }
            mCallbacks.add(c);
        }
    }

    public void unResgiterCallback(ServiceCallback c) {
        synchronized (mCallbacks) {
            mCallbacks.remove(c);
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BluetoothDevice device = gatt.getDevice();
            HUtils.logD(TAG, "connection state changed, device: " + device.getName() + ", state: " + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectedGatts.put(device.getAddress(), gatt);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectedGatts.remove(device.getAddress());
            }
            synchronized (mCallbacks) {
                for (ServiceCallback c : mCallbacks) {
                    c.onDeviceStateChanged(device, newState);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }
    };

    public interface ServiceCallback {
        void onDeviceStateChanged(BluetoothDevice device, int state);
    }
}

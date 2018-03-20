package com.ulangch.hcontroller.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.ulangch.hcontroller.utils.GattAttributes;
import com.ulangch.hcontroller.utils.HUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by xyzc on 18-3-15.
 */

public class HControllerService extends Service{
    private static final String TAG = "HControllerService";

    public static final int SUCCESS = 1;
    public static final int FAIL_UNKNOWN = -1;
    public static final int FAIL_GATT_NULL = -2;
    public static final int FAIL_SERVICE_NULL = -3;
    public static final int FAIL_CHARACT_NULL = -4;
    public static final int FAIL_CHARACT_CANNOT_WRITE = -5;
    public static final int FAIL_PROPERTY_NULL = -6;

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

    public boolean discoverServices(String addr) {
        HUtils.logD(TAG, "discover services, addr: " + addr);
        BluetoothGatt gatt = mConnectedGatts.get(addr);
        if (gatt != null) {
            return gatt.discoverServices();
        }
        HUtils.logE(TAG, "discover services failed, no gatt for addr: " + addr);
        return false;
    }

    public List<BluetoothGattService> getRemoteServices(String addr) {
        BluetoothGatt gatt = mConnectedGatts.get(addr);
        return gatt != null ? gatt.getServices() : null;
    }

    public int sendToRemoteService(String addr, String svcUUID, String charUUID, byte[] bytes) {
        BluetoothGatt gatt = mConnectedGatts.get(addr);
        if (gatt == null) {
            HUtils.logE(TAG, String.format("send failed for gatt null, [%s, %s, %s, %s]"
                    , addr, svcUUID, charUUID, HUtils.bytesToHex(bytes)));
            return FAIL_GATT_NULL;
        }
        BluetoothGattService remote = gatt.getService(UUID.fromString(svcUUID));
        if (remote == null) {
            HUtils.logE(TAG, String.format("send failed for remote null, [%s, %s, %s, %s]"
                    , addr, svcUUID, charUUID, HUtils.bytesToHex(bytes)));
            return FAIL_SERVICE_NULL;
        }
        BluetoothGattCharacteristic charact = remote.getCharacteristic(UUID.fromString(charUUID));
        if (charact == null) {
            HUtils.logE(TAG, String.format("send failed for charact null, [%s, %s, %s, %s]"
                    , addr, svcUUID, charUUID, HUtils.bytesToHex(bytes)));
            return FAIL_CHARACT_NULL;
        }
        if ((charact.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0) {
            HUtils.logE(TAG, String.format("send failed for charact cannot write, [%s, %s, %s, %s]"
                    , addr, svcUUID, charUUID, HUtils.bytesToHex(bytes)));
            return FAIL_CHARACT_CANNOT_WRITE;
        }
        if (charact.setValue(bytes) && gatt.writeCharacteristic(charact)) {
            return SUCCESS;
        } else {
            return FAIL_UNKNOWN;
        }
    }

    public int setCharacteristicNotification(String addr, String svcUUID, String charUUID) {
        BluetoothGatt gatt = mConnectedGatts.get(addr);
        if (gatt == null) {
            HUtils.logE(TAG, String.format("notification failed for gatt null, [%s, %s, %s]"
                    , addr, svcUUID, charUUID));
            return FAIL_GATT_NULL;
        }
        BluetoothGattService remote = gatt.getService(UUID.fromString(svcUUID));
        if (remote == null) {
            HUtils.logE(TAG, String.format("notification failed for remote null, [%s, %s, %s]"
                    , addr, svcUUID, charUUID));
            return FAIL_SERVICE_NULL;
        }
        BluetoothGattCharacteristic charact = remote.getCharacteristic(UUID.fromString(charUUID));
        if (charact == null) {
            HUtils.logE(TAG, String.format("notification failed for charact null, [%s, %s, %s]"
                    , addr, svcUUID, charUUID));
            return FAIL_CHARACT_NULL;
        }
        byte[] value = null;
        if ((charact.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
            value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
        } else if ((charact.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0){
            value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
        }
        if (value != null) {
            gatt.setCharacteristicNotification(charact, true);
            BluetoothGattDescriptor descriptor = charact.getDescriptor(UUID.fromString(GattAttributes.UUID_CLIENT_CONFIG));
            return descriptor.setValue(value) ? SUCCESS : FAIL_UNKNOWN;
        }
        HUtils.logE(TAG, String.format("notification failed for value null, [%s, %s, %s]"
                , addr, svcUUID, charUUID));
        return FAIL_PROPERTY_NULL;
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
            BluetoothDevice device = gatt.getDevice();
            HUtils.logD(TAG, "remote service discovered, device: " + device.getName() + ", status: " + status);
            synchronized (mCallbacks) {
                for (ServiceCallback c : mCallbacks) {
                    c.onServicesDiscovered(device, (status == BluetoothGatt.GATT_SUCCESS));
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            HUtils.logD(TAG, String.format("onCharacteristicRead, device: %s, character: %s, value: %s"
                    , gatt.getDevice().getAddress(), characteristic.getUuid(), HUtils.bytesToHex(characteristic.getValue())));
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            HUtils.logD(TAG, String.format("onCharacteristicChanged, device: %s, character: %s, value: %s"
                    , gatt.getDevice().getAddress(), characteristic.getUuid(), HUtils.bytesToHex(characteristic.getValue())));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            HUtils.logD(TAG, String.format("onCharacteristicWrite, device: %s, character: %s, value: %s"
                    , gatt.getDevice().getAddress(), characteristic.getUuid(), HUtils.bytesToHex(characteristic.getValue())));
        }
    };

    public interface ServiceCallback {
        void onDeviceStateChanged(BluetoothDevice device, int state);
        void onServicesDiscovered(BluetoothDevice device, boolean success);
    }
}

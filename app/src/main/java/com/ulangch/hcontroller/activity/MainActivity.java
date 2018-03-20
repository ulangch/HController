package com.ulangch.hcontroller.activity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.text.TextUtils;

import com.ulangch.hcontroller.R;
import com.ulangch.hcontroller.model.HBluetoothDevice;
import com.ulangch.hcontroller.service.HControllerService;
import com.ulangch.hcontroller.utils.GattAttributes;
import com.ulangch.hcontroller.utils.HUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener
        , Preference.OnPreferenceClickListener, ServiceConnection, HControllerService.ServiceCallback{
    private static final String TAG = "MainActivity";

    private static final String KEY_CATEGORY_CONNECTED_DEVICES = "category_connected_devices";
    private static final String KEY_CONNECT_DEVICE = "pref_connect_device";
    private static final String KEY_DEVICE_PARAMETER = "pref_device_parameter";
    private static final String KEY_ADJUST_STRENGTH = "pref_adjust_strength";
    private static final String KEY_SEND_ORDER = "pref_send_order";
    private static final String KEY_PWM = "pref_pwm";
    private static final String KEY_SENSOR_DATA = "pref_sensor_data";
    private static final String KEY_SENSOR_CONTROL = "pref_sensor_control";
    private static final String KEY_SENSOR_INFO = "pref_sensor_info";
    private static final String KEY_ALLOCATE_SENSOR = "pref_allocate_sensor";

    private static final int UHDL_DEVICE_STATE_CHANGED = 0x01;
    private static final int UHDL_SERVICE_DISCOVERED = 0x02;

    private PreferenceCategory mConnectedCategory;
    private Preference mConnectPref;
    private EditTextPreference mDeviceParameterPref;
    private EditTextPreference mAdjustStrengthPref;
    private EditTextPreference mSendOrderPref;
    private Preference mPWMPref;
    private Preference mSensorDataPref;
    private EditTextPreference mSensorControlPref;
    private Preference mSensorInfoPref;
    private EditTextPreference mAllocateSensorPref;

    private Map<String, HBluetoothDevice> mConnectedDevices;

    private HBluetoothDevice mDefaultDevice;

    private UiHandler mUiHandler;
    private HControllerService mHControllerService;

    private BluetoothAdapter mBluetoothAdapter;

    private volatile boolean mRefreshed = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_main);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            HUtils.showWarning(this, R.string.warning_ble_not_support);
            finish();
        }
        final BluetoothManager btMgr = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = btMgr.getAdapter();
        if (mBluetoothAdapter == null) {
            HUtils.showWarning(this, R.string.warning_bt_not_support);
            finish();
        }
        mConnectedCategory = (PreferenceCategory) findPreference(KEY_CATEGORY_CONNECTED_DEVICES);
        mConnectPref = findPreference(KEY_CONNECT_DEVICE);
        mPWMPref = findPreference(KEY_PWM);
        mSensorDataPref = findPreference(KEY_SENSOR_DATA);
        mSensorInfoPref = findPreference(KEY_SENSOR_INFO);
        mConnectPref.setOnPreferenceClickListener(this);
        mPWMPref.setOnPreferenceClickListener(this);
        mSensorDataPref.setOnPreferenceClickListener(this);
        mSensorInfoPref.setOnPreferenceClickListener(this);
        mDeviceParameterPref = (EditTextPreference) findPreference(KEY_DEVICE_PARAMETER);
        mAdjustStrengthPref = (EditTextPreference) findPreference(KEY_ADJUST_STRENGTH);
        mSendOrderPref = (EditTextPreference) findPreference(KEY_SEND_ORDER);
        mSensorControlPref = (EditTextPreference) findPreference(KEY_SENSOR_CONTROL);
        mAllocateSensorPref = (EditTextPreference) findPreference(KEY_ALLOCATE_SENSOR);
        mDeviceParameterPref.setOnPreferenceChangeListener(this);
        mAdjustStrengthPref.setOnPreferenceChangeListener(this);
        mSendOrderPref.setOnPreferenceChangeListener(this);
        mSensorControlPref.setOnPreferenceChangeListener(this);
        mAllocateSensorPref.setOnPreferenceChangeListener(this);
        mUiHandler = new UiHandler();
        mConnectedDevices = new HashMap<>();
        Intent intent = new Intent(this, HControllerService.class);
        bindService(intent, this, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        String key = preference.getKey();
        if (TextUtils.equals(key, KEY_DEVICE_PARAMETER)) {
            validAndSendToRemote((String) o, GattAttributes.UUID_SERVICE_180B, GattAttributes.UUID_CHARACTER_1503, 6);
            // for test
            //validAndSendToRemote((String) o, GattAttributes.UUID_TEST_SERVICE_1801, GattAttributes.UUID_TEST_CHARACTER_2A05, 6);
        } else if (TextUtils.equals(key, KEY_ADJUST_STRENGTH)) {
            validAndSendToRemote((String) o, GattAttributes.UUID_SERVICE_180B, GattAttributes.UUID_CHARACTER_1506, 6);
        } else if (TextUtils.equals(key, KEY_SEND_ORDER)) {
            validAndSendToRemote((String) o, GattAttributes.UUID_SERVICE_180C, GattAttributes.UUID_CHARACTER_150A, 4);
        } else if (TextUtils.equals(key, KEY_SENSOR_CONTROL)) {
            validAndSendToRemote((String) o, GattAttributes.UUID_SERVICE_180B, GattAttributes.UUID_CHARACTER_1509, 8);
        } else if (TextUtils.equals(key, KEY_ALLOCATE_SENSOR)) {
            validAndSendToRemote((String) o, GattAttributes.UUID_SERVICE_180C, GattAttributes.UUID_CHARACTER_150C, 12);
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        HUtils.logD(TAG, "click, key: " + key);
        Intent intent = new Intent();
        if (TextUtils.equals(key, KEY_CONNECT_DEVICE)) {
            intent.setClass(this, ConnectDeviceActivity.class);
            startActivity(intent);
        } else if (TextUtils.equals(key, KEY_PWM)) {

        } else if (TextUtils.equals(key, KEY_SENSOR_DATA)) {

        } else if (TextUtils.equals(key, KEY_SENSOR_INFO)) {

        } else if (HUtils.isAddress(key)) {
            if (mDefaultDevice != null && TextUtils.equals(key, mDefaultDevice.getAddress())) {
                displayRemoteServices(preference, mDefaultDevice);
            } else {
                selectDefaultDevice(preference, mConnectedDevices.get(key));
            }
        }
        return false;
    }

    private void refresh() {
        HUtils.logD(TAG, "refresh, mRefreshed = " + mRefreshed);
        if (!mRefreshed) {
            mHControllerService.registerCallback(this);
            List<BluetoothDevice> connDevices = mHControllerService.getConnectedDevices();
            HUtils.logD(TAG, "refresh, conn devices: " + connDevices);
            List<String> keysRemove = new ArrayList<>();
            for (HBluetoothDevice hd : mConnectedDevices.values()) {
                boolean found = false;
                for (BluetoothDevice d : connDevices) {
                    if (hd.getAddress().equals(d.getAddress())) {
                        if (!hd.isConnected()) {
                            hd.setState(BluetoothProfile.STATE_CONNECTED);
                        }
                        found = true;
                    }
                }
                if (!found) {
                    keysRemove.add(hd.getAddress());
                    hd.setState(BluetoothProfile.STATE_DISCONNECTED);
                }
            }
            for (BluetoothDevice d : connDevices) {
                HBluetoothDevice hd = mConnectedDevices.get(d.getAddress());
                if (hd == null) {
                    hd = new HBluetoothDevice(BluetoothProfile.STATE_CONNECTED, d);
                    mConnectedDevices.put(hd.getAddress(), hd);
                }
            }
            for (HBluetoothDevice hd : mConnectedDevices.values()) {
                refreshUiForDeviceStateChanged(hd);
            }
            for (String key : keysRemove) {
                if (mConnectedDevices.get(key) != null) {
                    mConnectedDevices.remove(key);
                }
            }
            mRefreshed = true;
        }
    }

    private void refreshUiForDeviceStateChanged(HBluetoothDevice hDevice) {
        HUtils.logD(TAG, "refresh ui, hDevice: " + hDevice);
        Preference pref = mConnectedCategory.findPreference(hDevice.getAddress());
        if (hDevice.isConnected() && pref == null) {
            pref = new Preference(this);
            pref.setKey(hDevice.getAddress());
            pref.setTitle(hDevice.getDevice().getName());
            pref.setSummary(hDevice.getAddress());
            pref.setOnPreferenceClickListener(this);
            mConnectedCategory.addPreference(pref);
        } else if (hDevice.isAvailable() && pref != null) {
            mConnectedCategory.removePreference(pref);
        }
        getListView().requestFocusFromTouch();
        //getListView().setSelection(0);
    }

    private void selectDefaultDevice(final Preference pref, final HBluetoothDevice hDevice) {
        if (pref != null && hDevice != null) {
            HUtils.logD(TAG, "select default device, device: " + hDevice.getAddress());
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dlg_title_default_device);
            builder.setMessage(R.string.dlg_msg_default_device);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (mDefaultDevice != null) {
                        Preference old = mConnectedCategory.findPreference(mDefaultDevice.getAddress());
                        if (old != null) {
                            old.setSummary(mDefaultDevice.getAddress());
                        }
                    }
                    pref.setSummary(hDevice.getAddress() + " " + getString(R.string.discovering_service));
                    mDefaultDevice = hDevice;
                    mHControllerService.discoverServices(mDefaultDevice.getAddress());
                    getListView().requestFocusFromTouch();
                    //getListView().setSelection(0);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
        }
    }

    private void displayRemoteServices(Preference pref, HBluetoothDevice hDevice) {
        if (pref != null && hDevice != null) {
            List<BluetoothGattService> gattServices = mHControllerService.getRemoteServices(hDevice.getAddress());
            HUtils.logD(TAG, "display remote services, device: " + hDevice.getAddress() + ", result: " + gattServices);
            if (gattServices != null && !gattServices.isEmpty()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.dlg_title_remote_services);
                StringBuilder sb = new StringBuilder();
                for (BluetoothGattService bgs : gattServices) {
                    sb.append(bgs.getUuid()).append("\n");
                    List<BluetoothGattCharacteristic> characteristics = bgs.getCharacteristics();
                    for (BluetoothGattCharacteristic bgc : characteristics) {
                        sb.append(bgc.getUuid()).append("\n");
                    }
                    sb.append("----------------------------------");
                    sb.append("\n");
                }
                builder.setMessage(sb.toString());
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.show();
            }
        }
    }

    private void validAndSendToRemote(String hex, String svcUUID, String charUUID, int length) {
        if (!TextUtils.isEmpty(hex) && hex.length() == length && HUtils.isHexString(hex)) {
            if (mDefaultDevice != null) {
                int result = mHControllerService.sendToRemoteService(
                        mDefaultDevice.getAddress(), svcUUID, charUUID, HUtils.hexToBytes(hex));
                if (result == HControllerService.SUCCESS) {
                    HUtils.showWarning(this, R.string.send_success);
                } else {
                    HUtils.showWarning(this, getString(R.string.send_failed) + " CODE: " + result);
                }
            } else {
                HUtils.showWarning(this, R.string.invalid_default_device);
            }
        } else {
            HUtils.showWarning(this, R.string.invalid_text_message);
        }
    }

    private void handleDeviceStateChanged(BluetoothDevice device, int state) {
        HBluetoothDevice hDevice = mConnectedDevices.get(device.getAddress());
        if (state == BluetoothProfile.STATE_CONNECTED) {
            if (hDevice == null) {
                hDevice = new HBluetoothDevice(state, device);
                mConnectedDevices.put(hDevice.getAddress(), hDevice);
            } else {
                hDevice.setDevice(device);
                hDevice.setState(state);
            }
        } else {
            if (hDevice != null) {
                mConnectedDevices.remove(hDevice.getAddress());
            }
        }
        refreshUiForDeviceStateChanged(hDevice);
    }

    private void handleServiceDiscovered(BluetoothDevice device, boolean success) {
        if (mDefaultDevice != null && TextUtils.equals(mDefaultDevice.getAddress(), device.getAddress())) {
            Preference pref = mConnectedCategory.findPreference(device.getAddress());
            if (pref != null) {
                pref.setSummary(device.getAddress() + " " + getString(R.string.default_device));
                getListView().requestFocusFromTouch();
            }
        }
    }

    private class UiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UHDL_DEVICE_STATE_CHANGED:
                    handleDeviceStateChanged((BluetoothDevice) msg.obj, msg.arg1);
                    break;
                case UHDL_SERVICE_DISCOVERED:
                    handleServiceDiscovered((BluetoothDevice) msg.obj, msg.arg1 == 1);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onDeviceStateChanged(BluetoothDevice device, int state) {
        mUiHandler.sendMessage(mUiHandler.obtainMessage(UHDL_DEVICE_STATE_CHANGED, state, 0 , device));
    }

    @Override
    public void onServicesDiscovered(BluetoothDevice device, boolean success) {
        HUtils.logD(TAG, "services discovered, device: " + device + ", success: " + success);
        mUiHandler.sendMessage(mUiHandler.obtainMessage(UHDL_SERVICE_DISCOVERED, success ? 1 : 0, 0, device));
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mHControllerService = ((HControllerService.LocalBinder) iBinder).getService();
        refresh();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mHControllerService.unResgiterCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mHControllerService != null) {
            refresh();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mRefreshed = false;
        mHControllerService.unResgiterCallback(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }
}

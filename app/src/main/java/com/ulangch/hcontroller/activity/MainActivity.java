package com.ulangch.hcontroller.activity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import com.ulangch.hcontroller.utils.HUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        } else if (TextUtils.equals(key, KEY_ADJUST_STRENGTH)) {

        } else if (TextUtils.equals(key, KEY_SEND_ORDER)) {

        } else if (TextUtils.equals(key, KEY_SENSOR_CONTROL)) {

        } else if (TextUtils.equals(key, KEY_ALLOCATE_SENSOR)) {

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
                HUtils.showWarning(this, R.string.already_default_device);
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
                    pref.setSummary(hDevice.getAddress() + " " + getString(R.string.default_device));
                    mDefaultDevice = hDevice;
                    getListView().requestFocusFromTouch();
                    //getListView().setSelection(0);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
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

    private class UiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UHDL_DEVICE_STATE_CHANGED:
                    handleDeviceStateChanged((BluetoothDevice) msg.obj, msg.arg1);
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

package com.ulangch.hcontroller.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.SwitchPreference;
import android.text.TextUtils;

import com.ulangch.hcontroller.R;
import com.ulangch.hcontroller.model.HBluetoothDevice;
import com.ulangch.hcontroller.service.HControllerService;
import com.ulangch.hcontroller.utils.HUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xyzc on 18-3-15.
 */

public class ConnectDeviceActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener
        , ServiceConnection, HControllerService.ServiceCallback {
    private static final String TAG = "ConnectDeviceActivity";

    private static final String PREFERENCE_START_SCAN = "pref_start_scan";
    private static final String CATEGORY_CONNECTED_DEVICES = "category_connected_devices";
    private static final String CATEGORY_AVAILABLE_DEVICES = "category_available_devices";

    private static final int REQUEST_ENABLE_BT = 0x01;

    private static final int UHDL_DEVICE_STATE_CHANGED = 0x01;
    private static final int UHDL_DEVICE_NAME_CHANGED = 0x02;

    private SwitchPreference mScanPreference;
    private PreferenceCategory mConnectedCategory;
    private PreferenceCategory mAvailableCategory;

    private Map<String, HBluetoothDevice> mDevices;

    private UiHandler mUiHandler;
    private BleReceiver mBleReceiver;
    private HControllerService mHControllerService;

    private BluetoothAdapter mBluetoothAdapter;

    private volatile boolean mRefreshed = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_device);
        mScanPreference = (SwitchPreference) findPreference(PREFERENCE_START_SCAN);
        mConnectedCategory = (PreferenceCategory) findPreference(CATEGORY_CONNECTED_DEVICES);
        mAvailableCategory = (PreferenceCategory) findPreference(CATEGORY_AVAILABLE_DEVICES);
        mScanPreference.setOnPreferenceClickListener(this);
        mDevices = new HashMap<>();
        mUiHandler = new UiHandler();
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
        // Discovery broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        // filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        mBleReceiver = new BleReceiver();
        registerReceiver(mBleReceiver, filter);
        Intent intent = new Intent(this, HControllerService.class);
        bindService(intent, this, BIND_AUTO_CREATE);
        if (mBluetoothAdapter.isEnabled()) {
            mScanPreference.setChecked(true);
            onPreferenceClick(mScanPreference);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        HUtils.logD(TAG, "click, key: " + key);
        if (TextUtils.equals(PREFERENCE_START_SCAN, key)) {
            if (((SwitchPreference) preference).isChecked()) {
                ensureBtEnabled();
                mBluetoothAdapter.startDiscovery();
            } else {
                mBluetoothAdapter.cancelDiscovery();
            }
        } else if (HUtils.isAddress(key)) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            HBluetoothDevice device = mDevices.get(key);
            if (device != null) {
                if (device.isAvailable()) {
                    mHControllerService.connect(key);
                    mAvailableCategory.removePreference(preference);
                    device.setState(BluetoothProfile.STATE_CONNECTING);
                    preference.setSummary(R.string.connecting);
                    mConnectedCategory.addPreference(preference);
                    getListView().requestFocusFromTouch();
                    getListView().setSelection(0);
                } else if (device.isConnected()) {
                    showDisconnectDialog(preference, device);
                } else {
                    // ... in connecting or disconnecting state
                }
            } else {
                getPreferenceScreen().removePreference(preference);
            }
        }
        return false;
    }

    private synchronized void refresh() {
        HUtils.logD(TAG, "refresh, mRefreshed = " + mRefreshed);
        if (!mRefreshed) {
            List<BluetoothDevice> connDevices = mHControllerService.getConnectedDevices();
            HUtils.logD(TAG, "refresh, conn devices: " + connDevices);
            for (HBluetoothDevice hd : mDevices.values()) {
                boolean found = false;
                for (BluetoothDevice d : connDevices) {
                    if (hd.getAddress().equals(d.getAddress())) {
                        if (!hd.isConnected()) {
                            hd.setState(BluetoothProfile.STATE_CONNECTED);
                        }
                        found = true;
                    }
                }
                if (!found && hd.isConnected()) {
                    hd.setState(BluetoothProfile.STATE_DISCONNECTED);
                }
            }
            for (BluetoothDevice d : connDevices) {
                if (mDevices.get(d.getAddress()) == null) {
                    HBluetoothDevice hd = new HBluetoothDevice(BluetoothProfile.STATE_CONNECTED, d);
                    mDevices.put(d.getAddress(), hd);
                }
            }
            HUtils.logD(TAG, "refresh, mDevices: " + mDevices);
            for (HBluetoothDevice hd : mDevices.values()) {
                refreshUiForDeviceStateChanged(hd);
            }
            mRefreshed = true;
        }
    }

    private void refreshUiForDeviceStateChanged(HBluetoothDevice hDevice) {
        Preference pref;
        if (hDevice.isAvailable()) {
            pref = mConnectedCategory.findPreference(hDevice.getAddress());
            if (pref != null) {
                mConnectedCategory.removePreference(pref);
            }
            pref = mAvailableCategory.findPreference(hDevice.getAddress());
            if (pref == null){
                pref = new Preference(this);
                pref.setOnPreferenceClickListener(this);
                mAvailableCategory.addPreference(pref);
            }
            pref.setKey(hDevice.getAddress());
            pref.setTitle(hDevice.getDevice().getName());
            pref.setSummary(hDevice.getAddress());
        } else {
            pref = mAvailableCategory.findPreference(hDevice.getAddress());
            if (pref != null) {
                mAvailableCategory.removePreference(pref);
            }
            pref = mConnectedCategory.findPreference(hDevice.getAddress());
            if (pref == null) {
                pref = new Preference(this);
                pref.setOnPreferenceClickListener(this);
                mConnectedCategory.addPreference(pref);
            }
            pref.setKey(hDevice.getAddress());
            pref.setTitle(hDevice.getDevice().getName());
            String summary;
            if (hDevice.getState() == BluetoothProfile.STATE_DISCONNECTING) {
                summary = this.getString(R.string.disconnecting);
            } else if (hDevice.getState() == BluetoothProfile.STATE_CONNECTING) {
                summary = this.getString(R.string.connecting);
            } else {
                summary = hDevice.getAddress();
            }
            pref.setSummary(summary);
        }
        getListView().requestFocusFromTouch();
        getListView().setSelection(0);
    }

    private void showDisconnectDialog(final Preference pref, final HBluetoothDevice device) {
        HUtils.logD(TAG, "show disconnect dialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dlg_title_disconnect);
        builder.setMessage(R.string.dlg_msg_disconnect);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                /*pref.setSummary(R.string.disconnecting);
                getListView().requestFocusFromTouch();
                getListView().setSelection(0);*/
                if (mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }
                mHControllerService.disconnect(device.getAddress());
                device.setState(BluetoothProfile.STATE_DISCONNECTING);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private void handleDeviceStateChanged(BluetoothDevice device, int state) {
        HBluetoothDevice hDevice = mDevices.get(device.getAddress());
        if (hDevice != null) {
            if (hDevice.getState() == BluetoothProfile.STATE_CONNECTING) {
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    HUtils.showWarning(this, R.string.connect_success);
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    HUtils.showWarning(this, R.string.connect_failed);
                }
            }
            hDevice.setState(state);
        } else {
            hDevice = new HBluetoothDevice(state, device);
            mDevices.put(device.getAddress(), hDevice);
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

    private class BleReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            HUtils.logD(TAG, "receive action: " + action);
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state != BluetoothAdapter.STATE_ON) {
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                    mConnectedCategory.removeAll();
                    mAvailableCategory.removeAll();
                    mDevices.clear();
                    mScanPreference.setChecked(false);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                mScanPreference.setChecked(true);
                mScanPreference.setSummary(R.string.scanning);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mScanPreference.setChecked(false);
                mScanPreference.setSummary(R.string.scan_stopped);
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                /*HUtils.logD(TAG, "find device: " + device.getName() + " | " + device.getAddress());
                mAvailableDevices.add(device);
                Preference pref = new Preference(ConnectDeviceActivity.this);
                pref.setKey(device.getAddress());
                pref.setTitle(device.getName());
                pref.setSummary(device.getAddress());
                mAvailableCategory.addPreference(pref);*/
            } else if (BluetoothDevice.ACTION_NAME_CHANGED.equals(action)) {
                HBluetoothDevice hDevice = mDevices.get(device.getAddress());
                if (hDevice != null) {
                    hDevice.setDevice(device);
                } else {
                    hDevice = new HBluetoothDevice(device);
                    mDevices.put(hDevice.getAddress(), hDevice);
                }
                Preference pref = findPreference(device.getAddress());
                if (pref != null) {
                    pref.setTitle(device.getName());
                } else {
                    pref = new Preference(ConnectDeviceActivity.this);
                    pref.setKey(device.getAddress());
                    pref.setTitle(device.getName());
                    pref.setSummary(device.getAddress());
                    pref.setOnPreferenceClickListener(ConnectDeviceActivity.this);
                    mAvailableCategory.addPreference(pref);
                }
            }
        }
    }

    private void ensureBtEnabled() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent();
            intent.setAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ensureBtEnabled();
        if (mHControllerService != null) {
            refresh();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mBluetoothAdapter.cancelDiscovery();
        mScanPreference.setChecked(false);
        mRefreshed = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBleReceiver);
        unbindService(this);
        mHControllerService = null;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        HUtils.logD(TAG, "service connected");
        mHControllerService = ((HControllerService.LocalBinder) service).getService();
        mHControllerService.registerCallback(this);
        refresh();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        HUtils.logD(TAG, "service disconnected");
        mHControllerService.unResgiterCallback(this);
    }

    @Override
    public void onDeviceStateChanged(BluetoothDevice device, int state) {
        HUtils.logD(TAG, "device state changed, device: " + device.getName() + ", state: " + state);
        mUiHandler.sendMessage(mUiHandler.obtainMessage(UHDL_DEVICE_STATE_CHANGED, state, 0, device));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            ensureBtEnabled();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}

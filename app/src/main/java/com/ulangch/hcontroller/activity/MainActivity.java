package com.ulangch.hcontroller.activity;

import android.content.Intent;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.text.TextUtils;

import com.ulangch.hcontroller.R;
import com.ulangch.hcontroller.service.HControllerService;

public class MainActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener
        , Preference.OnPreferenceClickListener{

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

    private HControllerService mHControllerService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_main);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        Intent intent = new Intent();
        if (TextUtils.equals(key, KEY_CONNECT_DEVICE)) {
            intent.setClass(this, ConnectDeviceActivity.class);
            startActivity(intent);
        } else if (TextUtils.equals(key, KEY_PWM)) {

        } else if (TextUtils.equals(key, KEY_SENSOR_DATA)) {

        } else if (TextUtils.equals(key, KEY_SENSOR_INFO)) {

        }
        return false;
    }

}

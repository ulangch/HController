package com.ulangch.hcontroller.application;

import android.app.Application;
import android.content.Intent;

import com.ulangch.hcontroller.service.HControllerService;
import com.ulangch.hcontroller.utils.HUtils;

/**
 * Created by xyzc on 18-3-16.
 */

public class HControllerApplication extends Application{
    private static final String TAG = "HControllerApplication";
    private Intent mService;

    @Override
    public void onCreate() {
        HUtils.logD(TAG, "onCreate");
        super.onCreate();
        mService = new Intent();
        mService.setClass(this, HControllerService.class);
        startService(mService);
    }

    @Override
    public void onTerminate() {
        HUtils.logD(TAG, "onTerminate");
        stopService(mService);
        super.onTerminate();
    }
}

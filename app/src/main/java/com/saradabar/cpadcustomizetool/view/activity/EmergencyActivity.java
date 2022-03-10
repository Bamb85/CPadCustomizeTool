package com.saradabar.cpadcustomizetool.view.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.saradabar.cpadcustomizetool.data.crash.CrashLogger;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.service.KeepService;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.util.Toast;

import java.util.Objects;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class EmergencyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new CrashLogger(this));

        String Course = PreferenceManager.getDefaultSharedPreferences(this).getString("emergency_mode", "");

        if (!startCheck()) {
            Toast.toast(this, R.string.toast_not_completed_settings);
            finishAndRemoveTask();
            return;
        }

        if (!setSystemSettings(true)) {
            Toast.toast(this, R.string.toast_not_change);
            finishAndRemoveTask();
            return;
        }

        if (Course.contains("1")) {
            if (setDchaSettings("jp.co.benesse.touch.allgrade.b003.touchhomelauncher", "jp.co.benesse.touch.allgrade.b003.touchhomelauncher.HomeLauncherActivity")) finishAndRemoveTask();
            return;
        }

        if (Course.contains("2")) {
            if (setDchaSettings("jp.co.benesse.touch.home", "jp.co.benesse.touch.home.LoadingActivity")) finishAndRemoveTask();
            return;
        }
        Toast.toast(this, R.string.toast_execution);
        finishAndRemoveTask();
    }

    private boolean startCheck() {
        return Preferences.GET_SETTINGS_FLAG(this);
    }

    private boolean setSystemSettings(boolean study) {
        ContentResolver resolver = getContentResolver();

        if (study) {
            SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);

            if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
                SharedPreferences.Editor spe = sp.edit();
                spe.putBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false);
                spe.putBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false);
                spe.putBoolean(Constants.KEY_ENABLED_KEEP_HOME, false);
                spe.apply();
                ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

                for (ActivityManager.RunningServiceInfo serviceInfo : Objects.requireNonNull(manager).getRunningServices(Integer.MAX_VALUE)) {
                    if (KeepService.class.getName().equals(serviceInfo.service.getClassName())) {
                        KeepService.getInstance().stopService(1);
                        KeepService.getInstance().stopService(2);
                        KeepService.getInstance().stopService(5);
                    }
                }
            }

            try {
                if (Preferences.isEmergencySettingsDchaState(this)) Settings.System.putInt(resolver, Constants.DCHA_STATE, 3);

                if (Preferences.isEmergencySettingsNavigationBar(this)) Settings.System.putInt(resolver, Constants.HIDE_NAVIGATION_BAR, 1);
                return true;
            } catch (SecurityException ignored) {
                return false;
            }
        } else {
            try {
                if (Preferences.isEmergencySettingsDchaState(this)) Settings.System.putInt(resolver, Constants.DCHA_STATE, 0);

                if (Preferences.isEmergencySettingsNavigationBar(this)) Settings.System.putInt(resolver, Constants.HIDE_NAVIGATION_BAR, 0);
                return true;
            } catch (SecurityException ignored) {
                return false;
            }
        }
    }

    private boolean setDchaSettings(String packageName, String className) {
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);

        try {
            startActivity(new Intent().setClassName(packageName, className));
        } catch (Exception e) {
            Toast.toast(this, R.string.toast_not_course);
            setSystemSettings(false);
            return true;
        }

        if (!Preferences.isEmergencySettingsLauncher(this) && !Preferences.isEmergencySettingsRemoveTask(this))
            return false;

        if (!Preferences.GET_DCHASERVICE_FLAG(getApplicationContext())) {
            Toast.toast(getApplicationContext(), R.string.toast_use_not_dcha);
            setSystemSettings(false);
            return true;
        }

        bindService(new Intent(Constants.DCHA_SERVICE).setPackage(Constants.PACKAGE_DCHA_SERVICE), new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                ActivityInfo activityInfo = null;
                IDchaService mDchaService = IDchaService.Stub.asInterface(service);

                if (resolveInfo != null) activityInfo = resolveInfo.activityInfo;

                if (Preferences.isEmergencySettingsLauncher(getApplicationContext())) {
                    try {
                        if (activityInfo != null) {
                            mDchaService.clearDefaultPreferredApp(activityInfo.packageName);
                            mDchaService.setDefaultPreferredHomeApp(packageName);
                        }
                    } catch (RemoteException ignored) {
                        Toast.toast(getApplicationContext(), R.string.toast_not_install_launcher);
                        setSystemSettings(false);
                        finishAndRemoveTask();
                    }
                }

                if (Preferences.isEmergencySettingsRemoveTask(getApplicationContext())) {
                    try {
                        mDchaService.removeTask(null);
                    } catch (RemoteException ignored) {
                    }
                }
                unbindService(this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                unbindService(this);
            }
        }, Context.BIND_AUTO_CREATE);
        return false;
    }
}
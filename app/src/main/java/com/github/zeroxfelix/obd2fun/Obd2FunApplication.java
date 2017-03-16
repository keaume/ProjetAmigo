package com.github.zeroxfelix.obd2fun;

import android.app.Application;
import android.content.Intent;
import android.support.v7.preference.PreferenceManager;

import com.github.zeroxfelix.obd2fun.fragment.SettingsFragment;
import com.github.zeroxfelix.obd2fun.sql.Obd2FunDataSource;
import timber.log.Timber;

public class Obd2FunApplication extends Application {

    private static Obd2FunApplication obd2FunApplication;
    private static Obd2FunDataSource obd2FunDataSource;

    @Override
    public void onCreate() {
        super.onCreate();
        obd2FunApplication = this;
        obd2FunDataSource = new Obd2FunDataSource(this);
        if (getPreferenceBoolean(SettingsFragment.ENABLE_SAVE_STACKTRACE_KEY, false)) {
            Thread.setDefaultUncaughtExceptionHandler(new Obd2FunLogUtility());
        }
        if (getPreferenceBoolean(SettingsFragment.ENABLE_LOGGING_KEY, false)) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    public static String getPreferenceString(String key, String defValue) {
        return PreferenceManager.getDefaultSharedPreferences(obd2FunApplication).getString(key, defValue);
    }

    public static boolean getPreferenceBoolean(String key, Boolean defValue) {
        return PreferenceManager.getDefaultSharedPreferences(obd2FunApplication).getBoolean(key, defValue);
    }

    public static String getResourceString(int resId) {
        return obd2FunApplication.getString(resId);
    }

    public static void sendBroadcastStatic(Intent intent) {
        obd2FunApplication.sendBroadcast(intent);
    }

    public static String getNameForVin(String vin) {
        return obd2FunDataSource.getNameForVin(vin);
    }

}

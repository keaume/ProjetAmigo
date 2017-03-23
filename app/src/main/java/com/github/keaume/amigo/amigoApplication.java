package com.github.keaume.amigo;

import android.app.Application;
import android.content.Intent;
import android.support.v7.preference.PreferenceManager;

import com.github.keaume.amigo.fragment.SettingsFragment;
import com.github.keaume.amigo.sql.AmigoDataSource;
import timber.log.Timber;

public class AmigoApplication extends Application {

    private static AmigoApplication amigoApplication;
    private static AmigoDataSource amigoDataSource;

    @Override
    public void onCreate() {
        super.onCreate();
        amigoApplication = this;
        amigoDataSource = new AmigoDataSource(this);
        if (getPreferenceBoolean(SettingsFragment.ENABLE_SAVE_STACKTRACE_KEY, false)) {
            Thread.setDefaultUncaughtExceptionHandler(new amigoLogUtility());
        }
        if (getPreferenceBoolean(SettingsFragment.ENABLE_LOGGING_KEY, false)) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    public static String getPreferenceString(String key, String defValue) {
        return PreferenceManager.getDefaultSharedPreferences(amigoApplication).getString(key, defValue);
    }

    public static boolean getPreferenceBoolean(String key, Boolean defValue) {
        return PreferenceManager.getDefaultSharedPreferences(amigoApplication).getBoolean(key, defValue);
    }

    public static String getResourceString(int resId) {
        return amigoApplication.getString(resId);
    }

    public static void sendBroadcastStatic(Intent intent) {
        amigoApplication.sendBroadcast(intent);
    }

    public static String getNameForVin(String vin) {
        return amigoDataSource.getNameForVin(vin);
    }

}

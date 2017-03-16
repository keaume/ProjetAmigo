package com.github.zeroxfelix.obd2fun.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.PreferenceManager;

import java.util.HashSet;

import com.github.zeroxfelix.obd2fun.R;
import com.github.zeroxfelix.obd2fun.activity.MainActivity;
import com.github.zeroxfelix.obd2fun.fragment.SettingsFragment;
import com.github.zeroxfelix.obd2fun.obd.ObdBroadcastIntent;
import com.github.zeroxfelix.obd2fun.obd.ObdCommandJob;
import com.github.zeroxfelix.obd2fun.obd.ObdCommandJobResult;
import com.github.zeroxfelix.obd2fun.obd.ObdCommandType;
import com.github.zeroxfelix.obd2fun.obd.ObdConnectionState;
import com.github.zeroxfelix.obd2fun.sql.Obd2FunDataSource;
import timber.log.Timber;

public class ObdDataRecordingService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int NOTIFICATION_ID = 987654321;

    private boolean isObdConnectionActive = false;
    private final IBinder binder = new ObdDataRecordingServiceBinder();

    private LocalBroadcastManager localBroadcastManager;
    private SharedPreferences sharedPrefs;
    private Obd2FunDataSource obd2FunDataSource;

    private final HashSet<ObdCommandType> registeredObdCommandTypes = new HashSet<>();

    private final BroadcastReceiver obdConnectionStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ObdConnectionState obdConnectionState = (ObdConnectionState) intent.getSerializableExtra("obdConnectionState");
            if (obdConnectionState == ObdConnectionState.CONNECTED) {
                setIsObdConnectionActive(true);
            } else {
                setIsObdConnectionActive(false);
            }
        }
    };

    private final BroadcastReceiver obdCommandJobResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d("Received new obdCommandJobResult");
            ObdCommandJobResult obdCommandJobResult = intent.getParcelableExtra("obdCommandJobResult");
            if (obdCommandJobResult.getState() == ObdCommandJob.State.FINISHED) {
                if (!obdCommandJobResult.getRawResult().isEmpty()) {
                    Timber.d("Saving obdCommandJobResult into database");
                    obd2FunDataSource.saveObdCommandJobResult(obdCommandJobResult);
                } else {
                    Timber.e("No data in obdCommandJobResult, discarding");
                }
            } else {
                Timber.e("obdCommandJobResult state was %s%s", obdCommandJobResult.getState().toString(), ", discarding");
            }
        }
    };

    @Override
    public void onCreate() {
        Timber.d("onCreate called");
        super.onCreate();

        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Timber.d("Registering myself as OnSharedPreferenceChangeListener");
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);

        Timber.d("Registering receiver for ObdConnectionState broadcasts");
        localBroadcastManager.registerReceiver(obdConnectionStateReceiver, new IntentFilter(ObdBroadcastIntent.OBD_CONNECTION_STATE));

        Timber.d("Opening database");
        obd2FunDataSource = new Obd2FunDataSource(getApplicationContext());

        registerForObdCommandJobResultBroadcasts();
    }

    @Override
    public void onDestroy() {
        Timber.d("onDestroy called");
        super.onDestroy();

        Timber.d("Leaving foreground");
        stopForeground(true);

        Timber.d("Unregistering myself as OnSharedPreferenceChangeListener");
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);

        Timber.d("Unregistering receiver for ObdConnectionState broadcasts");
        localBroadcastManager.unregisterReceiver(obdConnectionStateReceiver);

        resetObdCommandJobResultBroadcastRegistrations();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Timber.d("onSharedPreferenceChanged called");
        boolean reloadNeeded = false;
        if (key.equals(SettingsFragment.OBD_RECORDING_KEY)) {
            reloadNeeded = true;
        } else {
            for (ObdCommandType obdCommandType : ObdCommandType.values()) {
                if (obdCommandType.getIsRecordable() && key.equals(obdCommandType.getNameForValue())) {
                    reloadNeeded = true;
                    break;
                }
            }
        }
        if (reloadNeeded) {
            Timber.d("OBD recording settings changed, reloading settings");
            resetObdCommandJobResultBroadcastRegistrations();
            registerForObdCommandJobResultBroadcasts();
        }
    }

    public void setIsObdConnectionActive(boolean isObdConnectionActive) {
        Timber.d("Connection state changed to %s", String.valueOf(isObdConnectionActive));
        this.isObdConnectionActive = isObdConnectionActive;
        resetObdCommandJobResultBroadcastRegistrations();
        registerForObdCommandJobResultBroadcasts();
    }

    private Notification buildNotification(String contentText) {
        return new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle(getString(R.string.app_name) + " - " + getString(R.string.obd_recording_service_name))
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0))
                .setWhen(System.currentTimeMillis())
                .setOngoing(true)
                .build();
    }

    private void resetObdCommandJobResultBroadcastRegistrations() {
        Timber.d("Resetting obdCommandJobResult broadcast registrations");
        localBroadcastManager.unregisterReceiver(obdCommandJobResultReceiver);
        for (ObdCommandType obdCommandType : registeredObdCommandTypes) {
            localBroadcastManager.sendBroadcast(ObdBroadcastIntent.getUnregisterPeriodicObdCommandJobIntent(obdCommandType));
        }
        registeredObdCommandTypes.clear();
    }

    private void registerForObdCommandJobResultBroadcasts() {
        if (isObdConnectionActive && sharedPrefs.getBoolean(SettingsFragment.OBD_RECORDING_KEY, false)) {
            for (ObdCommandType obdCommandType : ObdCommandType.values()) {
                if (obdCommandType.getIsRecordable() && sharedPrefs.getBoolean(obdCommandType.getNameForValue(), false)) {
                    Timber.d("Registering for obdCommand: %s", obdCommandType.getNameForValue());
                    localBroadcastManager.registerReceiver(obdCommandJobResultReceiver, new IntentFilter(obdCommandType.getNameForValue()));
                    localBroadcastManager.sendBroadcast(ObdBroadcastIntent.getRegisterPeriodicObdCommandJobIntent(obdCommandType));
                    registeredObdCommandTypes.add(obdCommandType);
                }
            }
            if (!registeredObdCommandTypes.isEmpty()) {
                Timber.d("Entering foreground");
                startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.obd_recording_active)));
            } else {
                Timber.d("Recording obd commands list was empty, not registering for any obdCommandJobResult broadcasts, leaving foreground");
                stopForeground(true);
            }
        } else {
            Timber.d("Recording deactivated or connection not active, not registering for any obdCommandJobResult broadcasts, leaving foreground");
            stopForeground(true);
        }
    }

    public class ObdDataRecordingServiceBinder extends Binder {
        public ObdDataRecordingService getService() {
            return ObdDataRecordingService.this;
        }
    }
}

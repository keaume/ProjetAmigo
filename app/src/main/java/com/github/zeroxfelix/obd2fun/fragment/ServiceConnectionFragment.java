package com.github.zeroxfelix.obd2fun.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;

import com.github.zeroxfelix.obd2fun.Obd2FunApplication;
import com.github.zeroxfelix.obd2fun.service.AbstractObdService;
import com.github.zeroxfelix.obd2fun.service.BluetoothObdService;
import com.github.zeroxfelix.obd2fun.service.ObdDataRecordingService;
import timber.log.Timber;

public class ServiceConnectionFragment extends Fragment {

    private Context applicationContext;

    private AbstractObdService obdConnectionService;
    private final ServiceConnection obdConnectionServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Timber.d(className.toString(), " service is bound");
            obdConnectionService = ((AbstractObdService.AbstractObdServiceBinder) binder).getService();
            if (Obd2FunApplication.getPreferenceBoolean(SettingsFragment.CONNECT_ON_STARTUP_KEY, false)) {
                obdConnectionService.startObdConnection();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Timber.d(className.toString(), " service is unbound");
            obdConnectionService = null;
        }
    };

    private ObdDataRecordingService obdDataRecordingService;
    private final ServiceConnection obdDataRecordingServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Timber.d(className.toString(), " service is bound");
            obdDataRecordingService = ((ObdDataRecordingService.ObdDataRecordingServiceBinder) binder).getService();
            obdDataRecordingService.setIsObdConnectionActive(isObdConnectionActive());
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Timber.d(className.toString(), " service is unbound");
            obdDataRecordingService = null;
            bindObdDataRecordingService();
        }
    };

    @Override
    public void onAttach(Context context) {
        Timber.d("onAttach called");
        super.onAttach(context);
        if (applicationContext == null) {
            applicationContext = context.getApplicationContext();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate called");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        bindObdConnectionService();
        bindObdDataRecordingService();
    }

    @Override
    public void onDestroy() {
        Timber.d("onDestroy called");
        super.onDestroy();
        if (obdDataRecordingService != null) {
            unbindObdDataRecordingService();
        }
        if (obdConnectionService != null) {
            unbindObdConnectionService();
        }
    }

    public void startObdConnection() {
        if (obdConnectionService != null) {
            obdConnectionService.startObdConnection();
        }
    }

    public void stopObdConnection() {
        if (obdConnectionService != null) {
            obdConnectionService.stopObdConnection(false);
        }
    }

    public boolean isObdConnectionActive() {
        return obdConnectionService != null && obdConnectionService.isConnectionActive();
    }

    public long getCurrentSessionId() {
        if (obdConnectionService != null) {
            return obdConnectionService.getCurrentSessionId();
        } else {
            return 0L;
        }
    }

    public String getCurrentVin() {
        if (obdConnectionService != null) {
            return obdConnectionService.getCurrentVIN();
        } else {
            return "";
        }
    }

    private void bindObdConnectionService() {
        if (obdConnectionService == null) {
            Timber.d("Binding OBD connection service");

            Intent obdConnectionServiceIntent;
            // String selectedInterfaceType = sharedPrefs.getString(SettingsFragment.OBD_INTERFACE_TYPE_KEY, getString(R.string.interface_type_preference_default));

            // For now only BT interfaces, USB maybe in the future
            // if (selectedInterfaceType == getString(R.string.interface_type_preference_default)) {
            //     obdConnectionServiceIntent = new Intent(this, BluetoothObdService.class);
            // } else if (...) {
            //     ...
            // }

            // Use me for local testing...
            //obdConnectionServiceIntent = new Intent(this, MockObdService.class);

            obdConnectionServiceIntent = new Intent(applicationContext, BluetoothObdService.class);
            applicationContext.bindService(obdConnectionServiceIntent, obdConnectionServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindObdConnectionService() {
        if (obdConnectionService != null) {
            Timber.d("Unbinding OBD connection service");
            applicationContext.unbindService(obdConnectionServiceConnection);
            obdConnectionService = null;
        }
    }

    private void bindObdDataRecordingService() {
        if (obdDataRecordingService == null) {
            Timber.d("Binding OBD data recording service");
            Intent obdDataRecordingServiceIntent;
            obdDataRecordingServiceIntent = new Intent(applicationContext, ObdDataRecordingService.class);
            applicationContext.bindService(obdDataRecordingServiceIntent, obdDataRecordingServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindObdDataRecordingService() {
        if (obdDataRecordingService != null) {
            Timber.d("Unbinding OBD data recording service");
            applicationContext.getApplicationContext().unbindService(obdDataRecordingServiceConnection);
            obdDataRecordingService = null;
        }
    }
}

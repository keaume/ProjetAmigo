package com.github.zeroxfelix.obd2fun.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import java.io.IOException;
import java.util.UUID;

import com.github.zeroxfelix.obd2fun.fragment.SettingsFragment;
import com.github.zeroxfelix.obd2fun.obd.ObdConnectionState;
import timber.log.Timber;

public class BluetoothObdService extends AbstractObdService {

    private final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter btAdapter;
    private BluetoothDevice btDevice;
    private BluetoothSocket btSocket;
    private String btDeviceString;

    private final BroadcastReceiver btDisconnectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (isConnectionActive() && BluetoothObdService.this.btDevice.equals(btDevice)) {
                Timber.w("Bluetooth connection to device lost, stopping obd connection");
                stopObdConnection(true);
                sendObdConnectionStateBroadcast(ObdConnectionState.CONNECTION_LOST);
            }
        }
    };

    @Override
    public void onCreate() {
        Timber.d("onCreate called");
        super.onCreate();

        Timber.d("Registering receiver for bluetooth disconnect broadcasts");
        IntentFilter btDisconnectIntentFilter = new IntentFilter();
        btDisconnectIntentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        btDisconnectIntentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(btDisconnectReceiver, btDisconnectIntentFilter);
    }

    @Override
    public void onDestroy() {
        Timber.d("onDestroy called");
        super.onDestroy();

        Timber.d("Unregistering receiver for bluetooth disconnect broadcasts");
        unregisterReceiver(btDisconnectReceiver);
    }

    @Override
    protected ObdConnectionState _startObdConnection() {
        Timber.d("_startObdConnection called");

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        btDeviceString = sharedPrefs.getString(SettingsFragment.BLUETOOTH_DEVICE_KEY, null);

        if (btAdapter == null) {
            Timber.e("Bluetooth is not supported on this device, aborting connecting");
            return ObdConnectionState.CONNECTING_FAILED_BT_NOT_SUPPORTED;
        }

        if (!btAdapter.isEnabled()) {
            Timber.e("Bluetooth adapter is not enabled, aborting connecting");
            return ObdConnectionState.CONNECTING_FAILED_BT_IS_DISABLED;
        }

        if (btDeviceString == null || btDeviceString.equals("")) {
            Timber.e("No paired Bluetooth device has been selected, aborting connecting");
            return ObdConnectionState.CONNECTING_FAILED_BT_NO_DEVICE_SELECTED;
        }

        try {
            startBluetoothObdConnection();
            setObdConnectionInputStream(btSocket.getInputStream());
            setObdConnectionOutputStream(btSocket.getOutputStream());
        } catch (Exception e) {
            return ObdConnectionState.CONNECTING_FAILED;
        }

        return ObdConnectionState.CONNECTED;
    }

    @Override
    protected ObdConnectionState _stopObdConnection() {
        Timber.d("_stopObdConnection called");
        setObdConnectionInputStream(null);
        setObdConnectionOutputStream(null);
        if (btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException e) {
                Timber.e(e, "Failed to close socket");
            }
        }
        btSocket = null;
        btAdapter = null;
        btDevice = null;
        btDeviceString = null;
        return ObdConnectionState.DISCONNECTED;
    }

    @Override
    protected boolean isSocketConnected() {
        return btSocket != null && btSocket.isConnected();
    }

    private void startBluetoothObdConnection() throws IOException {
        Timber.d("Starting OBD connection");
        btDevice = btAdapter.getRemoteDevice(btDeviceString);

        Timber.d("Stopping Bluetooth discovery");
        btAdapter.cancelDiscovery();

        try {
            connectToBtDevice();
            Timber.d("Bluetooth connection successfully started");
        } catch (Exception e) {
            throw new IOException();
        }
    }

    // Taken from http://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3/18786701#18786701
    private void connectToBtDevice() throws IOException {
        Timber.d("Starting Bluetooth connection");
        try {
            btSocket = btDevice.createRfcommSocketToServiceRecord(BT_UUID);
            btSocket.connect();
        } catch (Exception e1) {
            Timber.e(e1, "There was an error while starting the Bluetooth connection, trying to fallback");
            try {
                btSocket = (BluetoothSocket) btDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(btDevice, 1);
                btSocket.connect();
            } catch (Exception e2) {
                Timber.e(e2, "Could not fallback while starting the Bluetooth connection, giving up");
                throw new IOException();
            }
        }
    }

}
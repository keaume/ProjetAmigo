package com.github.zeroxfelix.obd2fun.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.github.zeroxfelix.obd2fun.obd.ObdConnectionState;
import timber.log.Timber;

public class MockObdService extends AbstractObdService {

    @Override
    protected ObdConnectionState _startObdConnection() {
        Timber.d("_startObdConnection called");
        setObdConnectionInputStream(new ByteArrayInputStream("41 00 00 00>41 00 00 00>41 00 00 00>".getBytes()));
        setObdConnectionOutputStream(new ByteArrayOutputStream());
        return ObdConnectionState.CONNECTED;
    }

    @Override
    protected ObdConnectionState _stopObdConnection() {
        Timber.d("_stopObdConnection called");
        setObdConnectionInputStream(null);
        setObdConnectionOutputStream(null);
        return ObdConnectionState.DISCONNECTED;
    }

    @Override
    protected boolean isSocketConnected() {
        return true;
    }
}

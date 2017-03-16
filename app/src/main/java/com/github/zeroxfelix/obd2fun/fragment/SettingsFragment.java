package com.github.zeroxfelix.obd2fun.fragment;

import android.app.backup.BackupManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompatFix;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.pires.obd.enums.ObdProtocols;

import java.util.ArrayList;
import java.util.Set;

import com.github.zeroxfelix.obd2fun.Obd2FunApplication;
import com.github.zeroxfelix.obd2fun.Obd2FunLogUtility;
import com.github.zeroxfelix.obd2fun.R;
import com.github.zeroxfelix.obd2fun.interfaces.SetActionBarTitleInterface;
import com.github.zeroxfelix.obd2fun.interfaces.SetDrawerStateInterface;
import com.github.zeroxfelix.obd2fun.interfaces.SetSelectedDrawerMenuItemInterface;
import com.github.zeroxfelix.obd2fun.obd.ObdCommandType;
import timber.log.Timber;

public class SettingsFragment extends PreferenceFragmentCompatFix implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String CONNECT_ON_STARTUP_KEY = "connect_on_startup_preference";
    public static final String OBD_INTERFACE_TYPE_KEY = "interface_type_preference";
    public static final String BLUETOOTH_DEVICE_KEY = "bluetooth_device_preference";
    public static final String OBD_PROTOCOL_KEY = "obd_protocol_preference";
    public static final String OBD_UPDATE_DELAY_KEY = "obd_update_delay_preference";
    public static final String IMPERIAL_UNITS_KEY = "imperial_units_preference";
    public static final String OBD_RECORDING_KEY = "obd_recording_preference";
    public static final String OBD_RECORDING_COMMANDS_KEY = "obd_recording_commands_preference";
    public static final String ENABLE_LOGGING_KEY = "enable_logging_preference";
    public static final String ENABLE_SAVE_STACKTRACE_KEY = "enable_save_stacktrace_preference";

    private SharedPreferences sharedPrefs;
    private BluetoothAdapter btAdapter;
    private ListPreference listInterfaceTypes;
    private ListPreference listBluetoothDevices;
    private ListPreference listObdProtocols;
    private EditTextPreference textObdUpdateDelay;
    private PreferenceScreen listObdRecordingCommands;

    private final BroadcastReceiver btAdapterStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
                    if (listInterfaceTypes != null && listBluetoothDevices != null && listObdProtocols != null && textObdUpdateDelay != null) {
                        populateBluetoothDeviceList();
                        populateObdProtocolsList();
                        setSummaries();
                    }
                }
            }
        }
    };

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        Timber.d("onCreatePreferences called");
        setPreferencesFromResource(R.xml.preferences, s);
        setHasOptionsMenu(true);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        listInterfaceTypes = (ListPreference) getPreferenceScreen().findPreference(OBD_INTERFACE_TYPE_KEY);
        listBluetoothDevices = (ListPreference) getPreferenceScreen().findPreference(BLUETOOTH_DEVICE_KEY);
        listObdProtocols = (ListPreference) getPreferenceScreen().findPreference(OBD_PROTOCOL_KEY);
        textObdUpdateDelay = (EditTextPreference) getPreferenceScreen().findPreference(OBD_UPDATE_DELAY_KEY);
        listObdRecordingCommands = (PreferenceScreen) getPreferenceScreen().findPreference(OBD_RECORDING_COMMANDS_KEY);

        if (listInterfaceTypes != null && listBluetoothDevices != null && listObdProtocols != null && textObdUpdateDelay != null) {
            btAdapter = BluetoothAdapter.getDefaultAdapter();
            setupPreferences();
            populateBluetoothDeviceList();
            populateObdProtocolsList();
            setSummaries();
        }

        if (listObdRecordingCommands != null) {
            populateObdRecordingCommandsList();
        }

        Timber.d("Registering receiver for bluetooth adapter state changed broadcasts");
        IntentFilter btAdapterStateChangedIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(btAdapterStateChangedReceiver, btAdapterStateChangedIntentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("Unregistering receiver for bluetooth adapter state changed broadcasts");
        getActivity().unregisterReceiver(btAdapterStateChangedReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Timber.d("onCreateOptionsMenu called");
        inflater.inflate(R.menu.settings_fragment_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Timber.d("onOptionsItemSelected called");
        switch (item.getItemId()) {
            case R.id.request_backup:
                requestBackup();
                return true;
            case R.id.save_log_to_file:
                Obd2FunLogUtility.saveLogToExternalStorage();
                return true;
            case R.id.about_the_app:
                openAboutDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        Timber.d("onResume called");
        super.onResume();
        // Set selected menuItem in navigation drawer
        ((SetSelectedDrawerMenuItemInterface)getActivity()).setSelectedDrawerMenuItem(R.id.main_activity_drawer_menu_settings);
        // Set title and toolbar icon
        if (listInterfaceTypes != null && listBluetoothDevices != null && listObdProtocols != null && textObdUpdateDelay != null) {
            ((SetActionBarTitleInterface)getActivity()).setActionBarTitle(getString(R.string.main_activity_drawer_menu_settings));
            ((SetDrawerStateInterface)getActivity()).setDrawerState(true);
            populateBluetoothDeviceList();
            populateObdProtocolsList();
            setSummaries();
        } else {
            ((SetActionBarTitleInterface)getActivity()).setActionBarTitle(getString(R.string.obd_recording_category_title));
            ((SetDrawerStateInterface)getActivity()).setDrawerState(false);
        }
        Timber.d("Registering myself as OnSharedPreferenceChangeListener");
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        Timber.d("onPause called");
        super.onPause();
        Timber.d("Unregistering myself as OnSharedPreferenceChangeListener");
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Timber.d("onSharedPreferenceChanged called");
        requestBackup();
    }

    private void requestBackup() {
        Timber.d("Requesting backup");
        BackupManager bm = new BackupManager(getActivity().getApplicationContext());
        bm.dataChanged();
    }

    private void makeToast(String text) {
        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
    }

    private void setupPreferences() {
        listBluetoothDevices.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (btAdapter == null) {
                    makeToast(getString(R.string.bluetooth_not_supported));
                    return false;
                }
                if (!btAdapter.isEnabled()) {
                    makeToast(getString(R.string.bluetooth_disabled));
                    return false;
                }
                return true;
            }
        });

        String[] preferencesKeys = new String[] {OBD_INTERFACE_TYPE_KEY, BLUETOOTH_DEVICE_KEY, OBD_PROTOCOL_KEY, OBD_UPDATE_DELAY_KEY};
        for (final String preferenceKey : preferencesKeys) {
            Preference preference = getPreferenceScreen().findPreference(preferenceKey);
            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (OBD_UPDATE_DELAY_KEY.equals(preference.getKey())) {
                        double newObdUpdateDelay;
                        try {
                            newObdUpdateDelay = Double.parseDouble(newValue.toString().replace(",", "."));
                        } catch (Exception e) {
                            makeToast(String.format(getString(R.string.could_not_parse_as_double), newValue.toString()));
                            return false;
                        }
                        if (newObdUpdateDelay <= 0) {
                            makeToast(String.format(getString(R.string.could_not_parse_as_double), newValue.toString()));
                            return false;
                        }
                    }
                    setNewSummary(preference, newValue);
                    return true;
                }
            });
        }
    }

    private void setNewSummary(Preference preference, Object newValue) {
        switch(preference.getKey()) {
            case OBD_INTERFACE_TYPE_KEY:
            case OBD_PROTOCOL_KEY:
                preference.setSummary(newValue.toString());
                break;
            case BLUETOOTH_DEVICE_KEY:
                preference.setSummary(getPairedDeviceNameFromAddress(newValue.toString()) + " / " + newValue.toString());
                break;
            case OBD_UPDATE_DELAY_KEY:
                preference.setSummary(String.format(getString(R.string.obd_update_delay_preference_summary), newValue.toString()));
                break;
            default:
                break;
        }
    }

    private void populateBluetoothDeviceList() {
        ArrayList<CharSequence> pairedBtDeviceNameStrings = new ArrayList<>();
        ArrayList<CharSequence> pairedBtDeviceAddressStrings = new ArrayList<>();

        if (btAdapter == null) {
            listBluetoothDevices.setEntries(pairedBtDeviceNameStrings.toArray(new CharSequence[pairedBtDeviceNameStrings.size()]));
            listBluetoothDevices.setEntryValues(pairedBtDeviceAddressStrings.toArray(new CharSequence[pairedBtDeviceAddressStrings.size()]));
            makeToast(getString(R.string.bluetooth_not_supported));
            return;
        }

        Set<BluetoothDevice> pairedBtDevices = btAdapter.getBondedDevices();

        if (pairedBtDevices.size() > 0) {
            for (BluetoothDevice btDevice : pairedBtDevices) {
                pairedBtDeviceNameStrings.add(btDevice.getName() + " / " + btDevice.getAddress());
                pairedBtDeviceAddressStrings.add(btDevice.getAddress());
            }
        }

        listBluetoothDevices.setEntries(pairedBtDeviceNameStrings.toArray(new CharSequence[pairedBtDeviceNameStrings.size()]));
        listBluetoothDevices.setEntryValues(pairedBtDeviceAddressStrings.toArray(new CharSequence[pairedBtDeviceAddressStrings.size()]));
    }

    private void populateObdProtocolsList() {
        ArrayList<CharSequence> protocolStrings = new ArrayList<>();

        for (ObdProtocols protocol : ObdProtocols.values()) {
            protocolStrings.add(protocol.name());
        }

        listObdProtocols.setEntries(protocolStrings.toArray(new CharSequence[protocolStrings.size()]));
        listObdProtocols.setEntryValues(protocolStrings.toArray(new CharSequence[protocolStrings.size()]));
    }

    private void populateObdRecordingCommandsList() {
        for (ObdCommandType obdCommandType : ObdCommandType.values()) {
            if (obdCommandType.getIsRecordable()) {
                CheckBoxPreference checkBoxPreference = new CheckBoxPreference(getPreferenceManager().getContext());
                checkBoxPreference.setTitle(obdCommandType.getNameForValue());
                checkBoxPreference.setKey(obdCommandType.getNameForValue());
                checkBoxPreference.setChecked(sharedPrefs.getBoolean(obdCommandType.getNameForValue(), false));
                listObdRecordingCommands.addPreference(checkBoxPreference);
            }
        }
    }

    private String getPairedDeviceNameFromAddress(String pairedBtDeviceAddress) {
        if (btAdapter != null) {
            Set<BluetoothDevice> pairedBtDevices = btAdapter.getBondedDevices();
            for (BluetoothDevice btDevice : pairedBtDevices) {
                if (btDevice.getAddress().equals(pairedBtDeviceAddress)) {
                    return btDevice.getName();
                }
            }
        }
        return pairedBtDeviceAddress;
    }

    private void setSummaries() {
        String selectedDeviceType = sharedPrefs.getString(listInterfaceTypes.getKey(), getString(R.string.interface_type_preference_default));
        String selectedBtDeviceAddress = sharedPrefs.getString(listBluetoothDevices.getKey(), getString(R.string.bluetooth_device_preference_default));
        String selectedObdProtocol = sharedPrefs.getString(listObdProtocols.getKey(), getString(R.string.obd_protocol_preference_default));
        String selectedObdUpdateFrequency = sharedPrefs.getString(textObdUpdateDelay.getKey(), getString(R.string.obd_update_delay_preference_default));

        String pairedDeviceName = getPairedDeviceNameFromAddress(selectedBtDeviceAddress);
        if (pairedDeviceName.equals(selectedBtDeviceAddress)) {
            SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
            sharedPrefsEditor.remove(listBluetoothDevices.getKey());
            sharedPrefsEditor.apply();
            listBluetoothDevices.setSummary(getString(R.string.bluetooth_device_preference_default));
        } else {
            listBluetoothDevices.setSummary(pairedDeviceName + " / " + selectedBtDeviceAddress);
        }

        listInterfaceTypes.setSummary(selectedDeviceType);
        listObdProtocols.setSummary(selectedObdProtocol);
        textObdUpdateDelay.setSummary(String.format(getString(R.string.obd_update_delay_preference_summary), selectedObdUpdateFrequency));
    }

    public static int getObdCommandJobDelay() {
        String defaultObdCommandJobDelayString = Obd2FunApplication.getResourceString(R.string.obd_update_delay_preference_default);
        String obdCommandJobDelayString = Obd2FunApplication.getPreferenceString(OBD_UPDATE_DELAY_KEY, defaultObdCommandJobDelayString);
        int obdCommandJobDelay;
        try {
            obdCommandJobDelay = (int) (Double.parseDouble(obdCommandJobDelayString) * 1000);
        } catch (Exception e) {
            Timber.d("Failed to parse obdCommandJobDelay, reverting to default");
            obdCommandJobDelay = (int) (Double.parseDouble(defaultObdCommandJobDelayString) * 1000);
        }
        if (obdCommandJobDelay <= 0) {
            Timber.d("obdCommandJobDelay is zero or below, not possible, reverting to default");
            obdCommandJobDelay = (int) (Double.parseDouble(defaultObdCommandJobDelayString) * 1000);
        }
        return obdCommandJobDelay;
    }

    private void openAboutDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.settings_fragment_menu_about_the_app))
                .setMessage(getString(R.string.about_the_app))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Do nothing
                    }
                })
                .setIconAttribute(android.R.attr.dialogIcon)
                .show();
    }
}

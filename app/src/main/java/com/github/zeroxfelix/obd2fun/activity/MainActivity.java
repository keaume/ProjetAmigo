package com.github.zeroxfelix.obd2fun.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.zeroxfelix.obd2fun.Obd2FunApplication;
import com.github.zeroxfelix.obd2fun.R;
import com.github.zeroxfelix.obd2fun.fragment.AnalyzeDataFragment;
import com.github.zeroxfelix.obd2fun.fragment.ManageVehiclesFragment;
import com.github.zeroxfelix.obd2fun.fragment.ReadDataFragment;
import com.github.zeroxfelix.obd2fun.fragment.ServiceConnectionFragment;
import com.github.zeroxfelix.obd2fun.fragment.SettingsFragment;
import com.github.zeroxfelix.obd2fun.fragment.TroubleCodesFragment;
import com.github.zeroxfelix.obd2fun.interfaces.GetCurrentSessionIdInterface;
import com.github.zeroxfelix.obd2fun.interfaces.GetCurrentVinInterface;
import com.github.zeroxfelix.obd2fun.interfaces.GetIsObdConnectionActiveInterface;
import com.github.zeroxfelix.obd2fun.interfaces.SetActionBarTitleInterface;
import com.github.zeroxfelix.obd2fun.interfaces.SetDrawerStateInterface;
import com.github.zeroxfelix.obd2fun.interfaces.SetSelectedDrawerMenuItemInterface;
import com.github.zeroxfelix.obd2fun.obd.ObdBroadcastIntent;
import com.github.zeroxfelix.obd2fun.obd.ObdConnectionState;
import com.github.zeroxfelix.obd2fun.sql.Obd2FunDataSource;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback, SetActionBarTitleInterface, SetSelectedDrawerMenuItemInterface, SetDrawerStateInterface, GetIsObdConnectionActiveInterface, GetCurrentVinInterface, GetCurrentSessionIdInterface {
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private NavigationView nvDrawer;
    private ActionBarDrawerToggle drawerToggle;
    private SwitchCompat connectionSwitch;
    private TextView statusVin;
    private TextView statusVehicle;
    private TextView statusObd;
    private ImageView statusDot;
    private String vin;
    private boolean vehiclePopupShowedThisSession = false;

    private LocalBroadcastManager localBroadcastManager;
    private FragmentManager fragmentManager;
    private ServiceConnectionFragment serviceConnectionFragment;

    private final CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            onConnectionSwitchClicked(isChecked);
        }
    };

    private final BroadcastReceiver obdConnectionStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ObdConnectionState obdConnectionState = (ObdConnectionState) intent.getSerializableExtra("obdConnectionState");
            switch (obdConnectionState) {
                case DISCONNECTED:
                    makeToast(getString(R.string.obd_disconnected));
                    break;
                case CONNECTED:
                    makeToast(getString(R.string.obd_connected));
                    break;
                case CONNECTING_FAILED:
                    makeToast(getString(R.string.obd_connecting_failed));
                    break;
                case CONNECTING_FAILED_BT_NOT_SUPPORTED:
                    makeToast(getString(R.string.bluetooth_not_supported));
                    break;
                case CONNECTING_FAILED_BT_IS_DISABLED:
                    makeToast(getString(R.string.bluetooth_disabled));
                    break;
                case CONNECTING_FAILED_BT_NO_DEVICE_SELECTED:
                    makeToast(getString(R.string.bluetooth_no_selected_device));
                    break;
                case CONNECTION_LOST:
                    makeToast(getString(R.string.obd_connection_lost));
                    break;
                default:
            }
            setConnectionSwitchWithoutTriggeringListener();
            setStatusInformation();
            if (obdConnectionState != ObdConnectionState.CONNECTED) {
                Timber.d("Resetting vehiclePopup state");
                vehiclePopupShowedThisSession = false;
                Timber.d("Resetting vin");
                vin = null;
                Timber.d("Registering vinReceivedReceiver");
                localBroadcastManager.registerReceiver(vinReceivedReceiver, new IntentFilter(ObdBroadcastIntent.VIN_CHANGED));
                ReadDataFragment readDataFragment = (ReadDataFragment) fragmentManager.findFragmentByTag(ReadDataFragment.class.getSimpleName());
                readDataFragment.clearWidgetList();
            }
        }
    };

    private final BroadcastReceiver vinReceivedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            vin = intent.getStringExtra("newVin");
            Timber.d("VIN Received");
            Timber.d("Unregistering vinReceivedReceiver");
            localBroadcastManager.unregisterReceiver(vinReceivedReceiver);
            if(!vehiclePopupShowedThisSession){
                vehiclePopup(vin);
                vehiclePopupShowedThisSession = true;
            }
            setStatusInformation();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        fragmentManager = getSupportFragmentManager();
        Timber.d("Getting instance of serviceConnectionFragment");
        serviceConnectionFragment = (ServiceConnectionFragment) fragmentManager.findFragmentByTag(ServiceConnectionFragment.class.getSimpleName());
        if (serviceConnectionFragment == null) {
            Timber.d("No instance of serviceConnectionFragment available, instantiating a new one");
            serviceConnectionFragment = new ServiceConnectionFragment();
            fragmentManager.beginTransaction().add(serviceConnectionFragment, ServiceConnectionFragment.class.getSimpleName()).commit();
        }

        Timber.d("Registering receiver for ObdConnectionState broadcasts");
        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastManager.registerReceiver(obdConnectionStateReceiver, new IntentFilter(ObdBroadcastIntent.OBD_CONNECTION_STATE));

        Timber.d("Registering receiver for vinReceivedReceiver");
        localBroadcastManager.registerReceiver(vinReceivedReceiver, new IntentFilter(ObdBroadcastIntent.VIN_CHANGED));

        Timber.d("Setting up UI");
        toolbar = (Toolbar) findViewById(R.id.main_activity_toolbar);
        drawer = (DrawerLayout) findViewById(R.id.main_activity_drawer_layout);
        nvDrawer = (NavigationView) findViewById(R.id.main_activity_navigation_view);
        View headerView = nvDrawer.getHeaderView(0);
        statusObd = (TextView) headerView.findViewById(R.id.status_obd);
        statusVehicle = (TextView) headerView.findViewById(R.id.status_vehicle);
        statusVin = (TextView) headerView.findViewById(R.id.status_vin);
        statusDot = (ImageView) headerView.findViewById(R.id.status_dot);
        setSupportActionBar(toolbar);
        setupDrawerContent();
        drawerToggle = setupDrawerToggle();
        drawer.setDrawerListener(drawerToggle);
        Menu menu = nvDrawer.getMenu();
        MenuItem menuItem = menu.findItem(R.id.main_activity_drawer_menu_manage_connection);
        View actionView = MenuItemCompat.getActionView(menuItem);
        connectionSwitch = (SwitchCompat) actionView.findViewById(R.id.connection_switch);
        connectionSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
        setConnectionSwitchWithoutTriggeringListener();

        if (savedInstanceState == null) {
            Timber.d("Cold start, opening default fragment");
            selectDrawerItem(null);
            vin = null;
        }
        else{

            vin = savedInstanceState.getString("vin");
        }

        setStatusInformation();
    }

    @Override
    protected void onDestroy() {
        Timber.d("onDestroy called");
        super.onDestroy();
        Timber.d("Unregistering receiver for ObdConnectionState broadcasts");
        localBroadcastManager.unregisterReceiver(obdConnectionStateReceiver);
        Timber.d("Unregistering receiver for VinReceived broadcasts");
        localBroadcastManager.unregisterReceiver(vinReceivedReceiver);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        Timber.d("onPostCreate called");
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Timber.d("onConfigurationChanged called");
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        Timber.d("onBackPressed called");
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if(fragmentManager.getBackStackEntryCount() != 0) {
                fragmentManager.popBackStack();
            } else {
                showReallyQuitAlertDialog();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Timber.d("onOptionsItemSelected called");
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {
        Timber.d("onPreferenceStartScreen called");
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
        SettingsFragment settingsFragment = new SettingsFragment();
        settingsFragment.setArguments(args);
        fragmentManager.beginTransaction().replace(R.id.main_activity_content, settingsFragment, preferenceScreen.getKey()).addToBackStack(null).commit();
        return true;
    }

    @Override
    public void setSelectedDrawerMenuItem(Integer menuItemId) {
        Timber.d("setSelectedDrawerMenuItem called");
        nvDrawer.getMenu().findItem(menuItemId).setChecked(true);
    }

    @Override
    public void setActionBarTitle(String title) {
        Timber.d("setActionBarTitle called");
        toolbar.setTitle(title);
    }

    @Override
    public void setDrawerState(boolean enabled) {
        Timber.d("setDrawerState called");
        if (enabled) {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            drawerToggle.setDrawerIndicatorEnabled(true);
            drawerToggle.syncState();
        } else {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            drawerToggle.setDrawerIndicatorEnabled(false);
            drawerToggle.syncState();
        }
    }

    @Override
    public boolean isObdConnectionActive() {
        return serviceConnectionFragment.isObdConnectionActive();
    }

    @Override
    public String getCurrentVin() {
        return serviceConnectionFragment.getCurrentVin();
    }

    @Override
    public long getCurrentSessionId() {
        return serviceConnectionFragment.getCurrentSessionId();
    }

    private void setupDrawerContent() {
        nvDrawer.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        if(menuItem.getItemId() != R.id.main_activity_drawer_menu_manage_connection) {
                            selectDrawerItem(menuItem);
                        }
                        return true;
                    }
                });
    }

    private void onConnectionSwitchClicked(boolean isChecked){
        Timber.d("Connection state changed");
        if (isChecked) {
            serviceConnectionFragment.startObdConnection();
            makeToast(getString(R.string.obd_connecting));
        } else {
            serviceConnectionFragment.stopObdConnection();
        }
    }

    private void setConnectionSwitchWithoutTriggeringListener(){
        //Hack to change switch state without triggering change listener
        connectionSwitch.setOnCheckedChangeListener(null);
        connectionSwitch.setChecked(isObdConnectionActive());
        connectionSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    private void selectDrawerItem(MenuItem menuItem) {
        Timber.d("Changing the displayed fragment");
        Class<? extends Fragment> fragmentClass;
        Fragment fragment;

        if (menuItem != null) {
            switch(menuItem.getItemId()) {
                case R.id.main_activity_drawer_menu_analyze_data:
                    fragmentClass = AnalyzeDataFragment.class;
                    break;
                case R.id.main_activity_drawer_menu_trouble_codes:
                    fragmentClass = TroubleCodesFragment.class;
                    break;
                case R.id.main_activity_drawer_menu_settings:
                    fragmentClass = SettingsFragment.class;
                    break;
                case R.id.main_activity_drawer_menu_manage_vehicles:
                    fragmentClass = ManageVehiclesFragment.class;
                    break;
                case R.id.main_activity_drawer_menu_read_data:
                default:
                    fragmentClass = ReadDataFragment.class;
            }
        } else {
            fragmentClass = ReadDataFragment.class;
        }

        try {
            fragment = fragmentManager.findFragmentByTag(fragmentClass.getSimpleName());
            if (fragment == null) {
                fragment = fragmentClass.newInstance();
            }
        } catch (Exception e) {
            Timber.e(e, "Error while getting/instantiating a new fragment to display");
            return;
        }

        if (menuItem == null) {
            fragmentManager.beginTransaction().replace(R.id.main_activity_content, fragment, fragmentClass.getSimpleName()).commit();
        } else {
            fragmentManager.beginTransaction().replace(R.id.main_activity_content, fragment, fragmentClass.getSimpleName()).addToBackStack(null).commit();
        }

        drawer.closeDrawer(GravityCompat.START);
    }

    private ActionBarDrawerToggle setupDrawerToggle() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        return new ActionBarDrawerToggle(this, drawer, R.string.main_activity_drawer_open,  R.string.main_activity_drawer_close);
    }

    private void makeToast(String text) {
        Timber.d("Making a toast");
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    private void showReallyQuitAlertDialog() {
        Timber.d("Showing really quit dialog");
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.alert_dialog_quit_app_title))
                .setMessage(getString(R.string.alert_dialog_quit_app_message))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .show();
    }

    private void setStatusInformation(){
        if (isObdConnectionActive()) {
            statusDot.setImageResource(R.drawable.ic_dot_green);
            statusObd.setText(getString(R.string.navigation_drawer_connection_status_obd_connected));
        } else {
            statusDot.setImageResource(R.drawable.ic_dot_red);
            statusObd.setText(getString(R.string.navigation_drawer_connection_status_obd_not_connected));
            statusVin.setText(getString(R.string.navigation_drawer_connection_status_no_vin));
            statusVehicle.setText(getString(R.string.navigation_drawer_connection_status_no_vehicle));
        }
        if (vin != null && isObdConnectionActive()) {
            statusVin.setText(vin);
            if(Obd2FunApplication.getNameForVin(vin) != null){
                statusVehicle.setText(Obd2FunApplication.getNameForVin(vin));
            }
            else{
                statusVehicle.setText(getString(R.string.vehicle_popup_new_no_name));
            }
        } else {
            statusVin.setText(getString(R.string.navigation_drawer_connection_status_no_vin));
            statusVehicle.setText(getString(R.string.navigation_drawer_connection_status_no_vehicle));
        }
    }

    private void vehiclePopup(final String vin){
        final Obd2FunDataSource dataSource = new Obd2FunDataSource(this);
        if (dataSource.getNameForVin(vin) == null) {
            Timber.d("Unknown VIN: '%s', showing vehiclePopup", vin);
            @SuppressLint("InflateParams") final View popupView = this.getLayoutInflater().inflate(R.layout.vehicle_popup, null);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.vehicle_popup_new_headline)
                    .setView(popupView)
                    .setPositiveButton(R.string.vehicle_popup_save, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            EditText vehicleName = (EditText) popupView.findViewById(R.id.save_vehicle_as);
                            dataSource.setNameForVin(vin, vehicleName.getText().toString());
                            setStatusInformation();
                        }
                    })
                    .setNegativeButton(R.string.vehicle_popup_abort, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dataSource.setNameForVin(vin, getString(R.string.vehicle_popup_new_no_name));
                            setStatusInformation();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            dataSource.setNameForVin(vin, getString(R.string.vehicle_popup_new_no_name));
                            setStatusInformation();
                        }
                    })
                    .show();
        } else {
            Timber.d("Known VIN connected");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Timber.d("onSaveInstanceState called");
        outState.putString("vin", vin);
        super.onSaveInstanceState(outState);
    }

}




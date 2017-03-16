package com.github.zeroxfelix.obd2fun.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

import com.github.zeroxfelix.obd2fun.R;
import com.github.zeroxfelix.obd2fun.interfaces.SetActionBarTitleInterface;
import com.github.zeroxfelix.obd2fun.interfaces.SetSelectedDrawerMenuItemInterface;
import com.github.zeroxfelix.obd2fun.sql.Obd2FunDataSource;
import com.github.zeroxfelix.obd2fun.sql.SavedVehicles;
import com.github.zeroxfelix.obd2fun.ui.VehicleListViewAdapter;
import timber.log.Timber;

public class ManageVehiclesFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.d("onCreateView called");
        View view = inflater.inflate(R.layout.manage_vehicles_fragment, container, false);
        Obd2FunDataSource dataSource = new Obd2FunDataSource(getActivity().getApplicationContext());

        Timber.d("Fill list with saved vehicles");
        ArrayList<SavedVehicles> vehicleList = dataSource.getAllSavedVehicles();
        ListView vehicleListView = (ListView) view.findViewById(R.id.vehicle_list);
        vehicleListView.setEmptyView(view.findViewById(R.id.no_vehicle_text));
        VehicleListViewAdapter vehicleListViewAdapter = new VehicleListViewAdapter(getActivity(), vehicleList);
        vehicleListView.setAdapter(vehicleListViewAdapter);

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onResume() {
        Timber.d("onResume called");
        super.onResume();
        // Set title
        ((SetActionBarTitleInterface)getActivity()).setActionBarTitle(getString(R.string.main_activity_drawer_menu_manage_vehicles));
        // Set selected menuItem in navigation drawer
        ((SetSelectedDrawerMenuItemInterface)getActivity()).setSelectedDrawerMenuItem(R.id.main_activity_drawer_menu_manage_vehicles);
    }

}
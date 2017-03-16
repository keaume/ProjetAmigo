package com.github.zeroxfelix.obd2fun.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

import com.github.zeroxfelix.obd2fun.R;
import com.github.zeroxfelix.obd2fun.interfaces.SetActionBarTitleInterface;
import com.github.zeroxfelix.obd2fun.interfaces.SetSelectedDrawerMenuItemInterface;
import com.github.zeroxfelix.obd2fun.obd.ObdCommandType;
import com.github.zeroxfelix.obd2fun.ui.DataWidget;
import com.github.zeroxfelix.obd2fun.ui.WidgetListViewAdapter;
import timber.log.Timber;

public class ReadDataFragment extends Fragment {

    private ArrayList<DataWidget> widgetList;
    private WidgetListViewAdapter widgetListViewAdapter;
    private PopupMenu chooseWidgetPopup;
    private com.github.clans.fab.FloatingActionButton floatingActionButton;

    @Override
    public void onCreate(Bundle savedInstanceState){
        Timber.d("onCreate called");
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            Timber.d("Restoring variables previous states");
            widgetList = savedInstanceState.getParcelableArrayList("widgetList");
            widgetListViewAdapter = savedInstanceState.getParcelable("widgetListViewAdapter");
        } else {
            Timber.d("Beginning with a fresh instance");
            widgetList = new ArrayList<>();
            widgetListViewAdapter = null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.d("onCreateView called");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.read_data_fragment, container, false);

        ListView widgetGrid = (ListView) view.findViewById(R.id.widgetGrid);
        widgetGrid.setEmptyView(view.findViewById(R.id.read_data_fragments_no_widgets));
        if(widgetListViewAdapter == null) {
            widgetListViewAdapter = new WidgetListViewAdapter(this.getActivity(), widgetList);
        }
        widgetGrid.setAdapter(widgetListViewAdapter);

        floatingActionButton = (com.github.clans.fab.FloatingActionButton) view.findViewById(R.id.widget_add_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                floatingActionButton.post(new Runnable() {
                    @Override
                         public void run() {
                            Timber.d("Showing Menu");
                             chooseWidgetPopup.show();
                         }
                     });
            }
        });

        chooseWidgetPopup = new PopupMenu(this.getActivity(), floatingActionButton);
        for(ObdCommandType obdCommandType : ObdCommandType.values()) {
            if (obdCommandType.getIsRecordable()) {
                chooseWidgetPopup.getMenu().add(obdCommandType.getNameForValue());
            }
        }
        chooseWidgetPopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                ObdCommandType obdCommandType = ObdCommandType.getValueForName((String) item.getTitle());
                addWidget(new DataWidget(obdCommandType, getResources().getString(R.string.widget_wait_for_value), getActivity(), widgetListViewAdapter));
                return true;
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        Timber.d("onResume called");
        super.onResume();
        // Set title
        ((SetActionBarTitleInterface)getActivity()).setActionBarTitle(getString(R.string.main_activity_drawer_menu_read_data));
        // Set selected menuItem in navigation drawer
        ((SetSelectedDrawerMenuItemInterface)getActivity()).setSelectedDrawerMenuItem(R.id.main_activity_drawer_menu_read_data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Timber.d("onSaveInstanceState called");
        outState.putParcelableArrayList("widgetList", widgetList);
        outState.putParcelable("widgetListViewAdapter",widgetListViewAdapter);
        super.onSaveInstanceState(outState);
    }

    private void refreshGrid(){
        if (widgetListViewAdapter != null) {
            widgetListViewAdapter.notifyDataSetChanged();
        }
    }

    private void addWidget(DataWidget widget){
        widgetList.add(widget);
        refreshGrid();
    }

    public void clearWidgetList(){
        Timber.d("Clear widgetList on Disconnect");
        widgetList.clear();
        refreshGrid();
    }

}
package com.github.zeroxfelix.obd2fun.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

import com.github.zeroxfelix.obd2fun.R;
import com.github.zeroxfelix.obd2fun.sql.Obd2FunDataSource;
import com.github.zeroxfelix.obd2fun.sql.SavedVehicles;

public class VehicleListViewAdapter extends BaseAdapter {
    private final Context context;
    private final int layoutResourceId;
    private final ArrayList<SavedVehicles> data;

    public VehicleListViewAdapter(Context context, ArrayList<SavedVehicles> data) {
        this.layoutResourceId = R.layout.vehicle_template;
        this.context = context;
        this.data = data;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View row = convertView;
        final ViewHolder holder;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new ViewHolder();
            holder.vehicleName = (TextView) row.findViewById(R.id.vehicle_name);
            holder.vehicleVin = (TextView) row.findViewById(R.id.vehicle_vin);
            holder.vehicleDeleteButton = (ImageButton) row.findViewById(R.id.delete_vehicle);
            holder.vehicleEditButton = (ImageButton) row.findViewById(R.id.edit_vehicle);


            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        holder.vehicleName.setText(context.getString(R.string.manage_vehicle_fragment_name,data.get(position).getName()));
        holder.vehicleVin.setText(context.getString(R.string.manage_vehicle_fragment_vin,data.get(position).getVin()));

        holder.vehicleDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Obd2FunDataSource dataSource = new Obd2FunDataSource(context);
                new AlertDialog.Builder(context)
                        .setTitle(R.string.vehicle_popup_delete_headline)
                        .setMessage(R.string.vehicle_popup_delete_text)
                        .setPositiveButton(R.string.vehicle_popup_delete_all, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dataSource.deleteAllDataForVin(data.get(position).getVin());
                                data.remove(position);

                                notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton(R.string.vehicle_popup_abort, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
            }
        });

        holder.vehicleEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Obd2FunDataSource dataSource = new Obd2FunDataSource(context);
                LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
                @SuppressLint("InflateParams") final View popupView = inflater.inflate(R.layout.vehicle_popup,null);
                new AlertDialog.Builder(context)
                        .setTitle(R.string.vehicle_popup_edit_headline)
                        .setView(popupView)
                        .setPositiveButton(R.string.vehicle_popup_save, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                EditText vehicleName = (EditText) popupView.findViewById(R.id.save_vehicle_as);
                                dataSource.setNameForVin(data.get(position).getVin(), vehicleName.getText().toString());
                                data.get(position).refreshName();
                                notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton(R.string.vehicle_popup_abort, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
            }
        });


        return row;
    }


    static class ViewHolder {
        TextView vehicleName;
        TextView vehicleVin;
        ImageButton vehicleDeleteButton;
        ImageButton vehicleEditButton;
    }
}

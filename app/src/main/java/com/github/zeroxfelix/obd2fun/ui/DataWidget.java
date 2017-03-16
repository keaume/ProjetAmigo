package com.github.zeroxfelix.obd2fun.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;

import java.util.LinkedHashMap;
import java.util.Map;

import com.github.zeroxfelix.obd2fun.R;
import com.github.zeroxfelix.obd2fun.obd.ObdBroadcastIntent;
import com.github.zeroxfelix.obd2fun.obd.ObdCommandJob;
import com.github.zeroxfelix.obd2fun.obd.ObdCommandJobResult;
import com.github.zeroxfelix.obd2fun.obd.ObdCommandType;
import timber.log.Timber;

public class DataWidget implements Parcelable {
    private ObdCommandType commandType;
    private final String title;
    private String value;
    private String unit = null;
    private boolean isGraph = false;
    private boolean isGraphable;
    private Context context;
    private WidgetListViewAdapter adapter;
    private int numberOfPlottedValues = 50;
    private final LinkedHashMap<Integer, Double> valuesOverTime = new LinkedHashMap<Integer, Double>(){
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Double> eldest) {
            return this.size() > numberOfPlottedValues;
        }
    };
    private int n = 0;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d("Result received for %s", commandType.getNameForValue());
            ObdCommandJobResult obdCommandJobResult = intent.getParcelableExtra("obdCommandJobResult");
            if(obdCommandJobResult.getState() == ObdCommandJob.State.FINISHED) {
                value = obdCommandJobResult.getFormattedResult();
                unit = obdCommandJobResult.getResultUnit();
                if(isGraphable){
                    try {
                        Double value = Double.parseDouble(obdCommandJobResult.getCalculatedResult());
                        valuesOverTime.put(n, value);
                        Timber.d("Value '%s' added to valuesOverTime of '%s'", value, commandType.getNameForValue());
                    } catch (NumberFormatException e) {
                        Timber.e(e, "Error while parsing ObdCommandJobResult");
                    }
                    n++;
                }
                adapter.notifyDataSetChanged();
            }
            else {
                Timber.d("ObdCommandJob '%s' not finished properly.", commandType.getNameForValue());
                value = context.getString(R.string.read_data_fragment_command_not_supported);
                isGraphable = false;
                adapter.notifyDataSetChanged();
            }
        }
    };

    public DataWidget(ObdCommandType commandType, String value, Context context, WidgetListViewAdapter adapter) {
        this.commandType = commandType;
        this.title = commandType.getNameForValue();
        this.value = value;
        this.context = context;
        this.adapter = adapter;

        this.isGraphable = commandType.getIsGraphable();
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, new IntentFilter(commandType.getNameForValue()));
        Timber.d("Registered receiver for %s", commandType.getNameForValue());
        LocalBroadcastManager.getInstance(context).sendBroadcast(ObdBroadcastIntent.getRegisterPeriodicObdCommandJobIntent(commandType));
    }

    private DataWidget(Parcel in) {
        title = in.readString();
        value = in.readString();
        unit = in.readString();
        isGraph = in.readByte() != 0;
        isGraphable = in.readByte() != 0;
        numberOfPlottedValues = in.readInt();
        n = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(value);
        dest.writeString(unit);
        dest.writeByte((byte) (isGraph ? 1 : 0));
        dest.writeByte((byte) (isGraphable ? 1 : 0));
        dest.writeInt(numberOfPlottedValues);
        dest.writeInt(n);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DataWidget> CREATOR = new Creator<DataWidget>() {
        @Override
        public DataWidget createFromParcel(Parcel in) {
            return new DataWidget(in);
        }

        @Override
        public DataWidget[] newArray(int size) {
            return new DataWidget[size];
        }
    };

    public void unregisterReceiver(){
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
        LocalBroadcastManager.getInstance(context).sendBroadcast(ObdBroadcastIntent.getUnregisterPeriodicObdCommandJobIntent(commandType));
        Timber.d("Unregistered receiver for %s", commandType.getNameForValue());
    }

    public String getTitle(){
        return this.title;
    }

    public String getValue(){
        return this.value;
    }

    public String getUnit(){
        return this.unit;
    }

    public boolean isPercentage(){
        return commandType.getIsPercentage();
    }

    public boolean getIsGraphable(){
        return this.isGraphable;
    }

    public void setIsGraph(boolean b){
        this.isGraph = b;
    }

    public boolean getIsGraph(){
        return this.isGraph;
    }

    public Number[] getValuesOverTime(){
        Number[] valuesOverTimeAsNumber = new Number[valuesOverTime.size()];
        for (int i = 0; i< valuesOverTime.values().toArray().length; i++) {
            Double d = (Double) valuesOverTime.values().toArray()[i];
            valuesOverTimeAsNumber[i] = d;
        }

        return valuesOverTimeAsNumber;
    }
}
